import dayjs from 'dayjs'
import type { ReactNode } from 'react'
import type { IconType } from 'react-icons'
import {
  LuCalendarDays,
  LuChevronRight,
  LuTrendingUp,
  LuTriangleAlert,
} from 'react-icons/lu'
import { TbTriangleFilled, TbTriangleInvertedFilled } from 'react-icons/tb'
import { useTodos } from '@/lib/dashboard/todos'
import type { AnalysisResponse, BriefingResponse } from '@/lib/api/generated'

type Change = {
  icon: IconType
  href: string
  title: ReactNode
  source: string
}

const md = (date?: string) => (date ? dayjs(date).format('M.D') : '')

// 상단 강조 블록(피드백 반영안): 왼쪽 '이번 주 중요한 변화', 오른쪽 '먼저 할 일 3개'.
// 변화는 행사·가격·날씨(부자재) 근거에서 한 줄씩, 없는 근거는 줄을 뺀다
export default function DashboardHero({
  analysis,
  briefing,
}: {
  analysis: AnalysisResponse
  briefing: BriefingResponse
}) {
  const { todos } = useTodos()
  const strong = todos.filter((todo) => todo.strong)
  const remaining = todos.filter((todo) => !todo.done).length
  const [event, nextEvent] = briefing.events
  const price = briefing.ingredients.find(
    (item) => item.change1w && item.trend !== 'FLAT',
  )
  const supply = briefing.suggestions.find((item) => item.category === 'SUPPLY')
  const PriceMark =
    price?.trend === 'DOWN' ? TbTriangleInvertedFilled : TbTriangleFilled

  const changes: Change[] = []
  if (event)
    changes.push({
      icon: LuCalendarDays,
      href: '#events',
      title: [
        `${dayjs(event.startDate).format('ddd')} ${event.timeText?.split('–')[0] ?? ''} ${event.title} ${event.distanceM}m`,
        nextEvent &&
          `${nextEvent.title} ${nextEvent.status === 'ONGOING' ? '진행 중' : '예정'}`,
      ]
        .filter(Boolean)
        .join(' · '),
      source: `서울시 문화행사 정보 · ${md(briefing.sources[0]?.checkedAt ?? analysis.baseTime)} 기준`,
    })
  if (price?.change1w)
    changes.push({
      icon: LuTrendingUp,
      href: '#prices',
      title: (
        <>
          {price.name} 지난주보다{' '}
          <span
            className={`inline-flex items-center gap-0.5 ${price.trend === 'DOWN' ? 'text-fall' : 'text-rise'}`}
          >
            <PriceMark
              role="img"
              aria-label={price.trend === 'DOWN' ? '하락' : '상승'}
              className="size-3"
            />
            {Math.abs(price.change1w.percent)}%
          </span>
          {price.price != null &&
            ` · ${price.unit ?? ''} ${price.price.toLocaleString()}원`}
        </>
      ),
      source: `한국농수산식품유통공사 · ${md(price.baseDate)} 기준`,
    })
  if (supply)
    changes.push({
      icon: LuTriangleAlert,
      href: '#prices',
      title: supply.reason,
      source: briefing.weather
        ? `기상청 단기예보 · ${md(briefing.weather.checkedAt)} 기준`
        : '부자재 준비표',
    })

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
            {dayjs(analysis.baseTime).format('M월 D일 dddd · A h시')} 기준
          </p>
          <h1 id="hero-heading" className="mt-1 text-display">
            오늘의 1분 경영회의
          </h1>
        </div>
      </div>

      <div className="mt-4 grid grid-cols-[minmax(0,1fr)] gap-4 text-fg lg:mt-6 lg:grid-cols-2 lg:gap-3">
        <section
          aria-labelledby="changes-heading"
          className="rounded-xl bg-surface p-4 lg:p-5"
        >
          <h2
            id="changes-heading"
            className="flex items-center gap-2 text-body-strong"
          >
            이번 주 중요한 변화
            <span className="rounded-full bg-primary-50 px-2 text-tag text-brand-text">
              {changes.length}가지
            </span>
          </h2>
          {changes.length > 0 ? (
            <ul className="mt-2 divide-y divide-divider border-t border-divider">
              {changes.map(({ icon: Icon, href, title, source }) => (
                <li key={source}>
                  <a
                    href={href}
                    className="flex items-center gap-3 py-3 hover:bg-canvas"
                  >
                    <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-canvas">
                      <Icon aria-hidden />
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="block text-body-strong">{title}</span>
                      <span className="block text-tag text-muted">
                        {source}
                      </span>
                    </span>
                    <LuChevronRight aria-hidden className="shrink-0" />
                  </a>
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-3 text-caption text-muted">
              이번 주에 눈에 띄는 변화가 없어요.
            </p>
          )}
        </section>

        <section
          aria-labelledby="first-tasks-heading"
          className="flex flex-col rounded-xl bg-surface p-4 lg:p-5"
        >
          <div className="flex items-center gap-2">
            <h2 id="first-tasks-heading" className="text-body-strong">
              먼저 할 일 {strong.length}개
            </h2>
            <span className="rounded-full bg-hero px-2 text-tag text-on-hero">
              AI 추천
            </span>
            <a
              href="#today"
              className="ml-auto inline-flex min-h-6 items-center text-caption font-semibold text-brand-text max-lg:hidden"
            >
              모두 보기 · {remaining}건 남음
            </a>
            <span className="ml-auto text-caption font-semibold text-brand-text lg:hidden">
              {remaining}건 남음
            </span>
          </div>
          <ol className="mt-2 flex-1 divide-y divide-divider border-t border-divider">
            {strong.slice(0, 3).map((todo, index) => (
              <li key={todo.id} className="flex items-center gap-3 py-3">
                <span className="flex size-8 shrink-0 items-center justify-center rounded-full border-2 border-primary-500 text-body-strong text-brand-text">
                  {index + 1}
                </span>
                <span className="min-w-0">
                  <span
                    className={`block text-body-strong ${todo.done ? 'text-muted line-through' : ''}`}
                  >
                    {todo.title}
                  </span>
                  <span className="block text-tag font-semibold text-brand-text">
                    준비 기한 ·{' '}
                    {todo.dueDate
                      ? `${dayjs(todo.dueDate).format('M.D(ddd)')}까지`
                      : '이번 주 발주 전까지'}
                  </span>
                </span>
              </li>
            ))}
          </ol>
          <p className="mt-2 text-tag text-muted">
            다시 들어오면 담아 둔 할 일이 이 자리에 먼저 보여요.
          </p>
        </section>
      </div>
    </section>
  )
}
