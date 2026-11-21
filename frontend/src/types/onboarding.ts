import type { InterestItems, StoreLocation } from '@/types/search'

export type SalesType = 'STORE' | 'DELIVERY' | 'BOTH'

// 가게 정보 입력 화면에서 입력 중인 값. 이 기기 로컬스토리지에 저장해 다시 불러온다
export type OnboardingDraft = {
  location?: StoreLocation
  industryId?: number
  salesType?: SalesType
  items: InterestItems
}
