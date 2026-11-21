import { Button, Card, Radio, Tag } from 'antd'
import {
  LuChevronRight,
  LuCircleCheck,
  LuInfo,
  LuMinus,
  LuPlus,
  LuTrendingDown,
  LuTrendingUp,
} from 'react-icons/lu'
import { TbTriangleFilled, TbTriangleInvertedFilled } from 'react-icons/tb'
import { Link } from 'react-router'
import { useState } from 'react'
import EmptyState from '@/components/common/EmptyState'
import ItemEditModal from '@/components/items/ItemEditModal'
import type { InterestItems } from '@/types/search'
import PriceTrendChart from '@/components/dashboard/PriceTrendChart'
import RecommendationList from '@/components/dashboard/RecommendationList'

// shortcut: 조사 가격 API 연결 전 가짜 4주 가격. 증감 문구는 모두 이 값에서 계산한다
const PRICE_ITEMS = [
  {
    name: '배추',
    unit: '상품 · 1포기',
    prices: [3950, 4020, 4110, 4208, 4380],
  },
  {
    name: '돼지고기',
    unit: '삼겹살 · 100g',
    prices: [2780, 2740, 2700, 2683, 2650],
  },
  {
    name: '쌀',
    unit: '상품 · 20kg',
    prices: [54200, 54200, 54200, 54200, 54200],
  },
]

const WEEKS = ['4주 전', '3주 전', '2주 전', '1주 전', '기준일']

const won = (value: number) => `${value.toLocaleString()}원`
const percent = (diff: number, base: number) =>
  `${Math.abs((diff / base) * 100).toFixed(1)}%`

// 한국식 증감: 상승 빨강 ▲, 하락 파랑 ▼. 색만으로 구분하지 않도록 삼각형 아이콘과 글자를 같이 쓴다
function describe(prices: number[]) {
  const last = prices[prices.length - 1]
  const prev = prices[prices.length - 2]
  const first = prices[0]
  const weekDiff = last - prev
  const monthDiff = last - first
  const steps = prices.slice(1).map((price, i) => price - prices[i])
  const direction = weekDiff > 0 ? 'rise' : weekDiff < 0 ? 'fall' : 'flat'
  const tone = { rise: 'text-rise', fall: 'text-fall', flat: 'text-muted' }[
    direction
  ]
  const color = {
    rise: 'var(--color-rise)',
    fall: 'var(--color-fall)',
    flat: 'var(--color-muted)',
  }[direction]
  const Mark = {
    rise: TbTriangleFilled,
    fall: TbTriangleInvertedFilled,
    flat: null,
  }[direction]
  const Icon = { rise: LuTrendingUp, fall: LuTrendingDown, flat: LuMinus }[
    direction
  ]

  let summary = '4주 동안 가격 변동이 없어요.'
  if (monthDiff !== 0) {
    const verb = monthDiff > 0 ? '올랐어요' : '내렸어요'
    summary = `4주 전보다 ${won(Math.abs(monthDiff))}(${percent(monthDiff, first)}) ${verb}.`
    if (steps.every((step) => step > 0)) summary += ' 4주 연속 상승이에요.'
    if (steps.every((step) => step < 0)) summary += ' 4주 연속 하락이에요.'
  }

  return {
    last,
    tone,
    color,
    Mark,
    Icon,
    badge: { rise: '상승', fall: '하락', flat: '변동 없음' }[direction],
    tileChange:
      direction === 'flat'
        ? '변동 없음'
        : `${percent(weekDiff, prev)} ${direction === 'rise' ? '상승' : '하락'}`,
    weekChange:
      direction === 'flat'
        ? '1주 전과 같아요'
        : `${won(Math.abs(weekDiff))} (${percent(weekDiff, prev)})`,
    summary,
  }
}

