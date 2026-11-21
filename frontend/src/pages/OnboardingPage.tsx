import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, Button, Card, Select, Tag } from 'antd'
import { isAxiosError } from 'axios'
import { useState } from 'react'
import {
  LuArrowRight,
  LuCheck,
  LuChevronDown,
  LuChevronUp,
  LuClock,
  LuInfo,
  LuListChecks,
  LuMapPin,
  LuSearch,
} from 'react-icons/lu'
import { useNavigate } from 'react-router'
import ChoiceChips from '@/components/common/ChoiceChips'
import AppLayout from '@/components/layout/AppLayout'
import StepIndicator from '@/components/layout/StepIndicator'
import InterestItemsEditor from '@/components/items/InterestItemsEditor'
import LocationSearchModal from '@/components/onboarding/LocationSearchModal'
import type { InterestItems, StoreLocation } from '@/types/search'
import { industries, recommendedItems } from '@/lib/api/generated'
import { readExcludedCards, useStartAnalysis } from '@/lib/api/analysis'

const BENEFITS = [
  {
    icon: LuClock,
    title: '1분',
    body: '길게 읽지 않아도 오늘 할 일이 보여요',
  },
  { icon: LuInfo, title: '근거', body: '출처와 기준 시각을 함께 보여드려요' },
  {
    icon: LuListChecks,
    title: '준비',
    body: '읽고 끝이 아니라 체크할 일로 남겨요',
  },
]

type SalesType = 'STORE' | 'DELIVERY' | 'BOTH'
type Draft = {
  location?: StoreLocation
  industryId?: number
  salesType?: SalesType
  items: InterestItems
}

const SALES_TYPES: { value: SalesType; label: string }[] = [
  { value: 'STORE', label: '매장만' },
  { value: 'DELIVERY', label: '배달만' },
  { value: 'BOTH', label: '매장·배달 모두' },
]

function RequiredTag() {
  return <Tag>필수</Tag>
}

