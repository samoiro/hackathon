import { App, Button, Card, Checkbox, Popconfirm, Tag } from 'antd'
import { useId, useState } from 'react'
import { LuBan, LuCheck, LuChevronDown, LuChevronUp, LuX } from 'react-icons/lu'
import WaveProgress from '@/components/common/WaveProgress'
import DueBadge from '@/components/dashboard/DueBadge'
import { useSuggestionFeedback } from '@/lib/api/analysis'
import { TODO_LIMIT, useTodos } from '@/lib/dashboard/todos'
import type { Todo } from '@/types/todo'

// AI 추천(강추) 한 줄: 이유 · 준비 기한 · 이미 했어요 · 해당 없어요.
// 해당 없어요는 추천 카드와 같은 피드백을 보내고 목록에서 뺀다 (다음 분석에서도 제외)
function StrongItem({ todo, analysisId }: { todo: Todo; analysisId: string }) {
  const { toggleDone, dismiss } = useTodos()
  const { notification } = App.useApp()
  const vote = useSuggestionFeedback(analysisId, todo.id, {
    onSuccess: () => {
      dismiss(todo.id)
      notification.open({
        role: 'status',
        icon: <LuCheck aria-hidden className="text-primary-300" />,
        title: <span className="text-body-strong">알려 주셔서 고마워요</span>,
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

  return (
    <li className="flex items-start gap-3 py-3">
      <Checkbox
        checked={false}
        onChange={() => toggleDone(todo.id)}
        aria-label={`${todo.title} 완료`}
        className="mt-1"
      />
      <div className="min-w-0 flex-1">
        <p className="text-item">{todo.title}</p>
        <p className="mt-1 flex gap-1">
          <Tag color="#241a0d" variant="solid">
            강추
          </Tag>
          <Tag>{todo.tag}</Tag>
        </p>
        {todo.reason && (
          <p className="mt-1 text-caption text-muted">이유 · {todo.reason}</p>
        )}
        <p className="mt-2">
          <DueBadge dueDate={todo.dueDate} />
        </p>
        <div className="mt-2 flex flex-wrap gap-2">
          <Button
            size="small"
            icon={<LuCheck aria-hidden />}
            onClick={() => toggleDone(todo.id)}
          >
            이미 했어요
          </Button>
          <Button
            size="small"
            icon={<LuBan aria-hidden />}
            loading={vote.isPending}
            onClick={() => vote.mutate('NOT_RELEVANT')}
          >
            해당 없어요
          </Button>
        </div>
      </div>
    </li>
  )
}

// 직접 추가한 할 일: 체크 · 준비 기한 · 삭제(확인 창)
function AddedItem({ todo }: { todo: Todo }) {
  const { remove, toggleDone } = useTodos()
  return (
    <li className="flex items-start gap-3 py-3">
      <Checkbox
        checked={false}
        onChange={() => toggleDone(todo.id)}
        aria-label={`${todo.title} 완료`}
        className="mt-1"
      />
      <div className="min-w-0 flex-1">
        <p className="text-item">{todo.title}</p>
        <p className="mt-1 flex gap-1">
          <Tag className="border-fg">직접 추가</Tag>
          <Tag>{todo.tag}</Tag>
        </p>
        <p className="mt-2">
          <DueBadge dueDate={todo.dueDate} />
        </p>
      </div>
      {/* 삭제 확인(BI-PC-05): 아이콘 없이 제목·설명, 같은 너비의 아니요 / 예, 삭제해요(빨강) */}
      <Popconfirm
        icon={null}
        placement="topRight"
        title={<span className="text-item">정말 삭제할까요?</span>}
        description={
          <span className="text-caption text-muted">
            ‘{todo.title}’이 오늘 확인할 일에서 빠져요. 추천 할 일에서 다시 담을
            수 있어요.
          </span>
        }
        okText="예, 삭제해요"
        cancelText="아니요"
        classNames={{ root: 'w-100 max-w-[calc(100vw-32px)]' }}
        okButtonProps={{
          danger: true,
          size: 'middle',
          className: 'w-[calc(50%-4px)]',
        }}
        cancelButtonProps={{
          size: 'middle',
          className: 'w-[calc(50%-4px)]',
        }}
        onConfirm={() => remove(todo.id)}
      >
        <Button
          type="text"
          size="small"
          aria-label={`${todo.title} 지우기`}
          icon={<LuX aria-hidden />}
        />
      </Popconfirm>
    </li>
  )
}

function GroupHeading({ title, note }: { title: string; note: string }) {
  return (
    <div className="mt-4">
      <h3 className="text-body-strong">{title}</h3>
      <p className="text-tag text-muted">{note}</p>
    </div>
  )
}

// 오늘 확인할 일(피드백 반영안): AI 추천 · 직접 추가 · 완료한 일(접힘) 세 묶음.
// sheet: 모바일 아래 시트(BI-MO-03-1) 안에 넣을 때. 제목은 시트가 보여주므로 카드와 제목 줄을 뺀다
export default function TodayChecklist({
  analysisId,
  sheet = false,
}: {
  analysisId: string
  sheet?: boolean
}) {
  const { todos, toggleDone } = useTodos()
  const headingId = useId()
  const doneListId = useId()
  const [doneOpen, setDoneOpen] = useState(false)
  const strong = todos.filter((todo) => todo.strong && !todo.done)
  const added = todos.filter((todo) => !todo.strong && !todo.done)
  const done = todos.filter((todo) => todo.done)
  const remaining = strong.length + added.length

  const content = (
    <>
      {sheet ? (
        <p className="text-caption text-muted">
          {done.length} / {todos.length} 완료 · AI 추천 {strong.length} · 직접
          추가 {added.length}
        </p>
      ) : (
        <div className="flex items-center gap-2">
          <h2 id={headingId} className="text-card">
            오늘 확인할 일
          </h2>
          {remaining > 0 && <Tag color="warning">{remaining}건 남음</Tag>}
          <span className="ml-auto whitespace-nowrap text-caption">
            <span className="font-semibold">
              {done.length} / {todos.length}
            </span>{' '}
            <span className="text-muted">완료</span>
          </span>
        </div>
      )}
      <WaveProgress
        percent={todos.length ? (done.length / todos.length) * 100 : 0}
        showInfo={false}
        strokeColor="var(--color-aurora-500)"
        aria-label="오늘 할 일 진행률"
      />

      {strong.length > 0 && (
        <>
          <GroupHeading
            title={`AI 추천 · ${strong.length}`}
            note="근거가 많은 일 3개를 먼저 담아 뒀어요"
          />
          <ul className="flex flex-col divide-y divide-divider">
            {strong.map((todo) => (
              <StrongItem key={todo.id} todo={todo} analysisId={analysisId} />
            ))}
          </ul>
        </>
      )}

      {added.length > 0 && (
        <>
          <GroupHeading
            title={`직접 추가 · ${added.length}`}
            note="사장님이 담은 일"
          />
          <ul className="flex flex-col divide-y divide-divider">
            {added.map((todo) => (
              <AddedItem key={todo.id} todo={todo} />
            ))}
          </ul>
        </>
      )}

      {done.length > 0 && (
        <div className="mt-3 rounded-xl bg-canvas">
          <button
            type="button"
            aria-expanded={doneOpen}
            aria-controls={doneListId}
            onClick={() => setDoneOpen((open) => !open)}
            className="flex min-h-11 w-full items-center gap-2 px-3 text-caption font-semibold"
          >
            <LuCheck aria-hidden className="text-success" />
            완료한 일 · {done.length}
            <span className="ml-auto flex items-center gap-1 text-muted">
              {doneOpen ? '접기' : '펼치기'}
              {doneOpen ? (
                <LuChevronUp aria-hidden />
              ) : (
                <LuChevronDown aria-hidden />
              )}
            </span>
          </button>
          {doneOpen && (
            <ul id={doneListId} className="px-3 pb-2">
              {done.map((todo) => (
                <li key={todo.id} className="py-1">
                  <Checkbox checked onChange={() => toggleDone(todo.id)}>
                    <span className="text-caption text-muted line-through">
                      {todo.title}
                    </span>
                  </Checkbox>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      <p className="mt-3 text-caption text-muted">
        강추는 근거가 많은 일 3개예요. 이미 했거나 맞지 않으면 정리할 수 있고,
        모두 {TODO_LIMIT}개까지 담을 수 있어요.
      </p>
    </>
  )

  if (sheet) return content

  return (
    <Card id="today">
      <section aria-labelledby={headingId}>{content}</section>
    </Card>
  )
}
