import type { Address, SearchOption } from '@/types/search'

// shortcut: API가 없어 가짜 목록을 0.4초 늦게 돌려준다. 백엔드 연동 때 생성된 SDK 함수로 바꾼다
const CATEGORIES = [
  '일반 음식점 · 한식',
  '일반 음식점 · 중식',
  '일반 음식점 · 일식',
  '일반 음식점 · 양식',
  '분식',
  '치킨 · 호프',
  '주점',
  '카페 · 디저트',
  '베이커리',
]

const RAW_ITEMS = [
  '쌀',
  '돼지고기',
  '배추',
  '소고기',
  '닭고기',
  '양파',
  '대파',
  '마늘',
  '감자',
  '달걀',
  '무',
]

const SUB_ITEMS = ['포장 용기', '일회용 수저', '비닐봉투', '종이컵', '냅킨']

// 서울 실시간 도시데이터 집계 영역 일부 (Figma 메모: GET /api/areas)
const AREAS = [
  '서울숲공원',
  '성수카페거리',
  '뚝섬한강공원',
  '건대입구역',
  '왕십리역',
  '홍대 관광특구',
  '강남역',
  '여의도한강공원',
  '광화문·덕수궁',
  '잠실 관광특구',
]

const ADDRESSES: Address[] = [
  { road: '서울 성동구 서울숲2길 32', jibun: '지번 · 성수동1가 ○○○-○' },
  { road: '서울 성동구 서울숲2길 32-14', jibun: '지번 · 성수동1가 ○○○-○' },
  { road: '서울 성동구 서울숲길 17', jibun: '지번 · 성수동1가 ○○○-○' },
  { road: '서울 성동구 성수이로 88', jibun: '지번 · 성수동2가 ○○○-○' },
  { road: '서울 성동구 왕십리로 115', jibun: '지번 · 성수동1가 ○○○-○' },
  { road: '서울 마포구 와우산로 94', jibun: '지번 · 서교동 ○○○-○' },
  { road: '경기 성남시 분당구 판교역로 166', jibun: '지번 · 백현동 ○○○-○' },
]

const wait = (ms: number): Promise<void> =>
  new Promise((resolve) => setTimeout(resolve, ms))

async function searchList(
  list: string[],
  keyword: string,
): Promise<SearchOption[]> {
  await wait(400)
  return list
    .filter((item) => item.includes(keyword.trim()))
    .map((item) => ({ value: item, label: item }))
}

export const searchCategories = (keyword: string) =>
  searchList(CATEGORIES, keyword)
export const searchRawItems = (keyword: string) =>
  searchList(RAW_ITEMS, keyword)
export const searchSubItems = (keyword: string) =>
  searchList(SUB_ITEMS, keyword)
export const searchAreas = (keyword: string) => searchList(AREAS, keyword)

export async function searchAddresses(keyword: string): Promise<Address[]> {
  await wait(400)
  const trimmed = keyword.trim()
  if (!trimmed) return []
  return ADDRESSES.filter(
    (address) =>
      address.road.includes(trimmed) || address.jibun.includes(trimmed),
  )
}
