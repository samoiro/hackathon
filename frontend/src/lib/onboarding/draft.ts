import type { OnboardingDraft, SalesType } from '@/types/onboarding'
import type { InterestItem, StoreLocation } from '@/types/search'

const STORAGE_KEY = 'samoiro:onboarding-draft'
const SALES_TYPES: SalesType[] = ['STORE', 'DELIVERY', 'BOTH']

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null

const isLocation = (value: unknown): value is StoreLocation =>
  isRecord(value) &&
  typeof value.name === 'string' &&
  isRecord(value.address) &&
  typeof value.address.road === 'string' &&
  typeof value.address.jibun === 'string' &&
  typeof value.address.latitude === 'number' &&
  typeof value.address.longitude === 'number'

const items = (value: unknown, max: number): InterestItem[] =>
  Array.isArray(value)
    ? value
        .filter(
          (item): item is InterestItem =>
            isRecord(item) &&
            typeof item.id === 'number' &&
            typeof item.name === 'string',
        )
        .slice(0, max)
    : []

// 저장 값은 사용자가 고칠 수 있으니 모양이 맞는 값만 받는다. 저장소를 못 쓰는 환경이면 빈 입력으로 시작한다
export function loadOnboardingDraft(): OnboardingDraft | undefined {
  try {
    const saved: unknown = JSON.parse(
      localStorage.getItem(STORAGE_KEY) ?? 'null',
    )
    if (!isRecord(saved)) return undefined
    return {
      location: isLocation(saved.location) ? saved.location : undefined,
      industryId:
        typeof saved.industryId === 'number' ? saved.industryId : undefined,
      salesType: SALES_TYPES.find((type) => type === saved.salesType),
      items: isRecord(saved.items)
        ? { raw: items(saved.items.raw, 3), sub: items(saved.items.sub, 2) }
        : { raw: [], sub: [] },
    }
  } catch {
    return undefined
  }
}

export function saveOnboardingDraft(draft: OnboardingDraft): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(draft))
  } catch {
    // 저장하지 못해도 이번 방문 동안은 화면의 입력이 그대로 남는다
  }
}
