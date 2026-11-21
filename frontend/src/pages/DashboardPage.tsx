import { LuPencil, LuStore } from 'react-icons/lu'
import { Button } from 'antd'
import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import ResponsiveDialog from '@/components/common/ResponsiveDialog'
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
  const navigate = useNavigate()
  const [resetOpen, setResetOpen] = useState(false)

  return (
    <AppLayout
      title="오늘의 1분 경영회의"
      wide
      className="flex flex-col gap-4 lg:gap-6"
      showSettingsCoach
      // 헤더 가게 정보(피그마 BI-PC-03): 구분선 · 가게 아이콘 · 이름 · 위치와 업종, 그 옆에 수정 아이콘 버튼
      headerContext={
        <div className="flex min-w-0 items-center gap-2 lg:gap-5">
          <span
            aria-hidden
            className="h-6 w-px shrink-0 bg-divider max-lg:hidden"
          />
          <p className="flex min-w-0 items-center gap-2">
            <LuStore aria-hidden className="size-[18px] shrink-0" />
            {/* 좁은 화면 + 큰 글자에서는 말줄임. 전체 이름은 마우스를 올리면 보인다 */}
            <span
              title="성수동 한식당"
              className="min-w-0 truncate text-body-strong"
            >
              성수동 한식당
            </span>
            <span className="truncate text-caption text-muted max-lg:hidden">
              성수동 서울숲 인근 · 일반 음식점 · 품목 4개
            </span>
          </p>
          <Button
            size="small"
            aria-label="가게 정보 수정"
            aria-haspopup="dialog"
            title="가게 정보 수정"
            icon={<LuPencil aria-hidden />}
            className="shrink-0"
            onClick={() => setResetOpen(true)}
          />
        </div>
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
      <TodoProvider>
        <DashboardHero empty={empty} />

        {/* 두 칸(855:400)은 1280px부터. 그보다 좁으면 오른쪽 칸이 너무 좁아져 한 줄로 쌓는다 */}
        <div className="grid grid-cols-[minmax(0,1fr)] items-start gap-4 lg:gap-6 xl:grid-cols-[minmax(0,855fr)_minmax(0,400fr)]">
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
            <SupportPrograms empty={empty} />
          </div>
        </div>

        <EvidenceSection />
        <TodayChecklistBar />
      </TodoProvider>
    </AppLayout>
  )
}
