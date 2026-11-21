// GET /api/ingredients?query= 한 건 (원자재)
export type IngredientResponse = {
  id: number
  name: string
}

// 가격 단계 7단계 (수급조절매뉴얼). 4주 변동률을 부류별 분위수로 나눈다
export type PriceStage =
  | 'SEVERE_HIGH'
  | 'ALERT_HIGH'
  | 'WATCH_HIGH'
  | 'STABLE'
  | 'WATCH_LOW'
  | 'ALERT_LOW'
  | 'SEVERE_LOW'

// 가격 흐름 (change1w 기준, ±1% 미만이면 FLAT)
export type Trend = 'UP' | 'DOWN' | 'FLAT'

// "172원 (4.1%)". 하락이면 둘 다 음수
export type PriceChange = {
  amount: number
  percent: number
}

// 관심 품목 가격. 가격 자료가 없으면 price부터 priceStage까지 모두 null
export type IngredientPriceResponse = {
  evidenceId: string
  itemId: number
  name: string
  grade: string | null
  unit: string | null
  // LocalDate (YYYY-MM-DD)
  baseDate: string | null
  price: number | null
  // 4주 전 → 기준일 5개. 빈 주는 null
  weeklyPrices: (number | null)[] | null
  change1w: PriceChange | null
  change4w: PriceChange | null
  trend: Trend | null
  consecutiveWeeks: number | null
  priceStage: PriceStage | null
}
