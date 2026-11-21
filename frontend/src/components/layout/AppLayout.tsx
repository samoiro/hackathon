import { useEffect, useRef, type ReactNode } from 'react'
import { LuClock3, LuDatabase, LuType } from 'react-icons/lu'
import { useLocation, useNavigationType } from 'react-router'
import AppHeader from '@/components/layout/AppHeader'

// 탭 제목은 index.html의 서비스 이름으로 고정한다. 앱 안에서 이동했을 때는 본문으로 포커스를 옮긴다 (첫 진입·뒤로 가기는 그대로)
export default function AppLayout({
  headerContext,
  showSettingsCoach,
  wide = false,
  width,
  className = '',
  children,
}: {
  headerContext?: ReactNode
  showSettingsCoach?: boolean
  // 대시보드처럼 정보가 많은 화면은 헤더·본문·푸터를 1280(좌우 80 포함 1440)으로 넓힌다
  wide?: boolean
  // 본문만 더 좁게 쓸 때 (브리핑 카드 등)
  width?: string
  className?: string
  children: ReactNode
}) {
  const chromeWidth = wide ? 'max-w-[1440px]' : 'max-w-[1280px]'
  const navigationType = useNavigationType()
  const { pathname } = useLocation()
  const mainRef = useRef<HTMLElement>(null)

  useEffect(() => {
    if (navigationType !== 'POP') mainRef.current?.focus()
  }, [pathname, navigationType])

  return (
    <>
      {/* 본문 바로가기(WCAG 2.4.1): 평소엔 숨어 있고 Tab으로 처음 들어올 때만 왼쪽 위에 보인다 */}
      <a
        href="#main"
        className="sr-only z-50 rounded-xl bg-surface px-4 py-3 font-semibold focus:not-sr-only focus:fixed focus:top-3 focus:left-3"
      >
        본문 바로가기
      </a>
      <AppHeader
        context={headerContext}
        showSettingsCoach={showSettingsCoach}
        width={chromeWidth}
      />
      {/* tabIndex -1: 화면 이동 뒤 프로그램으로만 포커스를 받는다. 테두리는 키보드 포커스가 아니라 숨긴다 */}
      <main
        id="main"
        ref={mainRef}
        tabIndex={-1}
        className={`mx-auto ${width ?? chromeWidth} px-4 pt-4 pb-16 focus:outline-none lg:px-20 lg:pt-8 ${className}`}
      >
        {children}
      </main>
      <footer className="border-t border-divider bg-surface">
        <div
          className={`mx-auto ${chromeWidth} px-4 py-6 text-caption text-muted lg:px-20`}
        >
          <div className="flex flex-wrap justify-between gap-x-6 gap-y-1">
            <p>
              <span className="font-semibold text-fg">
                사모이로 · 사장님의 1분 경영회의
              </span>
              <span className="block">
                사모이로 팀 · Samoiro AI Hackathon 2026
              </span>
            </p>
            <p className="flex items-center gap-1.5 self-start">
              <LuClock3 aria-hidden />
              매일 오전 7시 브리핑
            </p>
          </div>
          {/* 전기안전체·서울시 데이터는 공공누리 1유형이라 출처 표시가 이용 조건이다 */}
          <dl className="mt-4 grid grid-cols-[auto_minmax(0,1fr)] gap-x-3 gap-y-1 border-t border-divider pt-4">
            <dt className="flex items-center gap-1.5 font-semibold">
              <LuDatabase aria-hidden />
              데이터
            </dt>
            <dd>
              서울 열린데이터광장(실시간 도시데이터, 문화행사 정보) ·
              한국농수산식품유통공사(농수산물 가격) · 중소벤처기업부(지원사업
              공고)
            </dd>
            <dt className="flex items-center gap-1.5 font-semibold">
              <LuType aria-hidden />
              서체
            </dt>
            <dd>
              전기안전체 ⓒ 한국전기안전공사, 공공누리 제1유형 · Pretendard, SIL
              Open Font License 1.1
            </dd>
          </dl>
          <p className="mt-4">© 2026 사모이로 팀</p>
        </div>
      </footer>
    </>
  )
}
