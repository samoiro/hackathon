// POST /api/locations/validate 요청. 주소 검색은 프론트(카카오 지도)가 하고 고른 주소만 보낸다
export type LocationValidateRequest = {
  roadAddress: string
  latitude: number
  longitude: number
}

// 항상 200. 서울 밖 등 쓸 수 없는 주소는 valid=false와 이유
export type LocationValidateResponse = {
  valid: boolean
  message: string | null
}
