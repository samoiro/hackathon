import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Card, Spin } from 'antd'
import { LuMapPin, LuRefreshCw, LuStore } from 'react-icons/lu'
import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import AreaStatus from '@/components/dashboard/AreaStatus'
import DashboardHero from '@/components/dashboard/DashboardHero'
import EventsSection from '@/components/dashboard/EventsSection'
import EvidenceSection from '@/components/dashboard/EvidenceSection'
import PriceSection from '@/components/dashboard/PriceSection'
import SupportPrograms from '@/components/dashboard/SupportPrograms'
import TodayChecklist from '@/components/dashboard/TodayChecklist'
import TodayChecklistBar from '@/components/dashboard/TodayChecklistBar'
import SectionTabs from '@/components/dashboard/SectionTabs'
import TodoProvider from '@/components/dashboard/TodoProvider'
import AppLayout from '@/components/layout/AppLayout'
import EmptyState from '@/components/common/EmptyState'
import ResponsiveDialog from '@/components/common/ResponsiveDialog'
import {
  readExcludedCards,
  useAnalysis,
  useStartAnalysis,
} from '@/lib/api/analysis'
import type { AnalysisRequest, SuggestionResponse } from '@/lib/api/generated'
import { searchAddresses } from '@/lib/kakao/maps'
import { loadOnboardingDraft } from '@/lib/onboarding/draft'
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
  const [resetOpen, setResetOpen] = useState(false)
  const [areaOpen, setAreaOpen] = useState(false)
  const analysis = query.data
  const briefing = analysis?.briefing
  const store = analysis?.store
  const original = client.getQueryData<AnalysisRequest>(['analysis-input', id])
  const road = store?.roadAddress ?? ''
  // 가게 좌표: 분석 응답에 좌표가 없어서, 가게 정보 입력 때 로컬스토리지에 저장한 좌표를 쓴다.
  // 저장된 주소가 이 분석의 가게 주소와 같을 때만 쓰고, 다르면(다른 가게를 입력한 뒤 예전 브리핑을 연 경우) 카카오 주소 검색으로 찾는다
  // shortcut: 분석 응답(StoreSummary)에 latitude·longitude가 생기면 그 값만 쓰고 이 저장값·주소 검색은 지운다
  const [saved] = useState(() => loadOnboardingDraft()?.location?.address)
  const savedAddress =
    saved && normalizeRoad(saved.road) === normalizeRoad(road)
      ? saved
      : undefined
  const addressLookup = useQuery({
    queryKey: ['search', 'kakao-address', road],
    enabled: Boolean(briefing && road && !original && !savedAddress),
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
    : (savedAddress ??
      addressLookup.data?.find(
        (item) => normalizeRoad(item.road) === normalizeRoad(road),
      ))
  const start = useStartAnalysis()
  const { notification } = App.useApp()
  const excluded = readExcludedCards()

  // 같은 가게 정보로 오늘 기준 분석을 새로 만든다 (품목 수정·갱신 버튼이 함께 쓴다)
  function restart(items: InterestItems) {
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
        onError: () =>
          notification.error({
            title: '새 브리핑을 시작하지 못했어요. 잠시 후 다시 시도해 주세요.',
          }),
      },
    )
  }

  return (
    <AppLayout
      wide
      className="flex flex-col gap-4 lg:gap-6"
      showSettingsCoach
      // 매일 아침 다시 누르는 버튼: 지금 가게 정보·품목으로 오늘 기준 브리핑을 새로 받는다
      headerActions={
        store && (
          <Button
            size="small"
            aria-label="오늘 기준으로 다시 받기"
            title={
              address
                ? '오늘 기준으로 다시 받기'
                : '가게 위치를 확인하는 중이에요'
            }
            icon={<LuRefreshCw aria-hidden />}
            loading={start.isPending}
            disabled={!address}
            className="max-lg:size-11"
            onClick={() =>
              restart({ raw: store.ingredients, sub: store.supplies })
            }
          />
        )
      }
      // 헤더 가게 정보(피그마 BI-PC-03): 구분선 · 가게 아이콘 · 이름 · 위치와 업종, 그 옆에 가게 정보 수정 버튼
      headerContext={
        store && (
          <div className="flex min-w-0 items-center gap-2 lg:gap-5">
            <span
              aria-hidden
              className="h-6 w-px shrink-0 bg-divider max-lg:hidden"
            />
            <p className="flex min-w-0 items-center gap-2">
              <LuStore
                aria-hidden
                className="size-[18px] shrink-0 max-sm:hidden"
              />
              {/* 좁은 화면 + 큰 글자에서는 말줄임. 전체 이름은 마우스를 올리면 보인다 */}
              <span
                title={store.name}
                className="min-w-0 truncate text-body-strong"
              >
                {store.name}
              </span>
              <span className="truncate text-caption text-muted max-lg:hidden">
                {[
                  store.areaLabel,
                  store.industry.name,
                  `품목 ${store.ingredients.length + store.supplies.length}개`,
                ]
                  .filter(Boolean)
                  .join(' · ')}
              </span>
            </p>
            {/* 모바일은 지역 현황을 아래 시트로 연다 (BI-MO-03-2) */}
            <Button
              size="small"
              aria-label="지역 현황"
              aria-haspopup="dialog"
              icon={<LuMapPin aria-hidden />}
              className="shrink-0 lg:hidden"
              onClick={() => setAreaOpen(true)}
            />
            <Button
              size="small"
              aria-haspopup="dialog"
              className="shrink-0"
              onClick={() => setResetOpen(true)}
            >
              <span className="max-lg:hidden">가게 정보 수정</span>
              <span className="lg:hidden">수정</span>
            </Button>
          </div>
        )
      }
    >
      {/* 가게 정보를 바꾸면 지금 브리핑이 맞지 않게 되므로, 처음 화면으로 돌아가기 전에 한 번 묻는다 */}
      <ResponsiveDialog
        open={resetOpen}
        onClose={() => setResetOpen(false)}
        title="가게 정보를 수정할까요?"
        width={440}
        footer={
          <>
            <Button onClick={() => setResetOpen(false)}>아니요</Button>
            <Button type="primary" onClick={() => navigate('/')}>
              처음으로 돌아가기
            </Button>
          </>
        }
      >
        <p className="text-body">
          지금 보고 있는 브리핑 결과는 초기화되고, 가게 정보 입력 화면으로
          돌아가요. 정보를 고친 뒤 브리핑을 다시 받을 수 있어요.
        </p>
      </ResponsiveDialog>
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
              reason: item.reason,
              dueDate: item.dueDate,
            }))}
        >
          <DashboardHero analysis={analysis} briefing={briefing} />
          {/* 두 칸(855:400)은 1280px부터. 그보다 좁으면 오른쪽 칸이 너무 좁아져 한 줄로 쌓는다 */}
          <div className="grid grid-cols-[minmax(0,1fr)] items-start gap-4 lg:gap-6 xl:grid-cols-[minmax(0,855fr)_minmax(0,400fr)]">
            <div className="flex flex-col gap-4 lg:gap-6">
              <SectionTabs />
              <EventsSection
                id={id}
                briefing={briefing}
                eventSearch={analysis.eventSearch}
              />
              <PriceSection
                analysis={analysis}
                briefing={briefing}
                onSave={restart}
                saving={start.isPending || addressLookup.isFetching}
                saveFailed={
                  start.isError || (!address && !addressLookup.isFetching)
                }
              />
            </div>
            <div className="flex flex-col gap-4 lg:gap-6">
              <div className="max-lg:hidden">
                <TodayChecklist analysisId={id} />
              </div>
              <div className="max-lg:hidden">
                <AreaStatus
                  label={store?.areaLabel || store?.gu || ''}
                  address={address}
                  events={briefing.events}
                  weather={briefing.weather}
                />
              </div>
              <SupportPrograms />
            </div>
          </div>
          <EvidenceSection sources={briefing.sources} />
          <TodayChecklistBar analysisId={id} />
          <ResponsiveDialog
            open={areaOpen}
            onClose={() => setAreaOpen(false)}
            title="지금 이 지역은 어떤가요?"
          >
            <AreaStatus
              sheet
              label={store?.areaLabel || store?.gu || ''}
              address={address}
              events={briefing.events}
              weather={briefing.weather}
            />
          </ResponsiveDialog>
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
