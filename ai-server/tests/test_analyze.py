"""POST /v1/analyze. LLM은 가짜로 바꾸고, 계약의 요청 예시와 프롬프트 초안의 응답 예시를 그대로 쓴다."""

import copy
import json
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.config import Settings
from app.llm import LlmTimeout, LlmUnavailable
from app.main import app, get_llm, get_settings

FIXTURES = Path(__file__).parent / "fixtures"
REQUEST = json.loads((FIXTURES / "analyze_request.json").read_text())
LLM_OUTPUT = json.loads((FIXTURES / "llm_output.json").read_text())
TOKEN = "test-token"
SETTINGS = Settings(llm_api_key="sk-test", llm_model="test-model", ai_server_token=TOKEN,
                    llm_timeout_seconds=40, llm_temperature=None)


class FakeLlm:
    """정해 둔 답을 차례로 돌려주고, 받은 메시지를 기록한다. 답 대신 예외를 넣으면 그 예외를 던진다."""

    def __init__(self, *answers):
        self.answers = list(answers)
        self.calls: list[list[dict]] = []
        self.schemas: list[dict] = []

    def complete(self, messages, schema, timeout):
        self.calls.append(messages)
        self.schemas.append(schema)
        answer = self.answers.pop(0)
        if isinstance(answer, Exception):
            raise answer
        return answer if isinstance(answer, str) else json.dumps(answer, ensure_ascii=False)


def with_card(output, card_id, **changes):
    out = copy.deepcopy(output)
    for s in out["suggestions"]:
        if s["card_id"] == card_id:
            s.update(changes)
    return out


@pytest.fixture
def call():
    def _call(llm, body=REQUEST, token=TOKEN):
        app.dependency_overrides[get_settings] = lambda: SETTINGS
        app.dependency_overrides[get_llm] = lambda: llm
        headers = {"X-Internal-Token": token} if token else {}
        return TestClient(app).post("/v1/analyze", json=body, headers=headers)

    yield _call
    app.dependency_overrides.clear()


def test_writes_every_card_in_request_order(call):
    llm = FakeLlm(LLM_OUTPUT)
    response = call(llm)
    assert response.status_code == 200
    body = response.json()
    assert [s["card_id"] for s in body["suggestions"]] == [c["card_id"] for c in REQUEST["cards"]]
    assert body["model"] == "test-model"
    assert len(llm.calls) == 1
    # request_id는 LLM에 보내지 않는다
    assert REQUEST["request_id"] not in llm.calls[0][1]["content"]
    # card_id는 요청의 칸으로만 고를 수 있다
    card_id = llm.schemas[0]["properties"]["suggestions"]["items"]["properties"]["card_id"]
    assert card_id["enum"] == [c["card_id"] for c in REQUEST["cards"]]


def test_rejects_missing_or_wrong_token_before_calling_llm(call):
    llm = FakeLlm()
    assert call(llm, token=None).json()["code"] == "UNAUTHORIZED"
    assert call(llm, token="wrong").status_code == 401
    assert llm.calls == []


def test_invalid_request_is_422_without_echoing_input(call):
    body = copy.deepcopy(REQUEST)
    body["cards"][0]["category"] = "RAW_MATERIAL"  # 예전 묶음 이름
    response = call(FakeLlm(), body=body)
    assert response.status_code == 422
    assert response.json()["code"] == "INVALID_REQUEST"
    assert "RAW_MATERIAL" not in response.json()["detail"]


def test_retries_once_with_errors_then_uses_valid_answer(call):
    bad = with_card(LLM_OUTPUT, "C1", sub_tasks=["토요일 13~14시 근무자 정하기", "주문 담당 나누기"])  # 13·14는 입력에 없음
    llm = FakeLlm(bad, LLM_OUTPUT)
    response = call(llm)
    assert response.status_code == 200
    assert len(response.json()["suggestions"]) == 7
    assert len(llm.calls) == 2
    retry_note = llm.calls[1][-1]["content"]
    assert "C1" in retry_note and "13" in retry_note


def test_card_still_invalid_after_retry_is_dropped(call):
    bad = with_card(LLM_OUTPUT, "C4", title="배추 거래처 단가 확인과 대체 재료 검토 및 메뉴 원가 다시 계산")
    response = call(FakeLlm(bad, bad))
    ids = [s["card_id"] for s in response.json()["suggestions"]]
    assert response.status_code == 200
    assert "C4" not in ids and len(ids) == 6


def test_effect_with_number_or_sales_claim_becomes_null(call):
    bad = with_card(LLM_OUTPUT, "C2", expected_effect="공연 날 매출이 20% 늘 수 있어요.")
    response = call(FakeLlm(bad, bad))
    c2 = next(s for s in response.json()["suggestions"] if s["card_id"] == "C2")
    assert c2["expected_effect"] is None
    assert c2["title"] == "공연 날 식재료 재고 확인"


def test_keeps_first_answer_when_retry_times_out(call):
    bad = with_card(LLM_OUTPUT, "C2", expected_effect="재고를 확인해요")  # 끝맺음 위반
    response = call(FakeLlm(bad, LlmTimeout()))
    assert response.status_code == 200
    assert len(response.json()["suggestions"]) == 7


def test_nothing_usable_after_retry_is_502(call):
    response = call(FakeLlm("not json", {"suggestions": []}))
    assert response.status_code == 502
    assert response.json()["code"] == "INVALID_LLM_OUTPUT"


@pytest.mark.parametrize("error, status, code", [
    (LlmTimeout(), 504, "LLM_TIMEOUT"),
    (LlmUnavailable("APIConnectionError"), 503, "LLM_UNAVAILABLE"),
])
def test_llm_failure_on_first_call(call, error, status, code):
    response = call(FakeLlm(error))
    assert response.status_code == status
    assert response.json()["code"] == code


def test_missing_api_key_is_503():
    app.dependency_overrides[get_settings] = lambda: Settings("", "", TOKEN, 40, None)
    try:
        response = TestClient(app).post("/v1/analyze", json=REQUEST, headers={"X-Internal-Token": TOKEN})
    finally:
        app.dependency_overrides.clear()
    assert response.status_code == 503


def test_no_cards_returns_empty_without_calling_llm(call):
    llm = FakeLlm()
    response = call(llm, body={**REQUEST, "cards": []})
    assert response.status_code == 200
    assert response.json()["suggestions"] == []
    assert llm.calls == []
