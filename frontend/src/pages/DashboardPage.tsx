import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, Button, Card, Spin } from 'antd'
import { LuPencil, LuStore } from 'react-icons/lu'
import { Link, useNavigate, useSearchParams } from 'react-router'
import AreaStatus from '@/components/dashboard/AreaStatus'
import DashboardHero from '@/components/dashboard/DashboardHero'
import EventsSection from '@/components/dashboard/EventsSection'
import EvidenceSection from '@/components/dashboard/EvidenceSection'
import PriceSection from '@/components/dashboard/PriceSection'
import SupportPrograms from '@/components/dashboard/SupportPrograms'
import TodayChecklist from '@/components/dashboard/TodayChecklist'
import TodayChecklistBar from '@/components/dashboard/TodayChecklistBar'
import TodoProvider from '@/components/dashboard/TodoProvider'
import AppLayout from '@/components/layout/AppLayout'
import EmptyState from '@/components/common/EmptyState'
import {
  readExcludedCards,
  useAnalysis,
  useStartAnalysis,
} from '@/lib/api/analysis'
import type { AnalysisRequest, SuggestionResponse } from '@/lib/api/generated'
import { searchAddresses } from '@/lib/kakao/maps'
import type { InterestItems } from '@/types/search'
import type { TodoTag } from '@/types/todo'

const normalizeRoad = (value: string) => value.replace(/^서울특별시 /, '서울 ')

const todoTags: Record<SuggestionResponse['category'], TodoTag> = {
  EVENT: '행사',
  INGREDIENT: '원자재',
  SUPPLY: '부자재',
}

export default function DashboardPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const client = useQueryClient()
  const id =
    params.get('id') ?? client.getQueryData<string>(['last-briefing']) ?? ''
  const query = useAnalysis(id, true)
  const analysis = query.data
  const briefing = analysis?.briefing
  const store = analysis?.store
  const original = client.getQueryData<AnalysisRequest>(['analysis-input', id])
  const road = store?.roadAddress ?? ''
  const addressLookup = useQuery({
    queryKey: ['search', 'kakao-address', road],
    enabled: Boolean(briefing && road && !original),
    queryFn: () => searchAddresses(road),
    retry: false,
  })
  const address = original
    ? {
        road: original.roadAddress,
        latitude: original.latitude,
        longitude: original.longitude,
        jibun: '',
      }
    : addressLookup.data?.find(
        (item) => normalizeRoad(item.road) === normalizeRoad(road),
      )
  const start = useStartAnalysis()
  const excluded = readExcludedCards()

  function updateItems(items: InterestItems) {
    if (!store || !address || start.isPending) return
    start.mutate(
      {
        storeName: store.name,
        roadAddress: store.roadAddress,
        latitude: address.latitude,
        longitude: address.longitude,
        industryId: store.industry.id,
        operationType: store.operationType ?? undefined,
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

  return (
    <AppLayout
      title="오늘의 1분 경영회의"
      wide
      className="flex flex-col gap-4 lg:gap-6"
      showSettingsCoach
      headerContext={
        store && (
          <Link
            to="/"
            aria-label="가게 정보 수정"
            className="inline-flex min-h-9 items-center gap-2 rounded-xl border border-line-strong px-3 text-caption hover:bg-canvas"
          >
            <LuStore aria-hidden />
            <span className="font-semibold">{store.name}</span>
            <span className="text-muted">{store.industry.name}</span>
            <LuPencil aria-hidden className="text-muted" />
          </Link>
        )
      }
    >
      {analysis && briefing ? (
        <TodoProvider
          key={id}
          initial={briefing.suggestions
            .filter(
              (item) => item.recommended && !excluded.includes(item.cardKey),
            )
            .map((item) => ({
              id: item.cardKey,
              title: item.title,
              tag: todoTags[item.category],
              strong: true,
            }))}
        >
          <DashboardHero analysis={analysis} briefing={briefing} />
          <div className="grid grid-cols-[minmax(0,1fr)] items-start gap-4 lg:grid-cols-[minmax(0,855fr)_minmax(0,400fr)] lg:gap-6">
            <div className="flex flex-col gap-4 lg:gap-6">
              <EventsSection
                id={id}
                briefing={briefing}
                eventSearch={analysis.eventSearch}
              />
              <PriceSection
                analysis={analysis}
                briefing={briefing}
                onSave={updateItems}
                saving={start.isPending || addressLookup.isFetching}
                saveFailed={
                  start.isError || (!address && !addressLookup.isFetching)
                }
              />
            </div>
            <div className="flex flex-col gap-4 lg:gap-6">
              <div className="max-lg:hidden">
                <TodayChecklist />
              </div>
              <AreaStatus
                label={store?.areaLabel || store?.gu || ''}
                address={address}
                events={briefing.events}
                weather={briefing.weather}
              />
              <SupportPrograms />
            </div>
          </div>
          <EvidenceSection sources={briefing.sources} />
          <TodayChecklistBar />
        </TodoProvider>
      ) : (
        <>
          <Card>
            <h1 className="text-display">오늘의 1분 경영회의</h1>
            {id && query.isPending ? (
              <div className="mt-4">
                <Spin /> 브리핑을 불러오고 있어요.
              </div>
            ) : query.isError ? (
              <Alert
                className="mt-4"
                type="error"
                showIcon
                title="브리핑을 불러오지 못했어요."
                action={
                  <Button onClick={() => void query.refetch()}>
                    다시 시도
                  </Button>
                }
              />
            ) : (
              <div className="mt-4">
                <EmptyState
                  title={
                    id
                      ? '아직 완료된 브리핑이 없어요.'
                      : '가게 정보로 브리핑을 시작해 보세요.'
                  }
                  description="분석이 끝나면 행사와 가격, 준비할 일을 보여드려요."
                  actions={
                    <Link
                      className="text-brand-text underline"
                      to={id ? `/briefing?id=${encodeURIComponent(id)}` : '/'}
                    >
                      {id ? '분석 상태 확인' : '가게 정보 입력'}
                    </Link>
                  }
                />
              </div>
            )}
          </Card>
          <SupportPrograms />
        </>
      )}
    </AppLayout>
  )
}
