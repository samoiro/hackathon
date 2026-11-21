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
import type { IngredientGroup } from '@/types/price'
import type { InterestItems } from '@/types/search'
import { PRICE_STAGE_STYLE, stageOf } from '@/lib/price/priceStage'
import PriceTrendChart from '@/components/dashboard/PriceTrendChart'
import RecommendationList from '@/components/dashboard/RecommendationList'

// shortcut: 조사 가격 API 연결 전 가짜 4주 가격. 증감 문구는 모두 이 값에서 계산한다
const PRICE_ITEMS: {
  name: string
  unit: string
  group: IngredientGroup
  prices: number[]
}[] = [
  {
    name: '배추',
    group: '채소류',
    unit: '상품 · 1포기',
    prices: [3950, 4020, 4110, 4208, 4380],
  },
  {
    name: '돼지고기',
    group: '축산물',
    unit: '삼겹살 · 100g',
    prices: [2780, 2740, 2700, 2683, 2650],
  },
  {
    name: '쌀',
    group: '식량작물',
    unit: '상품 · 20kg',
    prices: [54200, 54200, 54200, 54200, 54200],
  },
]

const WEEKS = ['4주 전', '3주 전', '2주 전', '1주 전', '기준일']

const won = (value: number) => `${value.toLocaleString()}원`
const percent = (diff: number, base: number) =>
  `${Math.abs((diff / base) * 100).toFixed(1)}%`

// 색은 4주 변동률의 7단계(stageOf)가 정한다. 1주 변동은 빨강 ▲·파랑 ▼ 화살표로만 보여주고 단계에는 넣지 않는다
// shortcut: 4주 변동률을 '4주 전 평균' 대신 4주 전 값으로 계산한다. API가 change4w를 주면 그 값을 쓴다
function describe(prices: number[], group: IngredientGroup) {
  const last = prices[prices.length - 1]
  const prev = prices[prices.length - 2]
  const first = prices[0]
  const weekDiff = last - prev
  const monthDiff = last - first
  const steps = prices.slice(1).map((price, i) => price - prices[i])
  const direction = weekDiff > 0 ? 'rise' : weekDiff < 0 ? 'fall' : 'flat'
  const stage = PRICE_STAGE_STYLE[stageOf((monthDiff / first) * 100, group)]
  const Mark = {
    rise: TbTriangleFilled,
    fall: TbTriangleInvertedFilled,
    flat: null,
  }[direction]
  const markTone = { rise: 'text-rise', fall: 'text-fall', flat: '' }[direction]
  const Icon =
    monthDiff > 0 ? LuTrendingUp : monthDiff < 0 ? LuTrendingDown : LuMinus

  let summary = '4주 동안 가격 변동이 없어요.'
  if (monthDiff !== 0) {
    const verb = monthDiff > 0 ? '올랐어요' : '내렸어요'
    summary = `4주 전보다 ${won(Math.abs(monthDiff))}(${percent(monthDiff, first)}) ${verb}.`
    if (steps.every((step) => step > 0)) summary += ' 4주 연속 상승이에요.'
    if (steps.every((step) => step < 0)) summary += ' 4주 연속 하락이에요.'
  }

  return {
    last,
    stage,
    Mark,
    markTone,
    Icon,
    weekLabel: { rise: '상승', fall: '하락', flat: '변동 없음' }[direction],
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
  const detail = describe(selected.prices, selected.group)
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
              <span className="text-muted max-lg:hidden">
                하나를 고르면 4주 추이를 볼 수 있어요
              </span>
            </div>
            <div className="mt-2 grid gap-3 lg:grid-cols-[160px_1fr]">
              <Radio.Group
                aria-label="가격을 볼 원자재"
                value={selectedName}
                onChange={(event) => setSelectedName(event.target.value)}
                className="flex gap-2 lg:flex-col"
              >
                {PRICE_ITEMS.map((item) => {
                  const info = describe(item.prices, item.group)
                  const checked = item.name === selectedName
                  return (
                    <Radio
                      key={item.name}
                      value={item.name}
                      className="relative m-0 flex min-w-0 cursor-pointer rounded-xl max-lg:flex-1 border border-line-strong bg-surface p-3 has-[:checked]:border-primary-700 has-[:checked]:bg-primary-50 has-[:checked]:ring-1 has-[:checked]:ring-primary-700 has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-focus [&_.ant-radio]:sr-only"
                    >
                      {checked && (
                        <LuCircleCheck
                          aria-hidden
                          className="absolute top-3 right-3 text-primary-700 max-lg:top-2 max-lg:right-2"
                        />
                      )}
                      <span className="block text-item">{item.name}</span>
                      <span className="block text-caption text-muted">
                        {won(info.last)}
                      </span>
                      <Tag className={`mt-1 self-start ${info.stage.chip}`}>
                        {info.stage.label}
                      </Tag>
                    </Radio>
                  )
                })}
              </Radio.Group>

              <div className="rounded-xl border border-divider p-4">
                <div className="flex justify-between text-caption text-muted">
                  <span>
                    {selected.name} · {selected.unit}
                  </span>
                  <Tag className={detail.stage.chip}>{detail.stage.label}</Tag>
                </div>
                {/* 품목을 바꾸면 새 가격을 낭독기에 알려준다 */}
                <p
                  aria-live="polite"
                  className="mt-1 flex flex-wrap items-baseline gap-x-3"
                >
                  <span className="sr-only">{selected.name} </span>
                  <span className="text-display tabular-nums">
                    {won(detail.last)}
                  </span>
                  <span className="flex items-center gap-1 text-body font-semibold whitespace-nowrap">
                    {detail.Mark && '1주 전 대비'}
                    {detail.Mark && (
                      <detail.Mark
                        role="img"
                        aria-label={detail.weekLabel}
                        className={`size-3.5 ${detail.markTone}`}
                      />
                    )}
                    {detail.weekChange}
                  </span>
                </p>
                <PriceTrendChart
                  key={selected.name}
                  color={detail.stage.ink}
                  data={selected.prices.map((price, index) => ({
                    week: WEEKS[index],
                    price,
                  }))}
                />
                <p
                  className={`mt-2 flex items-center gap-2 text-caption font-semibold ${detail.stage.inkClass}`}
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

            <div className="mt-4 grid gap-3 lg:grid-cols-2">
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
