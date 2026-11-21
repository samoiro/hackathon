"""POST /v1/analyze 요청·응답. 기준: docs/API_CONTRACT_DRAFT.md"""

from typing import Literal

from pydantic import BaseModel, ConfigDict, Field

Category = Literal["EVENT", "INGREDIENT", "SUPPLY"]
TaskType = Literal[
    "STAFFING", "STOCK", "HOURS", "PICKUP", "RESERVATION", "FACILITY", "PACKING",
    "PRICE_CHECK", "BUY_AHEAD", "ORDER_PLAN",
    "RESTOCK", "DISPLAY", "ORDER_LESS",
]


class _In(BaseModel):
    # Java가 필드를 더해도 깨지지 않게 모르는 필드는 버린다.
    model_config = ConfigDict(extra="ignore")


class Store(_In):
    gu: str
    area_label: str
    industry_category: str
    industry_name: str
    operation_type: Literal["STORE", "DELIVERY", "BOTH"] | None = None


class EventSearch(_In):
    radius_m: int
    days: int


class Event(_In):
    evidence_id: str
    title: str
    category: str | None = None
    place: str | None = None
    date_label: str
    status: Literal["UPCOMING", "ONGOING"]
    distance_m: int
    walk_minutes: int
    is_free: bool | None = None
    trigger: Literal["FESTIVAL", "PERFORMANCE"]
    time_band: Literal["MORNING", "LUNCH", "AFTERNOON", "EVENING", "UNKNOWN"] = "UNKNOWN"
    peak_label: str | None = None


class SeasonEvent(_In):
    evidence_id: str
    code: str
    label: str
    type: Literal["HOLIDAY", "SCHOOL", "SEASON"]
    date_label: str
    days_left: int


class WeatherWarning(_In):
    kind: str
    level: str


class Weather(_In):
    evidence_id: str
    impacts: list[str] = []
    warnings: list[WeatherWarning] = []
    first_date_label: str | None = None


class Signal(_In):
    text: str
    demand: Literal["UP", "STRONG_UP", "DOWN", "STRONG_DOWN"]


class RawMaterial(_In):
    evidence_id: str
    item_id: int
    name: str
    unit: str | None = None
    grade: str | None = None
    base_date: str | None = None
    price: int | None = None
    change_1w_pct: float | None = None
    change_4w_pct: float | None = None
    trend: Literal["UP", "DOWN", "FLAT"] | None = None
    consecutive_weeks: int | None = None
    price_stage: Literal[
        "SEVERE_HIGH", "ALERT_HIGH", "WATCH_HIGH", "STABLE", "WATCH_LOW", "ALERT_LOW", "SEVERE_LOW"
    ] | None = None
    signals: list[Signal] = []


class Supply(_In):
    evidence_id: str
    item_id: int
    name: str
    kind: Literal["SUPPLY", "STOCK"]
    demand: Literal["UP", "STRONG_UP", "DOWN"]
    trigger_label: str | None = None
    trigger_evidence_id: str | None = None
    prepare_by_label: str | None = None


class Card(_In):
    card_id: str
    category: Category
    task_type: TaskType
    item_id: int | None = None
    evidence_ids: list[str] = Field(min_length=1)


class AnalyzeRequest(_In):
    request_id: str
    base_time: str
    store: Store
    event_search: EventSearch | None = None
    events: list[Event] = []
    season_events: list[SeasonEvent] = []
    weather: Weather | None = None
    raw_materials: list[RawMaterial] = []
    supplies: list[Supply] = []
    # 묶음당 최대 EVENT 3 · INGREDIENT 3 · SUPPLY 2
    cards: list[Card] = Field(max_length=8)


class Suggestion(BaseModel):
    card_id: str
    title: str
    expected_effect: str | None
    sub_tasks: list[str]


class AnalyzeResponse(BaseModel):
    suggestions: list[Suggestion]
    model: str
    generated_at: str


class LlmOutput(BaseModel):
    """LLM이 돌려주는 JSON. 길이·개수는 validation에서 따로 검사한다."""

    suggestions: list[Suggestion]


class ErrorBody(BaseModel):
    code: str
    detail: str
