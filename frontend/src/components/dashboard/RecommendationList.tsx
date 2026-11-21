import { App, Button, Tag } from 'antd'
import DueBadge from '@/components/dashboard/DueBadge'
import { useIsMobile } from '@/hooks/useIsMobile'
import type { SuggestionResponse } from '@/lib/api/generated'
import {
  readExcludedCards,
  saveExcludedCard,
  useSuggestionFeedback,
} from '@/lib/api/analysis'
import { useEffect, useRef, useState } from 'react'
import { LuBan, LuCheck, LuPlus, LuThumbsUp } from 'react-icons/lu'
import { TODO_LIMIT, useTodos } from '@/lib/dashboard/todos'
import type { TodoTag } from '@/types/todo'

type Feedback = 'helpful' | 'dismissed' | null

const toTodo = (item: SuggestionResponse, tag: TodoTag) => ({
  id: item.cardKey,
  title: item.title,
  tag,
  strong: item.recommended,
  reason: item.reason,
  dueDate: item.dueDate,
})

// 추천 할 일 카드(피드백 반영안): 제목 · 기대 효과 · 준비 기한, 아래 줄에 피드백과 추가.
// 피드백은 PC에서 글자 버튼, 모바일에서 44px 아이콘 버튼(이름은 그대로 '도움돼요'·'해당 없어요')
// 추가를 누르면 '오늘 확인할 일'에 들어가고, 다시 누르면 빠진다. 강추는 처음부터 담겨 있고 뺄 수 없다
function RecommendationItem({
  item,
  analysisId,
  tag,
}: {
  item: SuggestionResponse
  analysisId: string
  tag: TodoTag
  // 묶음 머리의 '전체 추가' 버튼 (행사별 묶음에는 없다)
  bulkAdd?: boolean
}) {
  const { todos, add, remove } = useTodos()
  const mobile = useIsMobile()
  const added = todos.some((todo) => todo.id === item.cardKey)
  const full = !added && todos.length >= TODO_LIMIT
  const [voteState, setFeedback] = useState<Feedback>(() =>
    readExcludedCards().includes(item.cardKey) ? 'dismissed' : null,
  )
  const undoRef = useRef<HTMLButtonElement>(null)
  const dismissRef = useRef<HTMLButtonElement>(null)
  const returnFocus = useRef(false)
  const { notification } = App.useApp()
  const vote = useSuggestionFeedback(analysisId, item.cardKey, {
    onSuccess: (value) => {
      const dismissed = value === 'NOT_RELEVANT'
      returnFocus.current = dismissed
      setFeedback(dismissed ? 'dismissed' : 'helpful')
      if (dismissed) remove(item.cardKey)
      notification.open({
        role: 'status',
        icon: <LuCheck aria-hidden className="text-primary-300" />,
        title: (
          <span className="text-body-strong">
            {dismissed ? '알려 주셔서 고마워요' : '의견 고마워요!'}
          </span>
        ),
        description: (
          <span className="text-caption">
            사장님 의견을 반영해 다음 추천을 더 잘 맞춰 드릴게요.
          </span>
        ),
        duration: 5,
        closable: false,
      })
    },
    onError: () =>
      notification.error({
        title: '의견을 저장하지 못했어요. 다시 시도해 주세요.',
      }),
  })

  // 누른 버튼이 사라지므로 포커스를 이어지는 버튼으로 옮긴다 (해당 없어요 ↔ 되돌리기)
  useEffect(() => {
    if (!returnFocus.current) return
    returnFocus.current = false
    if (voteState === 'dismissed') undoRef.current?.focus()
    else dismissRef.current?.focus()
  }, [voteState])

  function restore() {
    returnFocus.current = true
    saveExcludedCard(item.cardKey, false)
    setFeedback(null)
  }

  if (voteState === 'dismissed') {
    return (
      <li className="rounded-xl border border-divider bg-surface p-4">
        <p className="text-body-strong text-muted">{item.title}</p>
        <p className="mt-2 text-caption text-muted">
          해당 없어요로 표시했어요.
          <br />
          다음 브리핑부터 이 할 일은 빼 드릴게요.
        </p>
        <Button
          ref={undoRef}
          type="link"
          className="mt-3 h-auto p-0 font-semibold underline"
          onClick={restore}
        >
          되돌리기
        </Button>
      </li>
    )
  }

  const feedbackButton = mobile ? 'size-11 shrink-0' : 'text-muted'

  return (
    <li className="rounded-xl border border-divider bg-surface p-4">
      <p className="flex flex-wrap items-center gap-2 text-body-strong">
        {item.recommended && (
          <Tag color="#241a0d" variant="solid">
            강추
          </Tag>
        )}
        {item.title}
      </p>
      <p className="mt-1 text-caption text-muted">
        기대 효과 · {item.expectedEffect ?? item.reason}
      </p>
      <p className="mt-2">
        <DueBadge dueDate={item.dueDate} />
      </p>
      <div className="mt-3 flex flex-wrap items-center gap-2">
        <div
          role="group"
          aria-label="이 추천이 도움이 되었나요"
          className="flex flex-wrap items-center gap-2"
        >
          <Button
            size="small"
            type={mobile ? 'default' : 'text'}
            aria-label={mobile ? '도움돼요' : undefined}
            aria-pressed={voteState === 'helpful'}
            icon={<LuThumbsUp aria-hidden />}
            loading={vote.isPending && vote.variables === 'HELPFUL'}
            disabled={vote.isPending}
            className={`${feedbackButton} aria-pressed:bg-hero aria-pressed:text-on-hero`}
            onClick={() => vote.mutate('HELPFUL')}
          >
            {!mobile && '도움돼요'}
          </Button>
          <Button
            ref={dismissRef}
            size="small"
            type={mobile ? 'default' : 'text'}
            aria-label={mobile ? '해당 없어요' : undefined}
            icon={<LuBan aria-hidden />}
            loading={vote.isPending && vote.variables === 'NOT_RELEVANT'}
            disabled={vote.isPending}
            className={feedbackButton}
            onClick={() => vote.mutate('NOT_RELEVANT')}
          >
            {!mobile && '해당 없어요'}
          </Button>
        </div>
        <Button
          size={mobile ? 'middle' : 'small'}
          aria-pressed={added}
          // 12개가 차면 더 담을 수 없다. 강추는 오늘 확인할 일에서 뺄 수 없어 추가됨으로 고정한다
          disabled={full || (added && item.recommended)}
          title={
            full ? `할 일은 ${TODO_LIMIT}개까지 담을 수 있어요` : undefined
          }
          icon={added ? <LuCheck aria-hidden /> : <LuPlus aria-hidden />}
          className="ml-auto min-w-20 aria-pressed:border-success-bg aria-pressed:bg-success-bg aria-pressed:text-success"
          onClick={() =>
            added ? remove(item.cardKey) : add(toTodo(item, tag))
          }
        >
          {added ? '추가됨' : '추가'}
        </Button>
      </div>
    </li>
  )
}

