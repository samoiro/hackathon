import { Tag } from 'antd'
import type { ReactNode } from 'react'
import {
  LuCalendar,
  LuChevronRight,
  LuMapPin,
  LuTrendingUp,
} from 'react-icons/lu'
import { TbTriangleFilled } from 'react-icons/tb'
import { PRICE_STAGE_STYLE } from '@/lib/price/priceStage'

const SUMMARIES: {
  icon: ReactNode
  label: string
  href: string
  value: ReactNode
  detail: string
}[] = [
  {
    icon: <LuCalendar aria-hidden />,
    label: '주변 행사',
    href: '#events',
    value: (
      <>
        2건 <span className="text-body font-normal text-muted">이번 주</span>
      </>
    ),
    detail: '금 16:00 서울숲 음악회 · 650m',
  },
  {
    icon: <LuTrendingUp aria-hidden />,
    label: '가격 변동',
    href: '#prices',
    value: (
      <>
        배추
        {/* 1주 변동은 화살표로만 색을 쓰고, 가격 단계(4주 변동률 기준)는 배지로 */}
        <span className="flex items-center gap-3">
          <TbTriangleFilled
            role="img"
            aria-label="상승"
            className="size-5 text-rise"
          />
          4.1%
        </span>
        <Tag className={PRICE_STAGE_STYLE.STABLE.chip}>
          {PRICE_STAGE_STYLE.STABLE.label}
        </Tag>
      </>
    ),
    detail: '돼지고기 1.2% 하락 · 쌀 변동 없음',
  },
  {
    icon: <LuMapPin aria-hidden />,
    label: '지금 서울숲공원',
    href: '#area',
    value: (
      <>
        <span
          aria-hidden
          className="size-3.5 shrink-0 rounded-full bg-success-solid"
        />
        <span className="text-success">여유</span>
        <span className="text-body font-normal text-muted">약 2.9만 명</span>
      </>
    ),
    detail: '맑음 8°C · 18시에는 약간 붐빔 예상',
  },
]

// 행사·품목이 없을 때 신호 카드 내용
const EMPTY_SUMMARIES: Record<
  string,
  Pick<(typeof SUMMARIES)[number], 'value' | 'detail'>
> = {
  '주변 행사': {
    value: '0건',
    detail: '1.5km까지 넓혀도 예정 행사가 없어요',
  },
  '가격 변동': {
    value: <span className="text-item">품목을 골라 주세요</span>,
    detail: '고르면 가격 변동을 여기서 바로 볼 수 있어요.',
  },
}

export default function DashboardHero({ empty = false }: { empty?: boolean }) {
  return (
    <section
      aria-labelledby="hero-heading"
      className="rounded-[20px] bg-hero p-5 text-on-hero lg:p-8"
    >
      {/* 모바일(BI-MO-03)은 캐릭터를 오른쪽 위로 보낸다 */}
      <div className="flex items-center gap-4 max-lg:flex-row-reverse max-lg:justify-between">
        {/* 전신 캐릭터는 어두운 바탕 위에 둔다(브랜드가이드 06). 제목이 뜻을 전하니 꾸밈 그림으로 둔다 */}
        <img
          src="/images/mascot.png"
          alt=""
          width={88}
          height={86}
          className="shrink-0 max-lg:w-18"
        />
        <div>
          <p className="text-caption font-semibold text-primary-300">
            11월 17일 월요일 · 오전 7시 기준
          </p>
          <h1 id="hero-heading" className="mt-1 text-display">
            오늘의 1분 경영회의
          </h1>
        </div>
      </div>
      {/* PC는 카드 3장, 모바일은 흰 카드 하나에 세 줄(아이콘 · 이름과 수치 · ›) */}
      <ul className="mt-4 grid gap-4 max-lg:gap-0 max-lg:divide-y max-lg:divide-divider max-lg:rounded-xl max-lg:bg-surface max-lg:px-4 lg:grid-cols-3">
        {SUMMARIES.map((base) => {
          const summary = {
            ...base,
            ...(empty ? EMPTY_SUMMARIES[base.label] : {}),
          }
          return (
            <li key={summary.label}>
              <a
                href={summary.href}
                className="grid grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-x-3 text-fg max-lg:py-4 lg:h-full lg:content-start lg:gap-x-2 lg:rounded-xl lg:bg-surface lg:p-6"
              >
                <span className="flex size-10 items-center justify-center rounded-full bg-canvas max-lg:row-span-2 lg:size-8">
                  {summary.icon}
                </span>
                <span className="text-caption font-semibold text-muted lg:text-item lg:text-fg">
                  {summary.label}
                </span>
                <LuChevronRight aria-hidden className="max-lg:row-span-2" />
                <span className="flex flex-wrap items-center gap-x-3 text-item lg:col-span-3 lg:mt-3 lg:text-display">
                  {summary.value}
                </span>
                <span className="col-span-3 mt-2 text-body max-lg:hidden">
                  {summary.detail}
                </span>
              </a>
            </li>
          )
        })}
      </ul>
    </section>
  )
}