export default function OnboardingPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const draft = queryClient.getQueryData<Draft>(['onboarding-draft'])
  const [locationOpen, setLocationOpen] = useState(false)
  const [location, setLocation] = useState(draft?.location)
  const [industryId, setIndustryId] = useState(draft?.industryId)
  const [moreOpen, setMoreOpen] = useState(false)
  const [salesType, setSalesType] = useState(draft?.salesType)
  const [items, setItems] = useState<InterestItems>(
    draft?.items ?? { raw: [], sub: [] },
  )
  const [submitted, setSubmitted] = useState(false)
  const catalog = useQuery({
    queryKey: ['industries'],
    queryFn: async ({ signal }) =>
      (await industries({ signal, throwOnError: true })).data,
  })
  const recommendations = useQuery({
    queryKey: ['industry-recommendations', industryId],
    enabled: industryId !== undefined,
    queryFn: async ({ signal }) => {
      if (industryId === undefined) throw new Error('업종을 골라 주세요.')
      return (
        await recommendedItems({
          path: { industryId },
          signal,
          throwOnError: true,
        })
      ).data
    },
  })
  const start = useStartAnalysis()
  const industry = catalog.data?.find(({ id }) => id === industryId)
  const ready = Boolean(location && industryId !== undefined)
  const suggested = recommendations.data

  function submit() {
    setSubmitted(true)
    if (start.isPending || !location || industryId === undefined) return
    queryClient.setQueryData<Draft>(['onboarding-draft'], {
      location,
      industryId,
      salesType,
      items,
    })
    start.mutate(
      {
        storeName: location.name,
        roadAddress: location.address.road,
        latitude: location.address.latitude,
        longitude: location.address.longitude,
        industryId,
        operationType: salesType,
        ingredientIds: items.raw.map(({ id }) => id),
        supplyIds: items.sub.map(({ id }) => id),
        excludedKeys: readExcludedCards(),
      },
      {
        onSuccess: ({ analysisId }) =>
          navigate(`/briefing?id=${encodeURIComponent(analysisId)}`),
      },
    )
  }

  function addRecommendations() {
    if (!suggested) return
    setItems((current) => ({
      raw: [
        ...current.raw,
        ...suggested.ingredients.filter(
          (item) => !current.raw.some(({ id }) => id === item.id),
        ),
      ].slice(0, 3),
      sub: [
        ...current.sub,
        ...suggested.supplies.filter(
          (item) => !current.sub.some(({ id }) => id === item.id),
        ),
      ].slice(0, 2),
    }))
    setMoreOpen(true)
  }

  const summary = [
    ['가게 이름', location?.name ?? '아직 입력하지 않았어요'],
    ['위치', location?.address.road ?? '아직 고르지 않았어요'],
    ['업종', industry?.name ?? '아직 고르지 않았어요'],
    ...(moreOpen || salesType || items.raw.length || items.sub.length
      ? [
          [
            '운영 형태',
            SALES_TYPES.find((type) => type.value === salesType)?.label ??
              '없음',
          ],
          ['원자재', items.raw.map(({ name }) => name).join(' · ') || '없음'],
          ['부자재', items.sub.map(({ name }) => name).join(' · ') || '없음'],
        ]
      : [['선택 항목', '아직 고르지 않았어요']]),
  ]

  return (
    <AppLayout
      title="가게 정보 입력"
      headerContext={
        <div className="mx-auto">
          <StepIndicator current={1} />
        </div>
      }
    >
      <p className="font-semibold text-brand-text">
        사장님의 하루를 바꾸는 1분
      </p>
      <h1 className="mt-2 text-display">가게 정보를 알려주세요</h1>
      <p className="mt-3 text-muted">
        가게와 업종만 알려주시면 바로 시작할 수 있어요.
      </p>

      <div className="mt-6 grid grid-cols-[minmax(0,1fr)] items-start gap-4 lg:mt-8 lg:grid-cols-[minmax(0,736fr)_minmax(0,360fr)] lg:gap-6">
        <div className="flex flex-col gap-4 lg:gap-6">
          <Card>
            <section aria-labelledby="store-heading">
              <div className="flex items-center gap-2">
                <h2 id="store-heading" className="text-item font-bold">
                  가게 이름과 위치
                </h2>
                <RequiredTag />
              </div>
              <div className="mt-4 flex items-center gap-3 rounded-xl border border-border p-4 lg:gap-4 lg:px-6 lg:py-5">
                <LuMapPin aria-hidden className="size-6 shrink-0" />
                <div className="flex-1">
                  <p className="text-body-strong">
                    {location?.name ?? '가게 이름과 주소를 입력해 주세요'}
                  </p>
                  {location && <p>{location.address.road}</p>}
                </div>
                <Button size="small" onClick={() => setLocationOpen(true)}>
                  {location ? '변경' : '주소 검색'}
                </Button>
              </div>
              <p className="mt-3 text-caption text-muted">
                주소를 기준으로 도보 15분(직선 1km) 안의 행사를 찾아요. 현재
                서울 지역만 지원해요.
              </p>
            </section>

            <section
              aria-labelledby="category-heading"
              className="mt-6 border-t border-divider pt-6"
            >
              <div className="flex items-center gap-2">
                <h2 id="category-heading" className="text-item font-bold">
                  업종
                </h2>
                <RequiredTag />
              </div>
              <Select<number>
                aria-labelledby="category-heading"
                className="mt-4 w-full"
                value={industryId}
                onChange={setIndustryId}
                options={catalog.data?.map(({ id, name, category }) => ({
                  value: id,
                  label: `${category} · ${name}`,
                }))}
                showSearch={{ optionFilterProp: 'label' }}
                loading={catalog.isPending}
                status={catalog.isError ? 'error' : undefined}
                notFoundContent={
                  catalog.isError
                    ? '업종을 불러오지 못했어요.'
                    : '검색 결과가 없어요.'
                }
                placeholder="업종을 골라 주세요"
                prefix={<LuSearch aria-hidden />}
              />
              {catalog.isError && (
                <Button type="link" onClick={() => void catalog.refetch()}>
                  업종 다시 불러오기
                </Button>
              )}
              <p className="mt-3 text-caption text-muted">
                업종에 맞는 기능만 골라 보여드려요.
              </p>
            </section>
          </Card>

          <Card>
            <section aria-labelledby="more-heading">
              <div className="flex items-start gap-4">
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <h2 id="more-heading" className="text-item font-bold">
                      더 알려주시면 브리핑이 구체적이 돼요
                    </h2>
                    <Tag>선택 2개</Tag>
                  </div>
                  <p className="mt-1 text-caption text-muted">
                    운영 형태 · 관심 품목. 나중에 대시보드에서도 고칠 수 있어요.
                  </p>
                </div>
                <Button
                  aria-expanded={moreOpen}
                  aria-controls={moreOpen ? 'more-panel' : undefined}
                  aria-label={moreOpen ? '선택 항목 접기' : '선택 항목 펼치기'}
                  icon={
                    moreOpen ? (
                      <LuChevronUp aria-hidden />
                    ) : (
                      <LuChevronDown aria-hidden />
                    )
                  }
                  onClick={() => setMoreOpen((open) => !open)}
                />
              </div>

              {industryId !== undefined && recommendations.isPending && (
                <p className="mt-4 text-caption text-muted" role="status">
                  추천 품목을 찾고 있어요.
                </p>
              )}
              {recommendations.isError && (
                <Button
                  type="link"
                  onClick={() => void recommendations.refetch()}
                >
                  추천 품목 다시 불러오기
                </Button>
              )}
              {suggested &&
                suggested.ingredients.length + suggested.supplies.length >
                  0 && (
                  <div className="mt-4 flex flex-wrap items-center justify-between gap-3 rounded-xl bg-canvas p-4">
                    <p className="text-caption">
                      <strong>업종 추천 품목</strong>
                      <br />
                      {[...suggested.ingredients, ...suggested.supplies]
                        .map(({ name }) => name)
                        .join(' · ')}
                    </p>
                    <Button size="small" onClick={addRecommendations}>
                      한 번에 담기
                    </Button>
                  </div>
                )}
              {moreOpen && (
                <div id="more-panel" className="mt-6 border-t border-divider">
                  <div className="border-b border-divider py-6">
                    <div className="flex items-center gap-2">
                      <h3 className="text-item">운영 형태</h3>
                      <Tag>선택</Tag>
                    </div>
                    <div className="mt-4">
                      <ChoiceChips<SalesType>
                        block
                        label="운영 형태"
                        value={salesType}
                        onChange={setSalesType}
                        options={SALES_TYPES}
                      />
                    </div>
                    <p className="mt-3 text-caption text-muted">
                      선택한 운영 방식에 맞춰 준비할 일을 제안해요.
                    </p>
                  </div>

                  <div className="pt-6">
                    <div className="flex items-center gap-2">
                      <h3 className="text-item">관심 품목</h3>
                      <Tag>선택</Tag>
                    </div>
                    <div className="mt-4">
                      <InterestItemsEditor value={items} onChange={setItems} />
                    </div>
                    <p className="mt-4 flex items-center gap-2 rounded-xl bg-canvas p-4 text-caption text-muted">
                      <LuInfo aria-hidden />
                      조사 가격은 실제 매입가와 다를 수 있어요.
                    </p>
                  </div>
                </div>
              )}
            </section>
          </Card>
        </div>

        {/* 모바일(BI-MO-01)은 요약·안내 카드 없이 아래 고정 버튼만 둔다 */}
        <div className="flex flex-col gap-6 max-lg:hidden">
          <Card>
            <section aria-labelledby="summary-heading">
              <h2 id="summary-heading" className="text-item font-bold">
                선택한 가게 요약
              </h2>
              <dl className="mt-4 flex flex-col gap-3 border-b border-divider pb-5 text-caption">
                {summary.map(([term, value]) => (
                  <div key={term} className="flex justify-between gap-4">
                    <dt className="text-muted">{term}</dt>
                    <dd className="text-right font-semibold">{value}</dd>
                  </div>
                ))}
              </dl>
              <p className="mt-4 flex items-center gap-2 text-caption text-muted">
                <Tag
                  color={ready ? 'success' : undefined}
                  icon={ready ? <LuCheck aria-hidden /> : undefined}
                >
                  {ready ? '필수 입력 완료' : '필수 입력 필요'}
                </Tag>
                가게 · 업종
              </p>
              <Button
                type="primary"
                block
                className="mt-4"
                icon={<LuArrowRight aria-hidden />}
                iconPlacement="end"
                loading={start.isPending}
                onClick={submit}
              >
                이번 주 브리핑 받기
              </Button>
              <p className="mt-4 text-center text-caption text-muted">
                브리핑은 약 1분이면 준비돼요
              </p>
            </section>
          </Card>

          <Card>
            <ul className="flex flex-col gap-4">
              {BENEFITS.map(({ icon: Icon, title, body }) => (
                <li key={title} className="flex items-start gap-4">
                  <span className="flex size-10 shrink-0 items-center justify-center rounded-full border border-divider">
                    <Icon aria-hidden className="size-5" />
                  </span>
                  <span>
                    <span className="block text-body-strong">{title}</span>
                    <span className="block text-caption text-muted">
                      {body}
                    </span>
                  </span>
                </li>
              ))}
            </ul>
          </Card>
        </div>
      </div>
      {submitted && !ready && (
        <Alert
          className="mt-4"
          type="error"
          showIcon
          title="가게 이름과 위치, 업종을 입력해 주세요."
        />
      )}
      {start.isError && (
        <Alert
          className="mt-4"
          type="error"
          showIcon
          title={
            isAxiosError(start.error) && start.error.response?.status === 422
              ? '서울 안에 있는 가게 주소를 골라 주세요.'
              : '분석을 시작하지 못했어요. 입력 내용을 확인하고 다시 시도해 주세요.'
          }
        />
      )}
      <div className="sticky bottom-0 -mx-4 mt-6 border-t border-divider bg-surface p-4 lg:hidden">
        <Button
          type="primary"
          size="large"
          block
          icon={<LuArrowRight aria-hidden />}
          iconPlacement="end"
          loading={start.isPending}
          onClick={submit}
        >
          이번 주 브리핑 받기
        </Button>
      </div>
      <LocationSearchModal
        open={locationOpen}
        location={location}
        onClose={() => setLocationOpen(false)}
        onSelect={(next) => {
          setLocation(next)
          setLocationOpen(false)
        }}
      />
    </AppLayout>
  )
}
