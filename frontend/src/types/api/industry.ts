import type { IngredientResponse } from '@/types/api/ingredient'
import type { SupplyResponse } from '@/types/api/supply'

// GET /api/industries 한 건
export type IndustryResponse = {
  id: number
  // 업종 분류 (예: 음식점)
  category: string
  // 업종 이름 (예: 한식·국밥)
  name: string
}

// GET /api/industries/{industryId}/recommended-items. "많이 고르는 품목 · 한 번에 담기"
export type RecommendedItemsResponse = {
  industryId: number
  // 최대 3개
  ingredients: IngredientResponse[]
  // 최대 2개
  supplies: SupplyResponse[]
}
