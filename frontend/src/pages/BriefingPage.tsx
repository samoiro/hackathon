import { Alert, Button, Card, Tag } from 'antd'
import { LuCheck, LuX } from 'react-icons/lu'
import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useAnalysis } from '@/lib/api/analysis'
import type { AnalysisStep } from '@/lib/api/generated'
import {
  Navigate,
  useNavigate,
  useNavigationType,
  useSearchParams,
} from 'react-router'
import AppLayout from '@/components/layout/AppLayout'
import StepIndicator from '@/components/layout/StepIndicator'
import MascotFace from '@/components/common/MascotFace'
import WaveProgress from '@/components/common/WaveProgress'

type StepState = 'done' | 'active' | 'failed' | 'waiting'

const STEPS: { code: AnalysisStep['code']; title: string }[] = [
  { code: 'STORE_CHECK', title: '가게 정보 확인' },
  { code: 'EVENT_SEARCH', title: '주변 행사 조회' },
  { code: 'PRICE_LOOKUP', title: '품목 가격 조회' },
  { code: 'TASK_GENERATION', title: '준비할 일 정리' },
]
const statusToState: Record<AnalysisStep['status'], StepState> = {
  DONE: 'done',
  RUNNING: 'active',
  FAILED: 'failed',
  WAITING: 'waiting',
}

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

export default function BriefingPage() {
  const navigate = useNavigate()
  const navigationType = useNavigationType()
  const [params] = useSearchParams()
  const id = params.get('id') ?? ''
  const reloaded =
    navigationType === 'POP' &&
    performance
      .getEntriesByType('navigation')
      .some(
        (entry) =>
          'type' in entry &&
          entry.type === 'reload' &&
          entry.name.split('#')[0] === window.location.href.split('#')[0],
      )
  const query = useAnalysis(reloaded ? '' : id, true)
  const analysis = query.data
  const client = useQueryClient()
  const failed =
    query.isError ||
    analysis?.status === 'FAILED' ||
    (analysis?.status === 'SUCCEEDED' && !analysis.briefing)
  const doneCount =
    analysis?.steps.filter(({ status }) => status === 'DONE').length ?? 0
  const store = analysis?.store
  const chips = store
    ? [
        [store.areaLabel, store.industry.name].filter(Boolean).join(' · '),
        store.ingredients.map(({ name }) => name).join(' · '),
        store.supplies.map(({ name }) => name).join(' · '),
      ].filter(Boolean)
    : []

  useEffect(() => {
    if (!failed && analysis?.status === 'SUCCEEDED' && analysis.briefing) {
      client.setQueryData(['last-briefing'], id)
      navigate(`/dashboard?id=${encodeURIComponent(id)}`, { replace: true })
    }
  }, [analysis, client, failed, id, navigate])

  if (reloaded || !id) return <Navigate to="/" replace />
  // 실패해도 바로 보내지 않고 실패 화면(우는 캐릭터·에러색 테두리)을 보여준다. 입력 화면으로 돌아가면 입력값이 그대로 채워진다
  const failureMessage =
    analysis?.failure?.message ||
    '분석을 완료하지 못했어요. 입력 내용을 확인하고 다시 시도해 주세요.'

  return (
    <AppLayout
      // 진행 단계는 로고 폭과 상관없이 헤더 정중앙에 둔다
      headerContext={
        <div className="absolute top-1/2 left-1/2 -translate-1/2">
          <StepIndicator current={2} />
        </div>
      }
    >
      <Card
        className={`mx-auto w-full max-w-[720px] ${failed ? 'border-danger' : ''}`}
      >
        <div className="flex flex-col items-center pt-4 text-center lg:px-4">
          <MascotFace crying={failed} className="size-16 lg:size-24" />
          <h1 aria-live="polite" className="mt-6 text-title">
            {failed
              ? '브리핑을 만들다가 멈췄어요'
              : '이번 주의 기회와 주의할 신호를 찾고 있어요'}
          </h1>
          <ul className="mt-4 flex flex-wrap justify-center gap-2">
            {chips.map((chip) => (
              <li
                key={chip}
                className="rounded-full border border-divider px-3 py-1 text-caption"
              >
                {chip}
              </li>
            ))}
          </ul>
        </div>

        <div className="mt-6 lg:mt-8 lg:px-4">
          <div className="flex flex-wrap justify-between gap-x-3 text-caption">
            <span className="font-semibold">
              {doneCount} / {STEPS.length} 단계
              {failed ? '에서 멈췄어요' : ' 완료'}
            </span>
            <span className="text-muted">
              {failed ? '입력한 정보는 그대로예요' : '곧 끝나요'}
            </span>
          </div>
          <WaveProgress
            percent={(doneCount / STEPS.length) * 100}
            showInfo={false}
            size={{ height: 16 }}
            strokeColor={
              failed ? 'var(--color-muted)' : 'var(--color-primary-500)'
            }
            aria-label="브리핑 진행률"
          />

          <ol className="mt-4 flex flex-col gap-1">
            {STEPS.map((step, index) => {
              const serverStep = analysis?.steps.find(
                ({ code }) => code === step.code,
              )
              const state = serverStep
                ? statusToState[serverStep.status]
                : 'waiting'
              const detail =
                state === 'failed'
                  ? analysis?.failure?.message
                  : step.code === 'STORE_CHECK' && store
                    ? `${store.gu} · ${store.industry.name}`
                    : step.code === 'EVENT_SEARCH' && analysis?.eventSearch
                      ? `향후 ${analysis.eventSearch.days}일 · ${analysis.eventSearch.count}건`
                      : undefined
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
                    {detail && state !== 'waiting' && (
                      <p className="text-caption text-muted">{detail}</p>
                    )}
                  </div>
                  {STATE_TAG[state]}
                </li>
              )
            })}
          </ol>

          {failed ? (
            <div className="mt-6 mb-4 flex flex-col gap-4">
              <Alert type="error" showIcon title={failureMessage} />
              <Button type="primary" block onClick={() => navigate('/')}>
                입력 화면으로 돌아가기
              </Button>
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
