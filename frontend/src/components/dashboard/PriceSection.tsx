import { Alert, Button, Card, Tag } from 'antd'
import dayjs from 'dayjs'
import { useId, useState } from 'react'
import {
  LuChevronDown,
  LuChevronRight,
  LuChevronUp,
  LuInfo,
  LuTrendingDown,
  LuTrendingUp,
  LuCalendarX,
  LuListX,
} from 'react-icons/lu'
import { TbTriangleFilled, TbTriangleInvertedFilled } from 'react-icons/tb'
import EmptyState from '@/components/common/EmptyState'
import PriceTrendChart from '@/components/dashboard/PriceTrendChart'
import RecommendationList from '@/components/dashboard/RecommendationList'
import ItemEditModal from '@/components/items/ItemEditModal'
import { PRICE_STAGE_STYLE } from '@/lib/price/priceStage'
import type {
  IngredientPriceResponse,
  AnalysisResponse,
  BriefingResponse,
} from '@/lib/api/generated'
import type { InterestItems } from '@/types/search'
import type { PriceStage } from '@/types/price'

const stageMap: Record<
  NonNullable<IngredientPriceResponse['priceStage']>,
  PriceStage
> = {
  SEVERE_HIGH: 'RISE_SEVERE',
  ALERT_HIGH: 'RISE_ALERT',
  WATCH_HIGH: 'RISE_CAUTION',
  STABLE: 'STABLE',
  WATCH_LOW: 'FALL_CAUTION',
  ALERT_LOW: 'FALL_ALERT',
  SEVERE_LOW: 'FALL_SEVERE',
}
const weekLabels = ['4주 전', '3주 전', '2주 전', '1주 전', '기준일']

const stageOf = (item: IngredientPriceResponse) =>
  item.priceStage ? PRICE_STAGE_STYLE[stageMap[item.priceStage]] : undefined

type StageStyle = (typeof PRICE_STAGE_STYLE)[PriceStage]

function StageChip({
  stage,
  className = '',
}: {
  stage: StageStyle
  className?: string
}) {
  return (
    <span
      className={`shrink-0 rounded-full px-2 py-0.5 text-tag font-semibold whitespace-nowrap ${stage.chip} ${className}`}
    >
      {stage.label}
    </span>
  )
}

// 1주 변동: 빨강 ▲·파랑 ▼ 화살표와 금액·비율. 모바일 타일은 비율만
function WeekChange({
  item,
  detail = false,
  className = '',
}: {
  item: IngredientPriceResponse
  detail?: boolean
  className?: string
}) {
  const change = item.change1w
  if (!change || item.trend === 'FLAT' || change.amount === 0)
    return <span className={className}>변동 없음</span>
  const up = change.amount > 0
  const Mark = up ? TbTriangleFilled : TbTriangleInvertedFilled
  const amount = `${Math.abs(change.amount).toLocaleString()}원`
  const percent = `${Math.abs(change.percent)}%`
  return (
    <span
      className={`inline-flex flex-wrap items-center gap-x-1 ${up ? 'text-rise' : 'text-fall'} ${className}`}
    >
      <Mark
        role="img"
        aria-label={up ? '상승' : '하락'}
        className="size-[0.8em]"
      />
      {detail ? (
        `${amount} (${percent})`
      ) : (
        <>
          <span className="max-lg:hidden">
            {amount} ({percent})
          </span>
          <span className="lg:hidden">{percent}</span>
        </>
      )}
    </span>
  )
}

const SCALE: {
  value: NonNullable<IngredientPriceResponse['priceStage']>
  bar: string
}[] = [
  { value: 'SEVERE_LOW', bar: 'bg-stage-fall-3' },
  { value: 'ALERT_LOW', bar: 'bg-stage-fall-2' },
  { value: 'WATCH_LOW', bar: 'bg-stage-fall-2/40' },
  { value: 'STABLE', bar: 'bg-line-strong' },
  { value: 'WATCH_HIGH', bar: 'bg-stage-rise-2/40' },
  { value: 'ALERT_HIGH', bar: 'bg-stage-rise-2' },
  { value: 'SEVERE_HIGH', bar: 'bg-stage-rise-3' },
]

