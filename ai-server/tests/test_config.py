from app.config import load_settings


def test_blank_optional_values_fall_back_to_defaults(monkeypatch):
    # .env.example을 복사해 선택 값을 비워 둔 경우
    monkeypatch.setenv("LLM_TIMEOUT_SECONDS", "")
    monkeypatch.setenv("LLM_TEMPERATURE", "")
    settings = load_settings()
    assert settings.llm_timeout_seconds == 40
    assert settings.llm_temperature is None