// 피드백 버튼 범례. 모바일은 아이콘만 보이므로 여기서 뜻을 알려준다 (리뷰 F-07: 화면에 한 번만)
export function FeedbackLegend() {
  return (
    <p className="mt-3 text-caption text-muted">
      <span className="mr-2 inline-flex items-center gap-1 font-semibold text-fg">
        <LuThumbsUp aria-hidden /> 도움돼요
      </span>
      <span className="mr-2 inline-flex items-center gap-1 font-semibold text-fg">
        <LuBan aria-hidden /> 해당 없어요
      </span>
      알려 주시면 사장님 의견을 반영해 다음 추천을 더 잘 맞춰 드려요.
    </p>
  )
}

export default function RecommendationList({
  title,
  description,
  items,
  analysisId,
  tag,
  bulkAdd = true,
  showFeedbackNote = false,
}: {
  title: string
  description: string
  items: SuggestionResponse[]
  analysisId: string
  // 오늘 확인할 일에 붙는 분류
  tag: TodoTag
  // 묶음 머리의 '전체 추가' 버튼 (행사별 묶음에는 없다)
  bulkAdd?: boolean
  // 같은 안내가 반복되지 않게 화면의 첫 목록에만 보여준다 (리뷰 F-07)
  showFeedbackNote?: boolean
}) {
  const { add } = useTodos()

  return (
    <section className="max-lg:border-t max-lg:border-divider max-lg:pt-4 lg:rounded-xl lg:bg-canvas lg:p-4">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h3 className="text-body-strong">{title}</h3>
          <p className="text-caption text-muted">{description}</p>
        </div>
        {bulkAdd && (
          <Button
            size="small"
            onClick={() =>
              items
                .filter((item) => !readExcludedCards().includes(item.cardKey))
                .forEach((item) => add(toTodo(item, tag)))
            }
          >
            전체 추가
          </Button>
        )}
      </div>
      <ul className="mt-3 flex flex-col gap-2">
        {items.map((item) => (
          <RecommendationItem
            key={item.cardKey}
            analysisId={analysisId}
            item={item}
            tag={tag}
          />
        ))}
      </ul>
      {showFeedbackNote && <FeedbackLegend />}
    </section>
  )
}