// 7단계 가격 막대(피드백 반영안). 지금 단계는 막대가 두껍고 이름이 굵다
function StageScale({
  current,
}: {
  current: NonNullable<IngredientPriceResponse['priceStage']>
}) {
  return (
    <div className="mt-4">
      <p className="text-tag font-semibold">가격 단계 · 4주 변동률 기준</p>
      <ol className="mt-2 grid grid-cols-7 gap-1">
        {SCALE.map(({ value, bar }) => {
          const active = value === current
          const label = PRICE_STAGE_STYLE[stageMap[value]].label
          return (
            <li
              key={value}
              aria-current={active ? 'true' : undefined}
              className="flex flex-col items-center gap-1 text-center"
            >
              <span
                className={`block w-full rounded-full ${bar} ${active ? 'h-2.5' : 'h-1.5 opacity-80'}`}
              />
              <span
                className={`text-tag ${active ? 'font-bold text-fg' : 'text-muted'}`}
              >
                {label}
              </span>
            </li>
          )
        })}
      </ol>
    </div>
  )
}

function monthSummary({ change4w, consecutiveWeeks }: IngredientPriceResponse) {
  if (!change4w || change4w.amount === 0) return '4주 동안 가격 변동이 없어요.'
  const up = change4w.amount > 0
  let text = `4주 전보다 ${Math.abs(change4w.amount).toLocaleString()}원(${Math.abs(change4w.percent)}%) ${up ? '올랐어요' : '내렸어요'}.`
  if ((consecutiveWeeks ?? 0) >= 2)
    text += ` ${consecutiveWeeks}주 연속 ${up ? '상승' : '하락'}이에요.`
  return text
}