export default function PriceSection({ empty = false }: { empty?: boolean }) {
  const [editOpen, setEditOpen] = useState(false)
  const [selectedName, setSelectedName] = useState(PRICE_ITEMS[0].name)
  const selected =
    PRICE_ITEMS.find((item) => item.name === selectedName) ?? PRICE_ITEMS[0]
  const detail = describe(selected.prices)
  const [items, setItems] = useState<InterestItems>({
    raw: ['쌀', '돼지고기', '배추'],
    sub: ['포장 용기'],
  })

  return (
    <Card id="prices">
      <section aria-labelledby="prices-heading">
        <div className="flex items-start justify-between">
          <div>
            <h2 id="prices-heading" className="text-card">
              관심 품목 가격
            </h2>
            <p className="mt-1 text-caption text-muted">
              {empty
                ? '아직 고른 품목이 없어요.'
                : '일반 음식점 · 내가 선택한 품목'}
            </p>
          </div>
          {!empty && (
            <Button
              type="link"
              aria-haspopup="dialog"
              className="h-auto p-0 font-semibold"
              onClick={() => setEditOpen(true)}
            >
              품목 설정 <LuChevronRight aria-hidden />
            </Button>
          )}
        </div>

        {empty ? (
          <div className="mt-4">
            <EmptyState
              icon={LuTrendingUp}
              title="품목을 고르면 가격과 준비 시점을 알려드려요."
              description="일반 음식점에서 많이 고르는 품목: 쌀 · 돼지고기 · 배추"
              actions={
                <>
                  <Button icon={<LuPlus aria-hidden />}>
                    추천 품목 한 번에 담기
                  </Button>
                  <Link
                    to="/"
                    className="flex items-center gap-1 font-semibold text-brand-text underline"
                  >
                    직접 고르기 <LuChevronRight aria-hidden />
                  </Link>
                </>
              }
            />
          </div>
        ) : (
          <>
            <div className="mt-4 flex justify-between text-caption">
              <h3 className="text-body-strong">원자재 · 3</h3>
              <span className="text-muted">
                하나를 고르면 4주 추이를 볼 수 있어요
              </span>
            </div>
            <div className="mt-2 grid grid-cols-[160px_1fr] gap-3">
              <Radio.Group
                aria-label="가격을 볼 원자재"
                value={selectedName}
                onChange={(event) => setSelectedName(event.target.value)}
                className="flex flex-col gap-2"
              >
                {PRICE_ITEMS.map((item) => {
                  const info = describe(item.prices)
                  const checked = item.name === selectedName
                  return (
                    <Radio
                      key={item.name}
                      value={item.name}
                      className="relative m-0 flex cursor-pointer rounded-xl border border-line-strong bg-surface p-3 has-[:checked]:border-primary-700 has-[:checked]:bg-primary-50 has-[:checked]:ring-1 has-[:checked]:ring-primary-700 has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-focus [&_.ant-radio]:sr-only"
                    >
                      {checked && (
                        <LuCircleCheck
                          aria-hidden
                          className="absolute top-3 right-3 text-primary-700"
                        />
                      )}
                      <span className="block text-item">{item.name}</span>
                      <span className="block text-caption text-muted">
                        {won(info.last)}
                      </span>
                      <span
                        className={`flex items-center gap-1 text-caption font-semibold ${info.tone}`}
                      >
                        {info.Mark && (
                          <info.Mark aria-hidden className="size-3" />
                        )}
                        {info.tileChange}
                      </span>
                    </Radio>
                  )
                })}
              </Radio.Group>

              <div className="rounded-xl border border-divider p-4">
                <div className="flex justify-between text-caption text-muted">
                  <span>
                    {selected.name} · {selected.unit}
                  </span>
                  <span
                    className={`flex items-center gap-1 font-semibold ${detail.tone}`}
                  >
                    {detail.Mark && (
                      <detail.Mark aria-hidden className="size-3" />
                    )}
                    {detail.badge}
                  </span>
                </div>
                {/* 품목을 바꾸면 새 가격을 낭독기에 알려준다 */}
                <p
                  aria-live="polite"
                  className="mt-1 flex items-baseline gap-3"
                >
                  <span className="sr-only">{selected.name} </span>
                  <span className="text-display tabular-nums">
                    {won(detail.last)}
                  </span>
                  <span
                    className={`flex items-center gap-1 text-body font-semibold ${detail.tone}`}
                  >
                    {detail.Mark && '1주 전 대비'}
                    {detail.Mark && (
                      <detail.Mark
                        role="img"
                        aria-label={detail.badge}
                        className="size-3.5"
                      />
                    )}
                    {detail.weekChange}
                  </span>
                </p>
                <PriceTrendChart
                  key={selected.name}
                  color={detail.color}
                  data={selected.prices.map((price, index) => ({
                    week: WEEKS[index],
                    price,
                  }))}
                />
                <p
                  className={`mt-2 flex items-center gap-2 text-caption font-semibold ${detail.tone}`}
                >
                  <detail.Icon aria-hidden />
                  {detail.summary}
                </p>
                <p className="mt-2 text-tag text-muted">
                  기준일 11.16 · 한국농수산식품유통공사 · 같은 단위끼리 비교
                </p>
              </div>
            </div>

            <div className="mt-4 flex justify-between text-caption">
              <h3 className="text-body-strong">부자재 · 1</h3>
              <span className="text-muted">가격 대신 준비 시점 안내</span>
            </div>
            <div className="mt-2 flex items-center justify-between rounded-xl border border-divider p-4">
              <div>
                <p className="text-item">포장 용기</p>
                <p className="text-caption text-muted">
                  행사 3일 전까지 · 11.18(화)
                </p>
              </div>
              <Tag color="warning">수요 높음</Tag>
            </div>

            <div className="mt-4 grid grid-cols-2 gap-3">
              <RecommendationList
                stacked
                tag="원자재"
                title="원자재 추천 할 일"
                description="가격 흐름에 맞췄어요"
                items={[
                  {
                    strong: true,
                    title: '배추 거래처 단가 확인',
                    effect: '다음 발주 전에 단가 변동을 미리 확인할 수 있어요.',
                  },
                  {
                    title: '돼지고기 발주량 점검',
                    effect: '가격이 내린 시점의 발주 계획을 살펴볼 수 있어요.',
                  },
                ]}
              />
              <RecommendationList
                stacked
                tag="부자재"
                title="부자재 추천 할 일"
                description="준비 시점에 맞췄어요"
                items={[
                  {
                    strong: true,
                    title: '포장 용기 재고 확인',
                    effect: '행사 전 포장 주문 증가에 대비할 수 있어요.',
                  },
                  {
                    title: '포장 용기 추가 주문 시점 확인',
                    effect:
                      '행사 3일 전까지 받을 수 있는지 미리 확인할 수 있어요.',
                  },
                ]}
              />
            </div>
            <p className="mt-4 flex items-center gap-2 rounded-xl bg-canvas p-4 text-caption text-muted">
              <LuInfo aria-hidden />
              조사 가격은 실제 매입가와 다를 수 있어요.
            </p>
          </>
        )}
      </section>
      <ItemEditModal
        open={editOpen}
        items={items}
        onClose={() => setEditOpen(false)}
        onSave={setItems}
      />
    </Card>
  )
}
