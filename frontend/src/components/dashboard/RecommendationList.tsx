import { useMutation, useQueryClient } from '@tanstack/react-query'
import { App, Button, Tag } from 'antd'
import dayjs from 'dayjs'
import {
  feedback,
  type SuggestionResponse,
  type SuggestionFeedbackRequest,
} from '@/lib/api/generated'
import { readExcludedCards, saveExcludedCard } from '@/lib/api/analysis'
import { useEffect, useRef, useState } from 'react'
import { LuBan, LuCheck, LuPlus, LuThumbsUp } from 'react-icons/lu'
import { TODO_LIMIT, useTodos } from '@/lib/dashboard/todos'
import type { TodoTag } from '@/types/todo'

type Feedback = 'helpful' | 'dismissed' | null

// 담기·피드백은 눌러서 켜고 끄는 버튼(aria-pressed). 모양과 너비는 그대로, 아이콘·글자·색이 함께 바뀐다
// 담기를 누르면 오른쪽 '오늘 확인할 일'에 들어가고, 다시 누르면 빠진다
function RecommendationItem({
  item,
  analysisId,
  tag,
  stacked,
}: {
  item: SuggestionResponse
  analysisId: string
  tag: TodoTag
  stacked: boolean
}) {
  const { todos, add, remove } = useTodos()
  const added = todos.some((todo) => todo.id === item.cardKey)
  const full = !added && todos.length >= TODO_LIMIT
  const [voteState, setFeedback] = useState<Feedback>(() =>
    readExcludedCards().includes(item.cardKey) ? 'dismissed' : null,
  )
  const undoRef = useRef<HTMLButtonElement>(null)
  const dismissRef = useRef<HTMLButtonElement>(null)
  const returnFocus = useRef(false)
  const { notification } = App.useApp()
  const queryClient = useQueryClient()
  const vote = useMutation({
    mutationFn: (value: SuggestionFeedbackRequest['vote']) =>
      feedback({
        path: { id: analysisId },
        body: { cardKey: item.cardKey, vote: value },
        throwOnError: true,
      }),
    onSuccess: async (_, value) => {
      const dismissed = value === 'NOT_RELEVANT'
      returnFocus.current = dismissed
      setFeedback(dismissed ? 'dismissed' : 'helpful')
      saveExcludedCard(item.cardKey, dismissed)
      if (dismissed) remove(item.cardKey)
      notification.open({
        role: 'status',
        title: dismissed ? '추천을 숨겼어요.' : '의견 고마워요!',
        duration: 5,
      })
      await queryClient.invalidateQueries({
        queryKey: ['analysis', analysisId],
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

  return (
    <li
      className={`rounded-xl border border-divider bg-surface p-4 ${
        stacked ? '' : 'xl:flex xl:items-center xl:gap-4'
      }`}
    >
      <div className="flex-1">
        <p className="flex items-center gap-2 text-body-strong">
          {item.recommended && (
            <Tag color="#241a0d" variant="solid">
              강추
            </Tag>
          )}
          {item.title}
        </p>
        <p className="mt-1 text-caption text-muted">{item.reason}</p>
        {item.dueDate && (
          <p className="mt-1 text-caption text-muted">
            {dayjs(item.dueDate).format('M월 D일')}까지
          </p>
        )}
        {item.expectedEffect && (
          <p className="mt-1 text-caption text-muted">
            기대 효과 · {item.expectedEffect}
          </p>
        )}
        {item.subTasks.length > 0 && (
          <ul className="mt-2 list-disc space-y-1 pl-4 text-caption text-muted">
            {item.subTasks.map((task) => (
              <li key={task}>{task}</li>
            ))}
          </ul>
        )}
      </div>
      <div
        className={`flex flex-wrap items-center gap-2 ${stacked ? 'mt-3' : 'max-xl:mt-3'}`}
      >
        <div
          role="group"
          aria-label="이 추천이 도움이 되었나요"
          className="flex flex-wrap items-center gap-2"
        >
          <Button
            size="small"
            color="default"
            variant={voteState === 'helpful' ? 'solid' : 'outlined'}
            aria-pressed={voteState === 'helpful'}
            icon={<LuThumbsUp aria-hidden />}
            loading={vote.isPending && vote.variables === 'HELPFUL'}
            disabled={voteState === 'helpful' || vote.isPending}
            onClick={() => vote.mutate('HELPFUL')}
          >
            도움돼요
          </Button>
          <Button
            ref={dismissRef}
            size="small"
            aria-pressed={false}
            icon={<LuBan aria-hidden />}
            loading={vote.isPending && vote.variables === 'NOT_RELEVANT'}
            disabled={vote.isPending}
            onClick={() => vote.mutate('NOT_RELEVANT')}
          >
            해당 없어요
          </Button>
        </div>
        <Button
          size="small"
          aria-pressed={added}
          // 12개가 차면 더 담을 수 없다. 강추는 오늘 확인할 일에서 뺄 수 없어 담김으로 고정한다
          disabled={full || (added && item.strong)}
          title={
            full ? `할 일은 ${TODO_LIMIT}개까지 담을 수 있어요` : undefined
          }
          icon={added ? <LuCheck aria-hidden /> : <LuPlus aria-hidden />}
          className="min-w-20 aria-pressed:border-success aria-pressed:bg-success-bg aria-pressed:text-success"
          onClick={() =>
            added
              ? remove(item.cardKey)
              : add({
                  id: item.cardKey,
                  title: item.title,
                  tag,
                  strong: item.recommended,
                })
          }
        >
          {added ? '담김' : '담기'}
        </Button>
      </div>
    </li>
  )
}

export default function RecommendationList({
  title,
  description,
  items,
  analysisId,
  tag,
  stacked = false,
  showFeedbackNote = false,
}: {
  title: string
  description: string
  items: SuggestionResponse[]
  analysisId: string
  // 오늘 확인할 일에 붙는 분류
  tag: TodoTag
  stacked?: boolean
  // 같은 안내가 반복되지 않게 화면의 첫 목록에만 보여준다 (리뷰 F-07)
  showFeedbackNote?: boolean
}) {
  const { add } = useTodos()

  return (
    <section className="rounded-xl bg-canvas p-4">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h3 className="text-body-strong">{title}</h3>
          <p className="text-caption text-muted">{description}</p>
        </div>
        <Button
          size="small"
          onClick={() =>
            items
              .filter((item) => !readExcludedCards().includes(item.cardKey))
              .forEach((item) =>
                add({
                  id: item.cardKey,
                  title: item.title,
                  tag,
                  strong: item.recommended,
                }),
              )
          }
        >
          전체 추가
        </Button>
      </div>
      <ul className="mt-3 flex flex-col gap-2">
        {items.map((item) => (
          <RecommendationItem
            key={item.cardKey}
            analysisId={analysisId}
            item={item}
            tag={tag}
            stacked={stacked}
          />
        ))}
      </ul>
      {showFeedbackNote && (
        <p className="mt-3 text-caption text-muted">
          추천마다 <span className="font-semibold text-fg">도움돼요</span>나{' '}
          <span className="font-semibold text-fg">해당 없어요</span>를 눌러
          주시면, 다음 브리핑부터 우리 가게에 맞는 추천을 더 골라 드려요.
        </p>
      )}
    </section>
  )
}
