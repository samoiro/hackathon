function field(value: unknown, key: string): unknown {
  return value !== null && typeof value === 'object' && key in value
    ? Reflect.get(value, key)
    : undefined
}

function rows(value: unknown): unknown[] {
  return Array.isArray(value) ? value : []
}

function string(value: unknown) {
  return typeof value === 'string' && value.trim() && value.trim() !== '-'
    ? value.trim()
    : undefined
}

function number(value: unknown) {
  const parsed = typeof value === 'number' ? value : Number(string(value))
  return Number.isFinite(parsed) ? parsed : undefined
}

const congestionLabels = new Map([
  ['RELAXED', '여유'],
  ['NORMAL', '보통'],
  ['SLIGHTLY_CROWDED', '약간 붐빔'],
  ['CROWDED', '붐빔'],
])

function readPopulation(row: unknown, rawPrefix: 'AREA' | 'FCST') {
  const level = string(
    field(row, 'congestion') ?? field(row, rawPrefix + '_CONGEST_LVL'),
  )
  return {
    congestion: level && (congestionLabels.get(level) ?? level),
    min: number(
      field(row, 'populationMin') ?? field(row, rawPrefix + '_PPLTN_MIN'),
    ),
    max: number(
      field(row, 'populationMax') ?? field(row, rawPrefix + '_PPLTN_MAX'),
    ),
  }
}

function time(value: unknown) {
  const timestamp = string(value)
  if (!timestamp) return undefined
  const iso = /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$/.test(timestamp)
    ? timestamp.replace(' ', 'T') + ':00+09:00'
    : timestamp
  const date = new Date(iso)
  return Number.isNaN(date.getTime()) ? undefined : date
}

export function readCityData(data: unknown) {
  // 로컬 구버전 서버는 서울시 원본 응답을 반환해 화면용 응답과 함께 읽는다.
  const rawPopulation = rows(field(data, 'LIVE_PPLTN_STTS'))[0]
  const rawCommerce = field(data, 'LIVE_CMRCL_STTS')
  const consumption =
    field(data, 'consumptionRatios') ??
    [10, 20, 30, 40, 50, 60].map((ageGroup) => ({
      ageGroup,
      percent: field(rawCommerce, 'CMRCL_' + ageGroup + '_RATE'),
    }))
  const rawWeather = rows(field(data, 'WEATHER_STTS'))[0]
  const rawWeatherForecast = rows(field(rawWeather, 'FCST24HOURS'))[0]
  const weather = field(data, 'weather')

  return {
    population: readPopulation(rawPopulation ?? data, 'AREA'),
    observedAt: time(
      field(data, 'observedAt') ?? field(rawPopulation, 'PPLTN_TIME'),
    ),
    forecasts: rows(
      field(data, 'forecasts') ?? field(rawPopulation, 'FCST_PPLTN'),
    ).flatMap((row) => {
      const date = time(field(row, 'time') ?? field(row, 'FCST_TIME'))
      return date ? [{ time: date, ...readPopulation(row, 'FCST') }] : []
    }),
    consumption: rows(consumption).flatMap((row) => {
      const ageGroup = number(field(row, 'ageGroup'))
      const percent = number(field(row, 'percent'))
      return ageGroup != null &&
        percent != null &&
        percent >= 0 &&
        percent <= 100
        ? [{ ageGroup, percent }]
        : []
    }),
    weather: {
      sky: string(
        field(weather, 'sky') ?? field(rawWeatherForecast, 'SKY_STTS'),
      ),
      temperature: number(
        field(weather, 'temperature') ?? field(rawWeather, 'TEMP'),
      ),
      rainChance: number(
        field(weather, 'precipitationChance') ??
          field(rawWeatherForecast, 'RAIN_CHANCE'),
      ),
    },
    events: rows(field(data, 'events') ?? field(data, 'EVENT_STTS')).flatMap(
      (row) => {
        const title = string(field(row, 'name') ?? field(row, 'EVENT_NM'))
        const latitude = number(field(row, 'latitude') ?? field(row, 'EVENT_Y'))
        const longitude = number(
          field(row, 'longitude') ?? field(row, 'EVENT_X'),
        )
        return title &&
          latitude != null &&
          longitude != null &&
          Math.abs(latitude) <= 90 &&
          Math.abs(longitude) <= 180
          ? [{ title, latitude, longitude }]
          : []
      },
    ),
  }
}
