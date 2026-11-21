// GET /api/supplies?query= 한 건 (부자재)
export type SupplyResponse = {
  id: number
  name: string
}

export type Demand = 'UP' | 'STRONG_UP' | 'DOWN'

export type TriggerType = 'WEATHER' | 'EVENT' | 'HOLIDAY' | 'SCHOOL' | 'SEASON'

// 부자재 준비 시점. 이번 주 걸린 규칙이 없으면 itemId·name만 채운다
export type SupplyTimingResponse = {
  evidenceId: string | null
  itemId: number
  name: string
  demand: Demand | null
  // LocalDate
  prepareBy: string | null
  leadDays: number | null
  triggerLabel: string | null
  triggerEvidenceId: string | null
}
