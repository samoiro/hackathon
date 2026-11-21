"""OpenAI 호출. 재시도는 analyze.py가 검증 결과를 보고 1번만 하므로 SDK 자체 재시도는 끈다."""

from typing import Protocol

import openai

from app.config import Settings


class LlmTimeout(Exception):
    """LLM이 시간 안에 답하지 않음 → 504"""


class LlmUnavailable(Exception):
    """키 미설정, 공급자 오류 → 503"""


class LlmClient(Protocol):
    def complete(self, messages: list[dict[str, str]], schema: dict, timeout: float) -> str: ...


class OpenAiClient:
    def __init__(self, settings: Settings):
        self._settings = settings
        self._client = openai.OpenAI(api_key=settings.llm_api_key, max_retries=0)

    def complete(self, messages: list[dict[str, str]], schema: dict, timeout: float) -> str:
        params = {
            "model": self._settings.llm_model,
            "messages": messages,
            "response_format": {
                "type": "json_schema",
                "json_schema": {"name": "suggestions", "strict": True, "schema": schema},
            },
            "timeout": timeout,
        }
        if self._settings.llm_temperature is not None:
            params["temperature"] = self._settings.llm_temperature
        try:
            response = self._client.chat.completions.create(**params)
        except openai.APITimeoutError as e:
            raise LlmTimeout() from e
        except openai.OpenAIError as e:
            # 오류 본문에 요청 원문이 섞일 수 있어 종류 이름만 남긴다.
            raise LlmUnavailable(type(e).__name__) from e
        # 거절(refusal)이면 content가 비어 있다. 빈 문자열은 형식 오류로 보고 다시 요청한다.
        return response.choices[0].message.content or ""
