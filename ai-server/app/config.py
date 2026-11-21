"""환경변수 설정. 값은 Render 비밀변수나 `uvicorn --env-file .env`로 넣는다."""

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    llm_api_key: str
    llm_model: str
    ai_server_token: str
    llm_timeout_seconds: float
    llm_temperature: float | None


def load_settings() -> Settings:
    temperature = os.getenv("LLM_TEMPERATURE", "").strip()
    return Settings(
        llm_api_key=os.getenv("LLM_API_KEY", "").strip(),
        llm_model=os.getenv("LLM_MODEL", "").strip(),
        ai_server_token=os.getenv("AI_SERVER_TOKEN", "").strip(),
        # 계약: Python의 LLM 호출 40초, Java의 읽기 대기 60초. 재시도까지 이 안에서 끝낸다.
        # .env에 빈 값(LLM_TIMEOUT_SECONDS=)으로 있어도 기본값을 쓴다.
        llm_timeout_seconds=float(os.getenv("LLM_TIMEOUT_SECONDS", "").strip() or "40"),
        # 추론형 모델은 temperature를 받지 않아서 값이 있을 때만 보낸다.
        llm_temperature=float(temperature) if temperature else None,
    )
