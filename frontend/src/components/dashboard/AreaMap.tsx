import { LuCalendar, LuStore } from 'react-icons/lu'

const PINS = [
  { label: '사진전 870m', className: 'top-[18%] right-[8%]' },
  { label: '음악회 650m', className: 'bottom-[18%] left-[8%]' },
]

// shortcut: 지도 API 연결 전이라 도로·공원·반경을 그린 정적 그림이다. 연동 때 지도 SDK로 바꾸고 출처 표기를 실제 값으로 넣는다
export default function AreaMap() {
  return (
    <div
      role="img"
      aria-label="가게 주변 지도. 반경 1km 안에 음악회 650m, 사진전 870m"
      className="relative mt-3 h-40 overflow-hidden rounded-xl bg-divider/70"
    >
      <svg
        aria-hidden
        className="absolute inset-0 size-full"
        viewBox="0 0 300 160"
        preserveAspectRatio="xMidYMid slice"
      >
        <rect
          x="0"
          y="95"
          width="120"
          height="65"
          rx="12"
          fill="var(--color-success-bg)"
        />
        <path
          d="M0 70 L300 40 M150 0 L185 160 M230 0 L255 160"
          stroke="var(--color-surface)"
          strokeWidth="10"
          fill="none"
        />
        <circle
          cx="160"
          cy="80"
          r="58"
          fill="none"
          stroke="var(--color-fg)"
          strokeWidth="1.2"
          strokeDasharray="5 5"
        />
      </svg>
      <span className="absolute top-1/2 left-1/2 flex -translate-1/2 items-center gap-1 rounded-full bg-hero px-2 py-1 text-tag text-on-hero">
        <LuStore aria-hidden />
        우리 가게
      </span>
      {PINS.map((pin) => (
        <span
          key={pin.label}
          className={`absolute flex items-center gap-1 rounded-full border border-fg bg-surface px-2 py-1 text-tag ${pin.className}`}
        >
          <LuCalendar aria-hidden />
          {pin.label}
        </span>
      ))}
      <span className="absolute right-2 bottom-1 text-tag text-muted">
        지도 API 출처 표기
      </span>
    </div>
  )
}
