export type SearchOption = { value: string; label: string }

export type Address = {
  road: string
  jibun: string
  latitude: number
  longitude: number
}
export type StoreLocation = { name: string; address: Address }

export type InterestItemGroup = 'raw' | 'sub'
export type InterestItem = { id: number; name: string }
export type InterestItems = Record<InterestItemGroup, InterestItem[]>
