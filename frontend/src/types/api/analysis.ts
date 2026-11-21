import type { IndustryResponse } from '@/types/api/industry'
import type {
  IngredientPriceResponse,
  IngredientResponse,
} from '@/types/api/ingredient'
import type {
  SupplyResponse,
  SupplyTimingResponse,
  TriggerType,
} from '@/types/api/supply'

// 날짜 문자열: LocalDate는 YYYY-MM-DD, OffsetDateTime은 ISO 8601

export type AnalysisStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'

// 분석 중 화면 4단계: 가게 정보 확인 · 주변 행사 조회 · 품목 가격 조회 · 준비할 일 정리
export type StepCode =
  'STORE_CHECK' | 'EVENT_SEARCH' | 'PRICE_LOOKUP' | 'TASK_GENERATION'

export type StepStatus = 'WAITING' | 'RUNNING' | 'DONE' | 'FAILED'

export type EventSource = 'CULTURAL_EVENT' | 'SEASON' | 'WEATHER' | 'NONE'

export type EventStatus = 'UPCOMING' | 'ONGOING'

export type EventTrigger = 'FESTIVAL' | 'PERFORMANCE'

export type OperationType = 'STORE' | 'DELIVERY' | 'BOTH'

export type SuggestionCategory = 'EVENT' | 'INGREDIENT' | 'SUPPLY'

export type TaskType =
  | 'STAFFING'
  | 'STOCK'
  | 'HOURS'
  | 'PICKUP'
  | 'RESERVATION'
  | 'FACILITY'
  | 'PACKING'
  | 'PRICE_CHECK'
  | 'BUY_AHEAD'
  | 'ORDER_PLAN'
  | 'RESTOCK'
  | 'DISPLAY'
  | 'ORDER_LESS'

export type FeedbackVote = 'HELPFUL' | 'NOT_RELEVANT'

export type WeatherImpact =
  | 'RAIN_SNOW'
  | 'HEAVY_SNOW'
  | 'COLD_WAVE'
  | 'HEATWAVE'
  | 'STRONG_WIND'
  | 'FINE_DUST'
  | 'WARNING'

export type DayPart = 'AM' | 'PM' | 'ALL_DAY'

// POST /api/analysis 요청 (가게 정보 선택 화면의 입력 전부)
export type AnalysisRequest = {
  // 최대 30자
  storeName: string
  roadAddress: string
  latitude: number
  longitude: number
  industryId: number
  operationType: OperationType | null
  // 최대 3개, 중복 불가
  ingredientIds: number[] | null
  // 최대 2개, 중복 불가
  supplyIds: number[] | null
  // '해당 없어요'로 표시한 카드 키 {category}:{taskType}:{itemId 또는 -}, 최대 50개
  excludedKeys: string[] | null
}

// POST /api/analysis 응답 (202). 이 id로 GET /api/analysis/{id}를 폴링한다
export type AnalysisCreatedResponse = {
  analysisId: string
  status: AnalysisStatus
}

export type AnalysisStep = {
  code: StepCode
  status: StepStatus
}

export type AnalysisFailure = {
  step: StepCode
  // 예: PRICE_SOURCE_FAILED
  code: string
  message: string
}

export type EventSearch = {
  days: number
  baseDate: string
  count: number
  source: EventSource
}

export type StoreSummary = {
  name: string
  roadAddress: string
  gu: string
  // 예: 성수동 · 서울숲 인근
  areaLabel: string
  industry: IndustryResponse
  operationType: OperationType | null
  ingredients: IngredientResponse[]
  supplies: SupplyResponse[]
}

export type EventResponse = {
  evidenceId: string
  title: string
  category: string
  trigger: EventTrigger
  place: string
  startDate: string
  endDate: string
  timeText: string | null
  status: EventStatus
  distanceM: number
  walkMinutes: number
  isFree: boolean | null
  latitude: number
  longitude: number
  sourceUrl: string | null
}

export type SeasonEventResponse = {
  evidenceId: string
  code: string
  label: string
  type: TriggerType
  startDate: string
  endDate: string
}

export type WeatherWarning = {
  kind: string
  level: string
  startTime: string
  endTime: string | null
}

export type WeatherForecast = {
  date: string
  dayPart: DayPart
  sky: string
  minTemperature: number | null
  maxTemperature: number | null
  precipitationChance: number | null
}

export type WeatherResponse = {
  evidenceId: string
  regionCode: string
  impacts: WeatherImpact[]
  warnings: WeatherWarning[]
  forecasts: WeatherForecast[]
  checkedAt: string
}

// 추천 카드. 강추(recommended)는 근거 점수 상위 3개
export type SuggestionResponse = {
  id: number
  cardKey: string
  category: SuggestionCategory
  taskType: TaskType
  itemId: number | null
  title: string
  reason: string
  // null이면 '이번 주 발주 전'
  dueDate: string | null
  // null이면 화면에서 그 줄을 숨긴다
  expectedEffect: string | null
  subTasks: string[]
  recommended: boolean
  // 첫 번째가 대표 근거
  evidenceIds: string[]
}

export type SourceResponse = {
  name: string
  provider: string
  updateCycle: string
  baseDate: string | null
  checkedAt: string
  url: string | null
}

// 통합 대시보드 브리핑 영역 전부 (지역 현황은 따로 받는다)
export type BriefingResponse = {
  events: EventResponse[]
  seasonEvents: SeasonEventResponse[]
  weather: WeatherResponse | null
  ingredients: IngredientPriceResponse[]
  supplies: SupplyTimingResponse[]
  suggestions: SuggestionResponse[]
  sources: SourceResponse[]
}

// GET /api/analysis/{id}. 분석 중에는 steps만, 끝나면 briefing 또는 failure
export type AnalysisResponse = {
  analysisId: string
  status: AnalysisStatus
  baseTime: string
  store: StoreSummary
  // StepCode 순서로 항상 4개
  steps: AnalysisStep[]
  eventSearch: EventSearch | null
  failure: AnalysisFailure | null
  briefing: BriefingResponse | null
}

// POST /api/analysis/{id}/feedback 요청 → 204. 카드 1장에 1표, 다시 보내면 표를 바꾼다
export type SuggestionFeedbackRequest = {
  cardKey: string
  vote: FeedbackVote
}