export default function PriceSection({
  analysis,
  briefing,
  onSave,
  saving,
  saveFailed,
}: {
  analysis: AnalysisResponse
  briefing: BriefingResponse
  onSave: (items: InterestItems) => void
  saving: boolean
  saveFailed: boolean
}) {
  const [editOpen, setEditOpen] = useState(false)
  const [selectedId, setSelectedId] = useState<number>()
  const { ingredients, supplies, suggestions } = briefing
  const selected =
    ingredients.find((item) => item.itemId === selectedId) ?? ingredients[0]
  const trend =
    selected?.weeklyPrices?.map((price, index) => ({
      price: typeof price === 'number' ? price : null,
      week: weekLabels[index] ?? '',
    })) ?? []
  const stage = selected && stageOf(selected)
  const [chartOpen, setChartOpen] = useState(false)
  const chartId = useId()
  const raw = suggestions.filter(({ category }) => category === 'INGREDIENT')
  const sub = suggestions.filter(({ category }) => category === 'SUPPLY')
  const items = {
    raw: analysis.store.ingredients,
    sub: analysis.store.supplies,
  }

  return (
    <Card id="prices">
      <section aria-labelledby="prices-heading">
        <div className="flex flex-wrap items-start justify-between gap-2">
          <div>
            <h2 id="prices-heading" className="text-card">
              관심 품목 가격
            </h2>
            <p className="mt-1 text-caption text-muted">
              {analysis.store.industry.name} · 내가 선택한 품목
              {ingredients[0]?.baseDate &&
                ` · 한국농수산식품유통공사 · ${dayjs(ingredients[0].baseDate).format('M.D')} 기준`}
            </p>
          </div>
          <Button
            type="link"
            aria-haspopup="dialog"
            className="h-auto p-0 font-semibold"
            onClick={() => setEditOpen(true)}
          >
            품목 설정 <LuChevronRight aria-hidden />
          </Button>
        </div>
        {!ingredients.length && !supplies.length && (
          <div className="mt-4">
            <EmptyState
              title="아직 고른 품목이 없어요."
              description="품목 설정에서 원자재와 부자재를 골라 주세요."
            />
          </div>
        )}
        {selected && (
          <div className="mt-4">
            <div className="flex flex-wrap items-baseline justify-between gap-x-3">
              <h3 className="text-body-strong">
                원자재 · {ingredients.length}
              </h3>
              <span className="text-caption text-muted">
                가격 단계는 4주 변동률 기준이에요
              </span>
            </div>
            <div className="mt-2 grid grid-cols-[minmax(0,1fr)] gap-3 lg:grid-cols-[minmax(0,180px)_minmax(0,1fr)]">
              {/* 모바일은 타일 최소 너비를 rem으로 둬서 글자를 키우면 3칸 → 2칸으로 줄어든다 */}
              <div
                role="group"
                aria-label="가격을 볼 원자재"
                className="grid grid-cols-[repeat(auto-fit,minmax(6rem,1fr))] gap-2 lg:flex lg:flex-col"
              >
                {ingredients.map((item) => {
                  const itemStage = stageOf(item)
                  return (
                    <button
                      type="button"
                      key={item.itemId}
                      aria-pressed={item.itemId === selected.itemId}
                      onClick={() => setSelectedId(item.itemId)}
                      className="grid min-w-0 cursor-pointer content-start gap-1 rounded-xl border border-line-strong bg-surface p-3 text-left aria-pressed:border-primary-700 aria-pressed:bg-primary-50 aria-pressed:ring-1 aria-pressed:ring-primary-700"
                    >
                      <span className="flex items-start justify-between gap-1">
                        <span className="text-item">{item.name}</span>
                        {itemStage && (
                          <StageChip
                            stage={itemStage}
                            className="max-lg:hidden"
                          />
                        )}
                      </span>
                      <WeekChange item={item} className="text-body-strong" />
                      <span className="text-tag text-muted">
                        지난주 대비
                        {item.price != null && (
                          <span className="max-lg:hidden">
                            {' '}
                            · {item.unit} {item.price.toLocaleString()}원
                          </span>
                        )}
                      </span>
                      {itemStage && (
                        <StageChip
                          stage={itemStage}
                          className="justify-self-start lg:hidden"
                        />
                      )}
                    </button>
                  )
                })}
              </div>

              <div className="min-w-0 rounded-xl border border-divider p-4 lg:p-5">
                <div className="flex items-start justify-between gap-2">
                  <p className="text-caption text-muted">
                    {[selected.name, selected.grade, selected.unit]
                      .filter(Boolean)
                      .join(' · ')}{' '}
                    기준
                  </p>
                  {stage && <StageChip stage={stage} />}
                </div>
                <p className="text-tag text-muted">
                  한국농수산식품유통공사
                  {selected.baseDate &&
                    ` · ${dayjs(selected.baseDate).format('M.D')} 기준`}{' '}
                  · 같은 단위끼리 비교
                </p>
                {/* 품목을 바꾸면 새 가격을 낭독기에 알려준다 */}
                <div aria-live="polite">
                  {selected.price != null ? (
                    <>
                      <p className="mt-3 flex flex-wrap items-center gap-x-2 text-title">
                        <span className="sr-only">{selected.name}</span>
                        지난주보다
                        <WeekChange item={selected} detail />
                      </p>
                      <p className="mt-1 text-body">
                        기준 가격{' '}
                        <strong className="font-semibold">
                          {selected.unit} {selected.price.toLocaleString()}원
                        </strong>
                        {selected.change1w && (
                          <>
                            {' '}
                            · 지난주{' '}
                            {(
                              selected.price - selected.change1w.amount
                            ).toLocaleString()}
                            원
                          </>
                        )}
                      </p>
                    </>
                  ) : (
                    <p className="mt-3 flex items-center gap-2 text-caption text-muted">
                      <LuCalendarX aria-hidden className="shrink-0" />이 품목의
                      기준일 가격 자료가 없어요.
                    </p>
                  )}
                </div>
                {selected.priceStage && (
                  <StageScale current={selected.priceStage} />
                )}
                {selected.change4w && (
                  <p
                    className={`mt-4 flex items-start gap-2 rounded-xl p-3 text-caption font-semibold ${
                      selected.change4w.amount > 0
                        ? 'bg-stage-rise-1 text-stage-rise-ink'
                        : selected.change4w.amount < 0
                          ? 'bg-stage-fall-1 text-stage-fall-ink'
                          : 'bg-canvas text-muted'
                    }`}
                  >
                    {selected.change4w.amount >= 0 ? (
                      <LuTrendingUp aria-hidden className="mt-1 shrink-0" />
                    ) : (
                      <LuTrendingDown aria-hidden className="mt-1 shrink-0" />
                    )}
                    {monthSummary(selected)}
                  </p>
                )}
                {trend.some(({ price }) => price !== null) && (
                  <>
                    <Button
                      block
                      className="mt-3"
                      aria-expanded={chartOpen}
                      aria-controls={chartId}
                      icon={
                        chartOpen ? (
                          <LuChevronUp aria-hidden />
                        ) : (
                          <LuChevronDown aria-hidden />
                        )
                      }
                      iconPlacement="end"
                      onClick={() => setChartOpen((open) => !open)}
                    >
                      4주 추이 그래프 {chartOpen ? '접기' : '펼치기'}
                    </Button>
                    {chartOpen && (
                      <div id={chartId}>
                        <PriceTrendChart
                          data={trend}
                          color={stage?.ink ?? 'var(--color-muted)'}
                        />
                      </div>
                    )}
                  </>
                )}
              </div>
            </div>
          </div>
        )}
        {supplies.length > 0 && (
          <div className="mt-4 grid grid-cols-[minmax(0,1fr)] gap-2">
            <div className="flex flex-wrap items-baseline justify-between gap-x-3">
              <h3 className="text-body-strong">부자재 · {supplies.length}</h3>
              <span className="text-caption text-muted">
                가격 대신 준비 시점 안내
              </span>
            </div>
            {supplies.map((item) => (
              <div key={item.itemId} className="rounded-xl bg-canvas p-4">
                <p className="text-item">
                  {item.name}{' '}
                  {item.demand && (
                    <Tag color="warning">
                      {item.demand === 'DOWN' ? '수요 감소' : '수요 높음'}
                    </Tag>
                  )}
                </p>
                <p className="mt-1 flex items-center gap-2 text-caption text-muted">
                  {!item.prepareBy && (
                    <LuCalendarX aria-hidden className="shrink-0" />
                  )}
                  {item.prepareBy
                    ? [
                        item.triggerLabel &&
                          `${item.triggerLabel} ${item.leadDays ?? ''}일 전까지`.replace(
                            ' 일',
                            '일',
                          ),
                        dayjs(item.prepareBy).format('M.D(ddd)'),
                      ]
                        .filter(Boolean)
                        .join(' · ')
                    : '준비 시점 자료가 없어요.'}
                </p>
              </div>
            ))}
          </div>
        )}
        {(raw.length > 0 || sub.length > 0) && (
          <div className="mt-4 grid grid-cols-[minmax(0,1fr)] gap-3 lg:grid-cols-2">
            {raw.length > 0 ? (
              <RecommendationList
                analysisId={analysis.analysisId}
                title="원자재 추천 할 일"
                description="가격 자료에서 확인한 준비 항목이에요."
                tag="원자재"
                items={raw}
              />
            ) : (
              <EmptyState
                icon={LuListX}
                title="원자재 추천 할 일이 없어요."
                description={
                  ingredients.length
                    ? '가격 자료가 생기면 추천해 드려요.'
                    : '품목 설정에서 원자재를 고르면 추천해 드려요.'
                }
              />
            )}
            {sub.length > 0 ? (
              <RecommendationList
                analysisId={analysis.analysisId}
                title="부자재 추천 할 일"
                description="준비 시점에 맞춰 골랐어요."
                tag="부자재"
                items={sub}
              />
            ) : (
              <EmptyState
                icon={LuListX}
                title="부자재 추천 할 일이 없어요."
                description={
                  supplies.length
                    ? '준비 시점 자료가 생기면 추천해 드려요.'
                    : '품목 설정에서 부자재를 고르면 추천해 드려요.'
                }
              />
            )}
          </div>
        )}
        {ingredients.length > 0 && (
          <p className="mt-4 flex items-center gap-2 rounded-xl bg-canvas p-4 text-caption text-muted">
            <LuInfo aria-hidden />
            조사 가격은 실제 매입가와 다를 수 있어요.
          </p>
        )}
      </section>
      <ItemEditModal
        open={editOpen}
        items={items}
        onClose={() => setEditOpen(false)}
        onSave={onSave}
        saving={saving}
      >
        {saveFailed && (
          <Alert
            type="error"
            showIcon
            title="바뀐 품목으로 새 분석을 시작하지 못했어요."
          />
        )}
      </ItemEditModal>
    </Card>
  )
}
