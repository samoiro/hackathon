import { Card } from 'antd'
import dayjs from 'dayjs'
import { LuSun } from 'react-icons/lu'
import AreaMap from '@/components/dashboard/AreaMap'
import type { Address } from '@/types/search'
import type { EventResponse, WeatherResponse } from '@/lib/api/generated'

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
  const forecast = weather?.forecasts[0]
  return (
    <Card id="area">
      <section aria-labelledby="area-heading">
        <h2 id="area-heading" className="text-card">
          지금 이 지역은 어떤가요?
        </h2>
        <p className="mt-2 text-caption text-muted">{label}</p>
        {forecast && (
          <div className="mt-4 flex flex-wrap items-center justify-between gap-2 rounded-xl bg-canvas p-4 text-caption">
            <span className="flex items-center gap-2 font-semibold">
              <LuSun aria-hidden />
              {dayjs(forecast.date).format('M.D')} 예보 · {forecast.sky}{' '}
              {forecast.maxTemperature != null &&
                `${forecast.maxTemperature}°C`}
            </span>
            {forecast.precipitationChance != null && (
              <span className="text-muted">
                강수 확률 {forecast.precipitationChance}%
              </span>
            )}
          </div>
        )}
        {weather?.checkedAt && (
          <p className="mt-2 text-caption text-muted">
            {dayjs(weather.checkedAt).format('M.D HH:mm')} 확인 · 케이웨더
          </p>
        )}
        {address ? (
          <AreaMap address={address} events={events} />
        ) : (
          <p className="mt-4 text-caption text-muted">
            지도에 표시할 가게 위치를 확인하지 못했어요.
          </p>
        )}
        <p className="mt-4 rounded-xl bg-canvas p-4 text-caption text-muted">
          현재 인구·혼잡도와 연령별 소비 자료는 준비 중이에요.
        </p>
      </section>
    </Card>
  )
}
