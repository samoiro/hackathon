import type { ReactNode } from 'react'
import { Link } from 'react-router'
import DisplaySettingsPopover from '@/components/layout/DisplaySettingsPopover'

// 헤더 v3: 로고 · 상황 영역(화면마다 하나) · 글자·화면 설정. PC 높이 72·좌우 80, 모바일은 한 줄 56·좌우 16에 로고 글자를 빼고 심볼만 둔다
export default function AppHeader({
  context,
  showSettingsCoach = false,
  width = 'max-w-[1280px]',
}: {
  context?: ReactNode
  showSettingsCoach?: boolean
  width?: string
}) {
  return (
    <header className="border-b border-divider bg-surface">
      <div
        className={`mx-auto grid h-14 ${width} grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-3 px-4 lg:h-18 lg:gap-10 lg:px-20`}
      >
        <Link
          to="/dashboard"
          aria-label="사모이로 홈"
          className="flex items-center gap-3 whitespace-nowrap"
        >
          <img src="/images/logo.svg" alt="" width={32} height={32} />
          <span className="max-lg:hidden">
            <span className="block font-brand text-title">사모이로</span>
            <span className="block font-brand text-tag font-normal text-muted">
              사장님의 1분 경영회의
            </span>
          </span>
        </Link>
        <div className="flex min-w-0">{context}</div>
        <DisplaySettingsPopover showCoach={showSettingsCoach} />
      </div>
    </header>
  )
}
