import { LuCheck } from 'react-icons/lu'

const STEPS = ['가게 정보', '분석', '브리핑']

// 입력 → 분석 → 브리핑 진행 단계. 지금 단계는 aria-current="step", 지난 단계는 '완료'를 글자로도 남긴다
export default function StepIndicator({ current }: { current: number }) {
  return (
    <ol aria-label="진행 단계" className="flex items-center gap-3">
      {STEPS.map((label, index) => {
        const step = index + 1
        const done = step < current
        const active = step === current
        return (
          <li key={label} className="flex items-center gap-3">
            {index > 0 && (
              <span aria-hidden className="h-px w-8 bg-line-strong" />
            )}
            <span
              aria-current={active ? 'step' : undefined}
              className="flex items-center gap-2 text-caption"
            >
              <span
                className={`flex size-6 items-center justify-center rounded-full text-tag ${
                  done
                    ? 'bg-success-bg text-success'
                    : active
                      ? 'bg-primary-500 font-semibold text-on-primary'
                      : 'border border-border text-muted'
                }`}
              >
                {done ? <LuCheck aria-hidden /> : step}
              </span>
              <span className={active ? 'font-semibold' : 'text-muted'}>
                {label}
                {done && <span className="sr-only"> 완료</span>}
              </span>
            </span>
          </li>
        )
      })}
    </ol>
  )
}
