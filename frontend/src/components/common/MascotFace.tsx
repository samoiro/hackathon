// 사모예드 얼굴(브랜드가이드 06). crying이면 웃는 입을 털색으로 덮고 처진 눈썹·시무룩한 입·눈물을 그 위에 그린다.
// 좌표는 mascot-face.png(320×304) 기준: 눈 (115,147)·(205,147), 코 (160,166), 입 115~205 × 182~246
// shortcut: 우는 얼굴 원본 그림이 없어 덧그린다. 디자이너가 그림을 주면 이미지만 바꾼다
export default function MascotFace({
  crying = false,
  className = '',
}: {
  crying?: boolean
  className?: string
}) {
  return (
    <span
      aria-hidden
      className={`flex items-center justify-center rounded-full bg-primary-100 ${className}`}
    >
      <span className="relative block w-[84%]">
        <img
          src="/images/mascot-face.png"
          alt=""
          width={320}
          height={304}
          className="block w-full"
        />
        {crying && (
          <svg
            viewBox="0 0 320 304"
            className="absolute inset-0 size-full"
            fill="none"
            strokeLinecap="round"
          >
            {/* 웃는 입 가리기 */}
            <ellipse cx="160" cy="216" rx="54" ry="33" fill="#f9f9f9" />
            <path d="M160 181v12" stroke="#2b1a12" strokeWidth="6" />
            {/* 시무룩한 입 */}
            <path d="M132 218q28-26 56 0" stroke="#2b1a12" strokeWidth="7" />
            {/* 처진 눈썹 */}
            <path
              d="M98 124l26-9M222 124l-26-9"
              stroke="#2b1a12"
              strokeWidth="6"
            />
            {/* 눈물 */}
            <path
              className="mascot-tear"
              d="M112 166c-6 9-9 14-9 19a9 9 0 0 0 18 0c0-5-3-10-9-19z"
              fill="#7abdff"
            />
            <path
              className="mascot-tear mascot-tear-late"
              d="M208 166c-6 9-9 14-9 19a9 9 0 0 0 18 0c0-5-3-10-9-19z"
              fill="#7abdff"
            />
          </svg>
        )}
      </span>
    </span>
  )
}
