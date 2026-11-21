import { LuPencil, LuStore } from 'react-icons/lu'
import { Link, useSearchParams } from 'react-router'
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

export default function DashboardPage() {
  // shortcut: API 연결 전이라 ?state=empty 로 행사·품목이 없는 화면을 본다. 기능 구현 때 조회 결과로 바꾼다
  const [searchParams] = useSearchParams()
  const empty = searchParams.get('state') === 'empty'

  return (
    <AppLayout
      title="오늘의 1분 경영회의"
      wide
      className="flex flex-col gap-4 lg:gap-6"
      showSettingsCoach
      headerContext={
        <Link
          to="/"
          aria-label="가게 정보 수정: 성수동 한식당, 일반 음식점"
          className="inline-flex h-9 items-center gap-2 rounded-xl border border-line-strong px-3 text-caption whitespace-nowrap hover:bg-canvas"
        >
          <LuStore aria-hidden />
          <span className="font-semibold">성수동 한식당</span>
          <span className="text-muted">일반 음식점</span>
          <LuPencil aria-hidden className="text-muted" />
        </Link>
      }
    >
      <TodoProvider>
        <DashboardHero empty={empty} />

        <div className="grid grid-cols-[minmax(0,1fr)] items-start gap-4 lg:grid-cols-[minmax(0,855fr)_minmax(0,400fr)] lg:gap-6">
          <div className="flex flex-col gap-4 lg:gap-6">
            <EventsSection empty={empty} />
            <PriceSection empty={empty} />
          </div>
          <div className="flex flex-col gap-4 lg:gap-6">
            {/* 모바일은 화면 아래 고정 줄(TodayChecklistBar)에서 연다 */}
            <div className="max-lg:hidden">
              <TodayChecklist />
            </div>
            <AreaStatus />
            <SupportPrograms />
          </div>
        </div>

        <EvidenceSection />
        <TodayChecklistBar />
      </TodoProvider>
    </AppLayout>
  )
}
