import {
  LuCalendarDays,
  LuChevronRight,
  LuMapPin,
  LuTrendingUp,
} from 'react-icons/lu'
import dayjs from 'dayjs'
import type { AnalysisResponse, BriefingResponse } from '@/lib/api/generated'

export default function DashboardHero({
  analysis,
  briefing,
}: {
  analysis: AnalysisResponse
  briefing: BriefingResponse
}) {
  const price = briefing.ingredients.find((item) => item.change1w)
  const forecast = briefing.weather?.forecasts[0]
  const signals = [
    {
      label: '주변 행사',
      href: '#events',
      icon: LuCalendarDays,
      value: `${briefing.events.length}건`,
      detail:
        briefing.events[0]?.title ??
        briefing.seasonEvents[0]?.label ??
        '조회된 주변 행사가 없어요.',
    },
    {
      label: '가격 변동',
      href: '#prices',
      icon: LuTrendingUp,
      value: price?.name ?? '자료 없음',
      detail: price?.change1w
        ? `1주 전 대비 ${price.change1w.percent}%`
        : '확인할 가격 변동 자료가 없어요.',
    },
    {
      label: '지역 현황',
      href: '#area',
      icon: LuMapPin,
      value: analysis.store.areaLabel || analysis.store.gu,
      detail: forecast
        ? `${dayjs(forecast.date).format('M월 D일')} 예보 · ${forecast.sky}`
        : '날씨 자료가 없어요.',
    },
  ]
  return (
    <section
      aria-labelledby="hero-heading"
      className="rounded-[20px] bg-hero p-5 text-on-hero lg:p-8"
    >
      <div className="flex items-center gap-4 max-lg:flex-row-reverse max-lg:justify-between">
        <img
          src="/images/mascot.png"
          alt=""
          width={88}
          height={86}
          className="shrink-0 max-lg:w-18"
        />
        <div>
          <p className="text-caption font-semibold text-primary-300">
            {dayjs(analysis.baseTime).format('M월 D일 dddd · HH:mm')} 기준
          </p>
          <h1 id="hero-heading" className="mt-1 text-display">
            오늘의 1분 경영회의
          </h1>
        </div>
      </div>
      <ul className="mt-4 grid gap-4 max-lg:gap-0 max-lg:divide-y max-lg:divide-divider max-lg:rounded-xl max-lg:bg-surface max-lg:px-4 lg:grid-cols-3">
        {signals.map(({ label, href, icon: Icon, value, detail }) => (
          <li key={href}>
            <a
              href={href}
              className="grid grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-x-3 text-fg max-lg:py-4 lg:gap-x-2 lg:rounded-xl lg:bg-surface lg:p-6"
            >
              <span className="flex size-10 items-center justify-center rounded-full bg-canvas max-lg:row-span-2 lg:size-8">
                <Icon aria-hidden />
              </span>
              <span className="text-caption font-semibold text-muted lg:text-item lg:text-fg">
                {label}
              </span>
              <LuChevronRight aria-hidden className="max-lg:row-span-2" />
              <span className="text-item lg:col-span-3 lg:mt-3 lg:text-display">
                {value}
              </span>
              <span className="col-span-3 mt-2 text-body max-lg:hidden">
                {detail}
              </span>
            </a>
          </li>
        ))}
      </ul>
    </section>
  )
}
