export type SearchOption = { value: string; label: string }

export type Address = { road: string; jibun: string }

export type InterestItemGroup = 'raw' | 'sub'

export type InterestItems = Record<InterestItemGroup, string[]>
