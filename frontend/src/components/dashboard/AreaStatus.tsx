import { useQuery } from '@tanstack/react-query'
import { Alert, Button, Card, Progress, Select } from 'antd'
import dayjs from 'dayjs'
import { useState } from 'react'
import { LuSun } from 'react-icons/lu'
import AreaMap from '@/components/dashboard/AreaMap'
import { readCityData } from '@/lib/api/cityData'
import { areas, cityData } from '@/lib/api/generated'
import type { EventResponse, WeatherResponse } from '@/lib/api/generated'
import type { Address } from '@/types/search'

const fullTime = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  month: 'numeric',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

function formatPopulation(min?: number, max?: number) {
  if (min == null || max == null) return null
  return min === max
    ? `${min.toLocaleString('ko-KR')}명`
    : `${min.toLocaleString('ko-KR')}~${max.toLocaleString('ko-KR')}명`
}

export default function AreaStatus({
  label,
  address,
  events,
  weather,
}: {
  label: string
  address?: Address
  events: EventResponse[]
  weather?: WeatherResponse
}) {
  const [selectedCode, setSelectedCode] = useState<string>()
  const catalog = useQuery({
    queryKey: ['areas'],
    queryFn: async ({ signal }) =>
      (await areas({ signal, throwOnError: true })).data,
    staleTime: 60 * 60 * 1000,
    retry: false,
  })
  const options = catalog.data ?? []
  const selected =
    options.find((area) => area.code === selectedCode) ??
    options.find((area) => area.name === label) ??
    (address
      ? options.reduce<(typeof options)[number] | undefined>(
          (nearest, area) => {
            const distance = (point: typeof area) =>
              (point.latitude - address.latitude) ** 2 +
              ((point.longitude - address.longitude) *
                Math.cos((address.latitude * Math.PI) / 180)) **
                2
            return !nearest || distance(area) < distance(nearest)
              ? area
              : nearest
          },
          undefined,
        )
      : undefined)
  const city = useQuery({
    queryKey: ['city-data', selected?.name],
    queryFn: async ({ signal }) => {
      if (!selected) throw new Error('집계 지역을 선택해 주세요.')
      const { data } = await cityData({
        path: { area: selected.name },
        signal,
        throwOnError: true,
      })
      return readCityData(data)
    },
    enabled: Boolean(selected),
    staleTime: 5 * 60 * 1000,
    retry: false,
  })
  const current = city.data
  const population = formatPopulation(
    current?.population.min,
    current?.population.max,
  )
  const forecast = weather?.forecasts[0]
  const cityWeather = current?.weather
  const weatherSummary = [
    cityWeather?.sky,
    cityWeather?.temperature == null ? null : `${cityWeather.temperature}°C`,
    cityWeather?.rainChance == null
      ? null
      : `강수 확률 ${cityWeather.rainChance}%`,
  ]
    .filter(Boolean)
    .join(' · ')

  return (
    <Card id="area">
      <section aria-labelledby="area-heading">
        <h2 id="area-heading" className="text-card">
          지금 이 지역은 어떤가요?
        </h2>
        {label && <p className="mt-2 text-caption text-muted">{label}</p>}
        <div className="mt-4">
          <label
            htmlFor="area-select"
            className="mb-2 block text-caption font-semibold"
          >
            집계 지역
          </label>
          <Select
            id="area-select"
            className="w-full"
            showSearch={{ optionFilterProp: 'label' }}
            placeholder="장소를 선택해 주세요"
            value={selected?.code}
            onChange={setSelectedCode}
            loading={catalog.isPending}
            disabled={options.length === 0}
            options={options.map(({ code, name }) => ({
              value: code,
              label: name,
            }))}
          />
        </div>
        {catalog.isError ? (
          <Alert
            className="mt-3"
            type="error"
            showIcon
            title="장소 목록을 불러오지 못했어요."
            action={
              <Button
                size="small"
                loading={catalog.isFetching}
                onClick={() => void catalog.refetch()}
              >
                다시 시도
              </Button>
            }
          />
        ) : catalog.isSuccess && options.length === 0 ? (
          <p className="mt-3 text-caption text-muted">
            조회할 수 있는 장소가 없어요.
          </p>
        ) : null}
        {selected && city.isPending && (
          <p role="status" className="mt-3 text-caption text-muted">
            지역 현황을 불러오고 있어요.
          </p>
        )}
        {city.isError && (
          <Alert
            className="mt-3"
            type="error"
            showIcon
            title="지역 현황을 불러오지 못했어요."
            action={
              <Button
                size="small"
                loading={city.isFetching}
                onClick={() => void city.refetch()}
              >
                다시 시도
              </Button>
            }
          />
        )}
        {weatherSummary ? (
          <div className="mt-4 flex items-center gap-2 rounded-xl bg-canvas p-4 text-caption">
            <LuSun aria-hidden /> {weatherSummary}
          </div>
        ) : (
          forecast && (
            <>
              <div className="mt-4 flex flex-wrap items-center justify-between gap-2 rounded-xl bg-canvas p-4 text-caption">
                <span className="flex items-center gap-2 font-semibold">
                  <LuSun aria-hidden />
                  가게 주변 {dayjs(forecast.date).format('M.D')} 예보 ·{' '}
                  {forecast.sky}{' '}
                  {forecast.maxTemperature != null &&
                    `${forecast.maxTemperature}°C`}
                </span>
                {forecast.precipitationChance != null && (
                  <span className="text-muted">
                    강수 확률 {forecast.precipitationChance}%
                  </span>
                )}
              </div>
              {weather?.checkedAt && (
                <p className="mt-2 text-caption text-muted">
                  {dayjs(weather.checkedAt).format('M.D HH:mm')} 확인 · 케이웨더
                </p>
              )}
            </>
          )
        )}
        {address || selected ? (
          <AreaMap
            address={address}
            area={selected}
            events={[...events, ...(current?.events ?? [])]}
          />
        ) : (
          <p className="mt-4 text-caption text-muted">
            지도에 표시할 가게 위치를 확인하지 못했어요.
          </p>
        )}
        {current && (
          <div className="mt-4 flex flex-col gap-3 text-caption">
            <div className="rounded-xl bg-canvas p-4">
              <h3 className="font-semibold">현재 인구 · 혼잡도</h3>
              <p className="mt-1">
                {[population, current.population.congestion]
                  .filter(Boolean)
                  .join(' · ') || '집계 자료가 없어요.'}
              </p>
              {current.observedAt && (
                <p className="text-muted">
                  {fullTime.format(current.observedAt)} 기준 · 서울시
                </p>
              )}
              {current.forecasts.length > 0 && (
                <>
                  <h4 className="mt-3 font-semibold">예상 혼잡도</h4>
                  <ul className="mt-1 max-h-40 space-y-1 overflow-y-auto">
                    {current.forecasts.map((row) => (
                      <li
                        key={row.time.toISOString()}
                        className="flex flex-wrap justify-between gap-x-2"
                      >
                        <time dateTime={row.time.toISOString()}>
                          {fullTime.format(row.time)}
                        </time>
                        <span>
                          {[row.congestion, formatPopulation(row.min, row.max)]
                            .filter(Boolean)
                            .join(' · ')}
                        </span>
                      </li>
                    ))}
                  </ul>
                </>
              )}
            </div>
            <div className="rounded-xl bg-canvas p-4">
              <h3 className="font-semibold">연령대별 소비 비율</h3>
              {current.consumption.length ? (
                <ul className="mt-2 space-y-1">
                  {current.consumption.map(({ ageGroup, percent }) => (
                    <li key={ageGroup} className="flex items-center gap-3">
                      <span className="shrink-0">
                        {ageGroup}대{ageGroup === 60 && ' 이상'}
                      </span>
                      <Progress
                        percent={percent}
                        size="small"
                        aria-label={`${ageGroup}대${ageGroup === 60 ? ' 이상' : ''} 소비 비율`}
                      />
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="mt-1 text-muted">이 지역의 소비 자료가 없어요.</p>
              )}
            </div>
          </div>
        )}
      </section>
    </Card>
  )
}
