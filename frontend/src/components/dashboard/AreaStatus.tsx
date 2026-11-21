import { useQuery } from '@tanstack/react-query'
import { Alert, Button, Card, Progress, Select } from 'antd'
import { useId, useState } from 'react'
import {
  LuChevronDown,
  LuChevronUp,
  LuInfo,
  LuMapPin,
  LuRefreshCw,
  LuSun,
} from 'react-icons/lu'
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

const hourMinute = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

// 혼잡도 4단계 색. 색만으로 구분하지 않도록 단계 이름을 항상 함께 쓴다
const CONGESTION: Record<string, { pill: string; dot: string }> = {
  여유: { pill: 'bg-success-bg text-success', dot: 'bg-success-solid' },
  보통: { pill: 'bg-info-bg text-info', dot: 'bg-info' },
  '약간 붐빔': { pill: 'bg-warning-bg text-warning', dot: 'bg-warning' },
  붐빔: { pill: 'bg-danger-bg text-danger', dot: 'bg-danger' },
}

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
  sheet = false,
}: {
  label: string
  address?: Address
  events: EventResponse[]
  weather?: WeatherResponse
  // 모바일 아래 시트(BI-MO-03-2) 안에 넣을 때. 제목은 시트가 보여주므로 카드와 제목을 뺀다
  sheet?: boolean
}) {
  const [selectedCode, setSelectedCode] = useState<string>()
  // 가게 좌표(가게 정보 입력 때 저장한 값)를 보내면 서버가 가까운 순으로 준다. 첫 번째가 가게가 든 영역
  const point = address
    ? { latitude: address.latitude, longitude: address.longitude }
    : undefined
  const catalog = useQuery({
    queryKey: ['areas', point],
    queryFn: async ({ signal }) =>
      (await areas({ query: point, signal, throwOnError: true })).data,
    staleTime: 60 * 60 * 1000,
    retry: false,
  })
  const options = catalog.data ?? []
  const selected =
    options.find((area) => area.code === selectedCode) ??
    (address ? options[0] : options.find((area) => area.name === label))
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

  const map =
    address || selected ? (
      <AreaMap
        address={address}
        area={selected}
        events={[...events, ...(current?.events ?? [])]}
      />
    ) : (
      <p className="mt-4 text-caption text-muted">
        지도에 표시할 가게 위치를 확인하지 못했어요.
      </p>
    )
  const [detailOpen, setDetailOpen] = useState(false)
  const detailId = useId()
  const showDetail = sheet || detailOpen
  const level = current?.population.congestion
  const levelStyle = level ? CONGESTION[level] : undefined
  const { male, female } = current?.gender ?? {}
  const refresh = () => {
    void catalog.refetch()
    if (selected) void city.refetch()
  }
  const refreshButton = (
    <Button
      size="small"
      aria-label="지역 현황 새로고침"
      title="지역 현황 새로고침"
      icon={<LuRefreshCw aria-hidden />}
      loading={city.isFetching || catalog.isFetching}
      className="shrink-0 max-lg:size-11"
      onClick={refresh}
    />
  )

  const content = (
    <>
      <div className="mt-4 flex items-center gap-2">
        <Select
          aria-label="집계 장소"
          className="min-w-0 flex-1"
          prefix={<LuMapPin aria-hidden />}
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
        {/* 시트는 제목 줄에 자리가 없어 선택칸 옆에 둔다 */}
        {sheet && refreshButton}
      </div>
      <p className="mt-2 text-caption text-muted">
        가게가 포함된 집계 영역 기준이에요.
      </p>
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

      {/* 날씨 한 줄(피드백 반영안): 왼쪽 하늘·기온, 오른쪽 강수 확률·출처 */}
      {(weatherSummary || forecast) && (
        <div className="mt-4 flex flex-wrap items-center justify-between gap-x-3 gap-y-1 rounded-xl bg-canvas px-4 py-3 text-caption">
          <span className="flex items-center gap-2 text-body-strong">
            <LuSun aria-hidden />
            {weatherSummary
              ? [
                  cityWeather?.sky,
                  cityWeather?.temperature == null
                    ? null
                    : `${cityWeather.temperature}°C`,
                ]
                  .filter(Boolean)
                  .join(' · ')
              : [
                  forecast?.sky,
                  forecast?.maxTemperature == null
                    ? null
                    : `${forecast.maxTemperature}°C`,
                ]
                  .filter(Boolean)
                  .join(' · ')}
          </span>
          <span className="text-muted">
            {[
              (weatherSummary
                ? cityWeather?.rainChance
                : forecast?.precipitationChance) == null
                ? null
                : `강수 확률 ${weatherSummary ? cityWeather?.rainChance : forecast?.precipitationChance}%`,
              weatherSummary ? null : '기상청',
            ]
              .filter(Boolean)
              .join(' · ')}
          </span>
        </div>
      )}

      {sheet && map}

      {current && (
        <>
          <div className="mt-4 flex flex-wrap items-baseline justify-between gap-x-3">
            <h3 className="text-body-strong">현재 인구·혼잡도</h3>
            {current.observedAt && (
              <span className="text-caption text-muted">
                {fullTime.format(current.observedAt)} 기준
              </span>
            )}
          </div>
          <p className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-2">
            {level && (
              <span
                className={`inline-flex items-center gap-2 rounded-xl px-3 py-2 text-item ${levelStyle?.pill ?? 'bg-canvas'}`}
              >
                <span
                  aria-hidden
                  className={`size-2.5 rounded-full ${levelStyle?.dot ?? 'bg-muted'}`}
                />
                {level}
              </span>
            )}
            <span className="text-item">
              {population ?? '집계 자료가 없어요.'}
            </span>
          </p>
          {/* 혼잡도 4단계 범례. 지금 단계는 색 + '지금' 글자로 함께 알린다 */}
          <ul aria-label="혼잡도 단계" className="mt-3 flex flex-wrap gap-1">
            {Object.entries(CONGESTION).map(([name, style]) => (
              <li key={name}>
                <span
                  className={`inline-block rounded-full px-2 py-0.5 text-tag ${name === level ? `${style.pill} font-semibold` : 'bg-canvas text-muted'}`}
                >
                  {name}
                  {name === level && <span className="sr-only"> (지금)</span>}
                </span>
              </li>
            ))}
          </ul>

          {male != null && female != null && (
            <>
              <h3 className="mt-4 text-body-strong">실시간 성별 인구 비율</h3>
              <dl className="mt-2 flex flex-col gap-1 text-caption">
                {[
                  ['남성', male],
                  ['여성', female],
                ].map(([name, percent]) => (
                  <div key={name} className="flex items-center gap-3">
                    <dt className="w-8 shrink-0 text-muted">{name}</dt>
                    <dd className="flex flex-1 items-center gap-3">
                      <Progress
                        percent={Number(percent)}
                        showInfo={false}
                        strokeColor="var(--color-border)"
                        className="m-0 flex-1"
                        aria-label={`${name} 비율`}
                      />
                      <span className="w-10 text-right font-semibold">
                        {percent}%
                      </span>
                    </dd>
                  </div>
                ))}
              </dl>
            </>
          )}
        </>
      )}

      {/* PC는 지도·시간대·연령을 접어 두고, 모바일 시트는 모두 펼친다 */}
      {!sheet && (address || selected || current) && (
        <Button
          block
          className="mt-4"
          aria-expanded={detailOpen}
          aria-controls={detailId}
          icon={
            detailOpen ? (
              <LuChevronUp aria-hidden />
            ) : (
              <LuChevronDown aria-hidden />
            )
          }
          iconPlacement="end"
          onClick={() => setDetailOpen((open) => !open)}
        >
          지도·시간대·연령 {detailOpen ? '접기' : '펼치기'}
        </Button>
      )}
      {showDetail && (
        <div id={detailId}>
          {!sheet && map}
          {current && current.forecasts.length > 0 && (
            <>
              <h3 className="mt-4 border-t border-divider pt-4 text-body-strong">
                이후 예상 혼잡도
              </h3>
              <ul className="mt-2 flex flex-col gap-2 text-caption">
                {current.forecasts.map((row) => (
                  <li
                    key={row.time.toISOString()}
                    className="flex flex-wrap items-center gap-x-3"
                  >
                    <time
                      dateTime={row.time.toISOString()}
                      className="w-12 font-semibold"
                    >
                      {hourMinute.format(row.time)}
                    </time>
                    {row.congestion && (
                      <span
                        className={`rounded-full px-2 py-0.5 text-tag ${CONGESTION[row.congestion]?.pill ?? 'bg-canvas'}`}
                      >
                        {row.congestion}
                      </span>
                    )}
                    <span className="ml-auto text-muted">
                      {formatPopulation(row.min, row.max)}
                    </span>
                  </li>
                ))}
              </ul>
            </>
          )}
          {current && (
            <>
              <h3 className="mt-4 border-t border-divider pt-4 text-body-strong">
                연령별 소비 비율
              </h3>
              {current.consumption.length ? (
                <dl className="mt-2 flex flex-col gap-1 text-caption">
                  {current.consumption.map(({ ageGroup, percent }) => (
                    <div key={ageGroup} className="flex items-center gap-3">
                      <dt className="w-10 shrink-0 text-muted">
                        {ageGroup}대{ageGroup === 60 && '+'}
                      </dt>
                      <dd className="flex flex-1 items-center gap-3">
                        <Progress
                          percent={percent}
                          showInfo={false}
                          strokeColor="var(--color-border)"
                          className="m-0 flex-1"
                          aria-label={`${ageGroup}대${ageGroup === 60 ? ' 이상' : ''} 소비 비율`}
                        />
                        <span className="w-10 text-right font-semibold">
                          {percent}%
                        </span>
                      </dd>
                    </div>
                  ))}
                </dl>
              ) : (
                <p className="mt-1 text-caption text-muted">
                  이 지역의 소비 자료가 없어요.
                </p>
              )}
              <p className="mt-3 flex gap-2 rounded-xl bg-canvas p-3 text-caption text-muted">
                <LuInfo aria-hidden className="mt-1 shrink-0" />이 집계 영역의
                연령대별 소비 비율이에요. 우리 가게 고객 비율이 아니고, 혼잡도가
                곧 매출 증가를 뜻하지도 않아요.
              </p>
            </>
          )}
        </div>
      )}
    </>
  )

  if (sheet) return content

  return (
    <Card id="area">
      <section aria-labelledby="area-heading">
        <div className="flex items-center justify-between gap-2">
          <h2 id="area-heading" className="text-card">
            지금 이 지역은 어떤가요?
          </h2>
          {refreshButton}
        </div>
        {content}
      </section>
    </Card>
  )
}
