import { Alert, Button, Card, Tag } from 'antd'
import { LuCheck, LuRotateCw, LuX } from 'react-icons/lu'
import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import AppLayout from '@/components/layout/AppLayout'
import StepIndicator from '@/components/layout/StepIndicator'
import WaveProgress from '@/components/common/WaveProgress'

type StepState = 'done' | 'active' | 'failed' | 'waiting'

const STEPS = [
  { title: '가게 정보 확인', detail: '서울 성동구 · 일반 음식점' },
  { title: '주변 행사 조회', detail: '1km 안 · 향후 7일 · 2건 확인' },
  {
    title: '품목 가격 조회',
    detail: '한국농수산식품유통공사 가격 추이 확인 중',
    failedDetail: '가격 자료를 불러오지 못했어요',
  },
  { title: '준비할 일 정리' },
]

const STATE_TAG = {
  done: (
    <Tag color="success" icon={<LuCheck aria-hidden />}>
      완료
    </Tag>
  ),
  active: <Tag color="processing">확인 중</Tag>,
  failed: (
    <Tag color="error" icon={<LuX aria-hidden />}>
      실패
    </Tag>
  ),
  waiting: <Tag>대기</Tag>,
}

const STATE_ROW = {
  done: '',
  active: 'bg-primary-50',
  failed: 'bg-danger-bg',
  waiting: '',
}

function StepMarker({ state, index }: { state: StepState; index: number }) {
  if (state === 'done')
    return (
      <span className="flex size-8 items-center justify-center rounded-full bg-aurora-500 text-fg">
        <LuCheck aria-hidden />
      </span>
    )
  if (state === 'failed')
    return (
      <span className="flex size-8 items-center justify-center rounded-full bg-danger text-surface">
        <LuX aria-hidden />
      </span>
    )
  return (
    <span
      className={`flex size-8 items-center justify-center rounded-full border font-semibold ${
        state === 'active'
          ? 'border-fg bg-fg text-surface'
          : 'border-border text-muted'
      }`}
    >
      {index + 1}
    </span>
  )
}

// 단계 하나에 0.7초, 4단계를 3초 안에 끝내고 대시보드로 넘어간다
const STEP_MS = 700
// 실패 화면에서 멈춘 단계(품목 가격 조회)
const FAILED_AT = 2

// shortcut: API 연결 전이라 단계를 시간으로 넘기고, ?status=error 로 실패 화면을 본다. 기능 구현 때 브리핑 생성 API 진행 상태로 바꾼다
export default function BriefingPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const failed = searchParams.get('status') === 'error'
  const [doneCount, setDoneCount] = useState(0)

  useEffect(() => {
    if (failed) return
    if (doneCount >= STEPS.length) {
      navigate('/dashboard', { replace: true })
      return
    }
    const timer = setTimeout(() => setDoneCount((count) => count + 1), STEP_MS)
    return () => clearTimeout(timer)
  }, [doneCount, failed, navigate])

  const stepState = (index: number): StepState => {
    if (failed) {
      if (index < FAILED_AT) return 'done'
      return index === FAILED_AT ? 'failed' : 'waiting'
    }
    if (index < doneCount) return 'done'
    return index === doneCount ? 'active' : 'waiting'
  }

  return (
    <AppLayout
      title={failed ? '브리핑 실패' : '브리핑 준비 중'}
      headerContext={
        <div className="mx-auto">
          <StepIndicator current={2} />
        </div>
      }
    >
      <Card className="mx-auto w-full max-w-[720px]">
        <div className="flex flex-col items-center pt-4 text-center lg:px-4">
          <img
            src="/images/mascot-face.png"
            alt=""
            width={96}
            height={96}
            className="size-16 rounded-full bg-primary-100 object-contain p-1.5 lg:size-24 lg:p-2"
          />
          <h1 aria-live="polite" className="mt-6 text-title">
            {failed
              ? '브리핑을 만들다가 멈췄어요'
              : '이번 주의 기회와 주의할 신호를 찾고 있어요'}
          </h1>
          <ul className="mt-4 flex flex-wrap justify-center gap-2">
            {['성수동 · 일반 음식점', '쌀 · 돼지고기 · 배추', '포장 용기'].map(
              (chip) => (
                <li
                  key={chip}
                  className="rounded-full border border-divider px-3 py-1 text-caption"
                >
                  {chip}
                </li>
              ),
            )}
          </ul>
        </div>

        <div className="mt-6 lg:mt-8 lg:px-4">
          <div className="flex flex-wrap justify-between gap-x-3 text-caption">
            <span className="font-semibold">
              {failed
                ? `${FAILED_AT} / ${STEPS.length} 단계에서 멈췄어요`
                : `${doneCount} / ${STEPS.length} 단계 완료`}
            </span>
            <span className="text-muted">
              {failed ? '입력한 정보는 그대로예요' : '곧 끝나요'}
            </span>
          </div>
          <WaveProgress
            percent={((failed ? FAILED_AT : doneCount) / STEPS.length) * 100}
            showInfo={false}
            strokeColor={
              failed ? 'var(--color-muted)' : 'var(--color-primary-500)'
            }
            aria-label="브리핑 진행률"
          />

          <ol className="mt-4 flex flex-col gap-1">
            {STEPS.map((step, index) => {
              const state = stepState(index)
              return (
                <li
                  key={step.title}
                  className={`flex items-center gap-4 rounded-xl p-4 ${STATE_ROW[state]}`}
                >
                  <StepMarker state={state} index={index} />
                  <div className="flex-1">
                    <p
                      className={
                        state === 'waiting' ? 'text-muted' : 'text-body-strong'
                      }
                    >
                      {step.title}
                    </p>
                    {step.detail && state !== 'waiting' && (
                      <p className="text-caption text-muted">
                        {state === 'failed' ? step.failedDetail : step.detail}
                      </p>
                    )}
                  </div>
                  {STATE_TAG[state]}
                </li>
              )
            })}
          </ol>

          {failed ? (
            <div className="mt-6 mb-4 flex flex-col gap-4">
              <Alert
                type="error"
                showIcon
                title={
                  <span className="text-caption text-danger">
                    지금은 브리핑을 만들 수 없어요. 잠시 후 다시 시도해 주세요.
                    입력한 정보는 그대로 남아 있어요.
                  </span>
                }
              />
              <div className="grid gap-3 lg:grid-cols-2">
                <Button
                  type="primary"
                  icon={<LuRotateCw aria-hidden />}
                  onClick={() => {
                    setDoneCount(0)
                    setSearchParams({})
                  }}
                >
                  다시 시도
                </Button>
                <Button onClick={() => navigate('/')}>
                  입력 화면으로 돌아가기
                </Button>
              </div>
            </div>
          ) : (
            <p className="mt-6 mb-4 text-center text-caption text-muted">
              출처와 기준 시각은 브리핑에서 함께 보여드려요.
            </p>
          )}
        </div>
      </Card>
    </AppLayout>
  )
}
