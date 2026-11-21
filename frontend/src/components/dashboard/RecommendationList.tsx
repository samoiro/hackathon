import { App, Button, Tag } from 'antd'
import { useEffect, useRef, useState } from 'react'
import { LuBan, LuCheck, LuPlus, LuThumbsUp } from 'react-icons/lu'
import { TODO_LIMIT, useTodos } from '@/lib/dashboard/todos'
import type { TodoTag } from '@/types/todo'

export type Recommendation = {
  title: string
  effect: string
  strong?: boolean
}

type Feedback = 'helpful' | 'dismissed' | null

// 담기·피드백은 눌러서 켜고 끄는 버튼(aria-pressed). 모양과 너비는 그대로, 아이콘·글자·색이 함께 바뀐다
// 담기를 누르면 오른쪽 '오늘 확인할 일'에 들어가고, 다시 누르면 빠진다
// shortcut: 피드백은 화면 안에서만 유지된다. 기능 구현 때 피드백 mutation에 연결한다
function RecommendationItem({
  item,
  tag,
  stacked,
}: {
  item: Recommendation
  tag: TodoTag
  stacked: boolean
}) {
  const { todos, add, remove } = useTodos()
  const added = todos.some((todo) => todo.title === item.title)
  const full = !added && todos.length >= TODO_LIMIT
  const [feedback, setFeedback] = useState<Feedback>(null)
  const undoRef = useRef<HTMLButtonElement>(null)
  const dismissRef = useRef<HTMLButtonElement>(null)
  const returnFocus = useRef(false)
  const { notification } = App.useApp()

  // 누른 버튼이 사라지므로 포커스를 이어지는 버튼으로 옮긴다 (해당 없어요 ↔ 되돌리기)
  useEffect(() => {
    if (!returnFocus.current) return
    returnFocus.current = false
    if (feedback === 'dismissed') undoRef.current?.focus()
    else dismissRef.current?.focus()
  }, [feedback])

  const changeFeedback = (next: Feedback) => {
    returnFocus.current = next === 'dismissed' || feedback === 'dismissed'
    setFeedback(next)
    // 피드백을 남겼을 때만 토스트(5초 뒤 사라짐). 되돌리기·도움돼요 끄기는 조용히
    if (next)
      notification.open({
        role: 'status',
        icon: <LuCheck aria-hidden className="text-primary-300" />,
        title: (
          <span className="text-body-strong">
            {next === 'helpful' ? '의견 고마워요!' : '알려 주셔서 고마워요'}
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
  }

  if (feedback === 'dismissed') {
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
          onClick={() => changeFeedback(null)}
        >
          되돌리기
        </Button>
      </li>
    )
  }

  return (
    <li
      className={`rounded-xl border border-divider bg-surface p-4 ${
        stacked ? '' : 'lg:flex lg:items-center lg:gap-4'
      }`}
    >
      <div className="flex-1">
        <p className="flex items-center gap-2 text-body-strong">
          {item.strong && (
            <Tag color="#241a0d" variant="solid">
              강추
            </Tag>
          )}
          {item.title}
        </p>
        <p className="mt-1 text-caption text-muted">
          기대 효과 · {item.effect}
        </p>
      </div>
      <div
        className={`flex flex-wrap items-center gap-2 ${stacked ? 'mt-3' : 'max-lg:mt-3'}`}
      >
        <div
          role="group"
          aria-label="이 추천이 도움이 되었나요"
          className="flex items-center gap-2"
        >
          <Button
            size="small"
            color="default"
            variant={feedback === 'helpful' ? 'solid' : 'outlined'}
            aria-pressed={feedback === 'helpful'}
            icon={<LuThumbsUp aria-hidden />}
            onClick={() =>
              changeFeedback(feedback === 'helpful' ? null : 'helpful')
            }
          >
            도움돼요
          </Button>
          <Button
            ref={dismissRef}
            size="small"
            aria-pressed={false}
            icon={<LuBan aria-hidden />}
            onClick={() => changeFeedback('dismissed')}
          >
            해당 없어요
          </Button>
        </div>
        <Button
          size="small"
          aria-pressed={added}
          // 12개가 차면 더 담을 수 없다
          disabled={full}
          title={
            full ? `할 일은 ${TODO_LIMIT}개까지 담을 수 있어요` : undefined
          }
          icon={added ? <LuCheck aria-hidden /> : <LuPlus aria-hidden />}
          className="min-w-20 aria-pressed:border-success aria-pressed:bg-success-bg aria-pressed:text-success"
          onClick={() =>
            added
              ? remove(item.title)
              : add({ title: item.title, tag, strong: item.strong })
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
  tag,
  stacked = false,
  showFeedbackNote = false,
}: {
  title: string
  description: string
  items: Recommendation[]
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
            items.forEach((item) =>
              add({ title: item.title, tag, strong: item.strong }),
            )
          }
        >
          전체 추가
        </Button>
      </div>
      <ul className="mt-3 flex flex-col gap-2">
        {items.map((item) => (
          <RecommendationItem
            key={item.title}
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
