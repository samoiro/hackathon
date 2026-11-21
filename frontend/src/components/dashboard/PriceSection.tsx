import { Alert, Button, Card, Tag } from 'antd'
import dayjs from 'dayjs'
import { useState } from 'react'
import { LuChevronRight, LuInfo } from 'react-icons/lu'
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
  const stage = selected?.priceStage
    ? PRICE_STAGE_STYLE[stageMap[selected.priceStage]]
    : undefined
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
          <div className="mt-4 grid gap-3">
            <h3 className="text-body-strong">원자재 · {ingredients.length}</h3>
            <div className="grid gap-2 sm:grid-cols-3">
              {ingredients.map((item) => {
                const active = item.itemId === selected.itemId
                const Icon =
                  item.trend === 'UP'
                    ? TbTriangleFilled
                    : TbTriangleInvertedFilled
                return (
                  <button
                    type="button"
                    key={item.itemId}
                    aria-pressed={active}
                    onClick={() => setSelectedId(item.itemId)}
                    className="grid min-w-0 cursor-pointer gap-1 rounded-xl border border-divider p-3 text-left aria-pressed:border-primary-700 aria-pressed:bg-primary-50"
                  >
                    <span className="text-item">{item.name}</span>
                    <span className="text-body-strong tabular-nums">
                      {item.price != null
                        ? `${item.price.toLocaleString()}원`
                        : '가격 자료 없음'}
                    </span>
                    {item.change1w && (
                      <span
                        className={`flex items-center gap-1 text-caption ${item.trend === 'UP' ? 'text-rise' : item.trend === 'DOWN' ? 'text-fall' : 'text-muted'}`}
                      >
                        {item.trend && item.trend !== 'FLAT' && (
                          <Icon aria-hidden />
                        )}
                        {item.trend === 'FLAT'
                          ? '변동 없음'
                          : `${Math.abs(item.change1w.percent)}% ${item.trend === 'UP' ? '상승' : item.trend === 'DOWN' ? '하락' : '변동'}`}
                      </span>
                    )}
                  </button>
                )
              })}
            </div>
            <div className="rounded-xl bg-canvas p-4">
              <div className="flex flex-wrap items-center gap-2">
                <h4 className="text-body-strong">
                  {[selected.name, selected.grade, selected.unit]
                    .filter(Boolean)
                    .join(' · ')}
                </h4>
                {stage && (
                  <span
                    className={`rounded-full px-3 py-1 text-tag ${stage.chip}`}
                  >
                    {stage.label}
                  </span>
                )}
              </div>
              {selected.price != null ? (
                <p className="mt-1 text-display tabular-nums">
                  {selected.price.toLocaleString()}원
                </p>
              ) : (
                <p className="mt-2 text-caption text-muted">
                  이 품목의 기준일 가격 자료가 없어요.
                </p>
              )}
              {trend.some(({ price }) => price !== null) && (
                <PriceTrendChart
                  data={trend}
                  color={
                    selected.trend === 'UP'
                      ? 'var(--color-rise)'
                      : selected.trend === 'DOWN'
                        ? 'var(--color-fall)'
                        : 'var(--color-muted)'
                  }
                />
              )}
              {selected.change4w && (
                <p className="mt-2 text-caption text-muted">
                  4주 전 대비 {selected.change4w.amount.toLocaleString()}원 (
                  {selected.change4w.percent}%)
                </p>
              )}
              {selected.baseDate && (
                <p className="mt-2 text-caption text-muted">
                  {dayjs(selected.baseDate).format('M월 D일')} 기준 ·
                  한국농수산식품유통공사
                </p>
              )}
            </div>
          </div>
        )}
        {supplies.length > 0 && (
          <div className="mt-4 grid gap-2">
            <h3 className="text-body-strong">부자재 · {supplies.length}</h3>
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
                <p className="text-caption text-muted">
                  {item.prepareBy
                    ? `${dayjs(item.prepareBy).format('M월 D일')}까지 준비`
                    : '준비 시점 자료가 없어요.'}
                  {item.triggerLabel && ` · ${item.triggerLabel}`}
                </p>
              </div>
            ))}
          </div>
        )}
        {(raw.length > 0 || sub.length > 0) && (
          <div className="mt-4 grid gap-3 lg:grid-cols-2">
            {raw.length > 0 && (
              <RecommendationList
                analysisId={analysis.analysisId}
                title="원자재 추천 할 일"
                description="가격 자료에서 확인한 준비 항목이에요."
                tag="원자재"
                items={raw}
                stacked
              />
            )}
            {sub.length > 0 && (
              <RecommendationList
                analysisId={analysis.analysisId}
                title="부자재 추천 할 일"
                description="준비 시점에 맞춰 골랐어요."
                tag="부자재"
                items={sub}
                stacked
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
