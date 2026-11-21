import logging
import secrets
from functools import lru_cache

from fastapi import Depends, FastAPI, Header, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.analyze import InvalidLlmOutput, generate
from app.config import Settings, load_settings
from app.llm import LlmClient, LlmTimeout, LlmUnavailable, OpenAiClient
from app.schemas import AnalyzeRequest, AnalyzeResponse, ErrorBody

logging.basicConfig(level=logging.INFO)

app = FastAPI(title="Hackathon AI Server", version="0.1.0")


class Unauthorized(Exception):
    pass


@lru_cache
def get_settings() -> Settings:
    return load_settings()


@lru_cache
def _openai_client(settings: Settings) -> OpenAiClient:
    return OpenAiClient(settings)


def get_llm(settings: Settings = Depends(get_settings)) -> LlmClient:
    if not settings.llm_api_key or not settings.llm_model:
        raise LlmUnavailable("LLM_API_KEY 또는 LLM_MODEL이 설정되지 않았어요")
    return _openai_client(settings)


def verify_token(
    settings: Settings = Depends(get_settings),
    token: str | None = Header(default=None, alias="X-Internal-Token"),
) -> None:
    # 공개 URL이라 Java만 부를 수 있게 막는다. 서버 토큰이 비어 있으면 아무도 통과하지 못한다.
    if not settings.ai_server_token or not token or not secrets.compare_digest(token, settings.ai_server_token):
        raise Unauthorized()


def _error(status: int, code: str, detail: str) -> JSONResponse:
    return JSONResponse(status_code=status, content=ErrorBody(code=code, detail=detail).model_dump())


@app.exception_handler(Unauthorized)
def _unauthorized(_: Request, __: Unauthorized) -> JSONResponse:
    return _error(401, "UNAUTHORIZED", "X-Internal-Token이 없거나 맞지 않아요")


@app.exception_handler(RequestValidationError)
def _invalid_request(_: Request, exc: RequestValidationError) -> JSONResponse:
    # 입력값은 빼고 위치만 알려 준다(요청 원문을 응답·로그에 남기지 않음).
    fields = sorted({".".join(str(p) for p in e["loc"][1:]) or "body" for e in exc.errors()})
    return _error(422, "INVALID_REQUEST", "요청 형식이 맞지 않아요: " + ", ".join(fields[:10]))


@app.exception_handler(InvalidLlmOutput)
def _invalid_output(_: Request, __: InvalidLlmOutput) -> JSONResponse:
    return _error(502, "INVALID_LLM_OUTPUT", "다시 요청해도 규칙에 맞는 추천 문장이 없어요")


@app.exception_handler(LlmUnavailable)
def _unavailable(_: Request, exc: LlmUnavailable) -> JSONResponse:
    return _error(503, "LLM_UNAVAILABLE", f"LLM을 부를 수 없어요 ({exc})")


@app.exception_handler(LlmTimeout)
def _timeout(_: Request, __: LlmTimeout) -> JSONResponse:
    return _error(504, "LLM_TIMEOUT", "LLM이 시간 안에 답하지 않았어요")


@app.get("/health", tags=["system"])
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post(
    "/v1/analyze",
    tags=["analyze"],
    response_model=AnalyzeResponse,
    dependencies=[Depends(verify_token)],
    responses={code: {"model": ErrorBody} for code in (401, 422, 502, 503, 504)},
)
def analyze(
    request: AnalyzeRequest,
    settings: Settings = Depends(get_settings),
    llm: LlmClient = Depends(get_llm),
) -> AnalyzeResponse:
    """Java가 정한 카드 칸마다 제목·기대 효과·세부 할 일을 쓴다. 계약: docs/API_CONTRACT_DRAFT.md"""
    return generate(request, llm, settings)
