// GET /api/areas 한 건 (서울시 주요 장소)
export type AreaResponse = {
  // 예: POI009
  code: string
  // /api/seoul/city-data/{area}에 그대로 넘긴다
  name: string
  category: string
  latitude: number
  longitude: number
}

// 서울시 혼잡도: 여유 · 보통 · 약간 붐빔 · 붐빔
export type Congestion = 'RELAXED' | 'NORMAL' | 'SLIGHTLY_CROWDED' | 'CROWDED'

export type CityPopulationForecast = {
  // OffsetDateTime (ISO)
  time: string
  congestion: Congestion
  populationMin: number
  populationMax: number
}

export type CityAgeRatio = {
  // 0 = 10세 미만, 10 = 10대 … 70 = 70대 이상
  ageGroup: number
  percent: number
}

export type CityWeather = {
  sky: string | null
  temperature: number | null
  precipitationChance: number | null
  pm10Index: string | null
  pm25Index: string | null
}

// GET /api/seoul/city-data/{area} ("지금 이 지역은 어떤가요?")
export type CityDataResponse = {
  areaCode: string
  areaName: string
  // OffsetDateTime
  observedAt: string
  congestion: Congestion
  populationMin: number
  populationMax: number
  forecasts: CityPopulationForecast[]
  ageRatios: CityAgeRatio[]
  weather: CityWeather | null
}
