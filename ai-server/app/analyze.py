"""카드 칸마다 문장을 받아 검증하고, 어기면 1번만 다시 요청한다."""

import logging
import time
from collections.abc import Callable
from datetime import datetime, timedelta, timezone

from pydantic import ValidationError

from app.config import Settings
from app.llm import LlmClient, LlmTimeout, LlmUnavailable
from app.prompt import build_messages, input_json, output_schema, retry_message
from app.schemas import AnalyzeRequest, AnalyzeResponse, LlmOutput, Suggestion
from app.validation import check, numbers

log = logging.getLogger(__name__)
KST = timezone(timedelta(hours=9))
MAX_ATTEMPTS = 2  # 첫 요청 + 재요청 1번


class InvalidLlmOutput(Exception):
    """재요청 후에도 남길 칸이 0개 → 502"""


def generate(
    request: AnalyzeRequest,
    llm: LlmClient,
    settings: Settings,
    clock: Callable[[], float] = time.monotonic,
) -> AnalyzeResponse:
    started = clock()
    card_ids = [c.card_id for c in request.cards]
    best: dict[str, Suggestion] = {}
    clean: set[str] = set()
    attempts = 0
    if card_ids:
        messages = build_messages(request)
        schema = output_schema(card_ids)
        allowed = numbers(input_json(request))
        deadline = started + settings.llm_timeout_seconds
        while attempts < MAX_ATTEMPTS:
            remaining = deadline - clock()
            if remaining <= 0:
                break
            attempts += 1
            called = clock()
            try:
                raw = llm.complete(messages, schema, timeout=remaining)
            except (LlmTimeout, LlmUnavailable):
                if attempts == 1:
                    raise
                break  # 재요청이 실패하면 첫 답에서 남길 수 있는 칸으로 응답한다
            answered = clock()
            try:
                checked = check(LlmOutput.model_validate_json(raw), card_ids, allowed)
                errors = checked.errors
            except ValidationError:
                checked, errors = None, ["JSON이 약속한 형식이 아니에요"]
            # 위반 문구에는 card_id·규칙·숫자만 있고 사용자 입력 원문은 없다. 프롬프트를 다듬을 때 본다.
            log.info(
                "analyze request_id=%s attempt=%d llm=%.1fs check=%.3fs errors=%s",
                request.request_id, attempts, answered - called, clock() - answered, errors or "없음",
            )
            if checked:
                for card_id, suggestion in checked.kept.items():
                    # 고칠 것 없이 통과한 문장을 기대 효과를 지운 문장보다 앞세운다.
                    if card_id in checked.clean and card_id not in clean:
                        best[card_id] = suggestion
                        clean.add(card_id)
                    elif card_id not in best:
                        best[card_id] = suggestion
            if not errors:
                break
            messages = [*messages, {"role": "assistant", "content": raw}, retry_message(errors)]
        if not best:
            raise InvalidLlmOutput()
    log.info(
        "analyze request_id=%s cards=%d kept=%d clean=%d attempts=%d elapsed=%.1fs",
        request.request_id, len(card_ids), len(best), len(clean), attempts, clock() - started,
    )
    return AnalyzeResponse(
        suggestions=[best[c] for c in card_ids if c in best],
        model=settings.llm_model,
        generated_at=datetime.now(KST).isoformat(timespec="seconds"),
    )
