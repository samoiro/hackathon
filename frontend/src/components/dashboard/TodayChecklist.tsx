import { Button, Card, Checkbox, Popconfirm, Tag } from 'antd'
import { useId } from 'react'
import { LuX } from 'react-icons/lu'
import WaveProgress from '@/components/common/WaveProgress'
import { TODO_LIMIT, useTodos } from '@/lib/dashboard/todos'

// 추천 할 일에서 담은 항목이 쌓이는 곳. 체크하면 취소선이 그어지고, 모든 항목을 확인 창을 거쳐 지울 수 있다.
// sheet: 모바일 아래 시트(BI-MO-03-1) 안에 넣을 때. 제목은 시트가 보여주므로 카드와 제목 줄을 뺀다
export default function TodayChecklist({ sheet = false }: { sheet?: boolean }) {
  const { todos, remove, toggleDone } = useTodos()
  const headingId = useId()
  const doneCount = todos.filter((todo) => todo.done).length
  const remaining = todos.length - doneCount
  const count = (
    <span className="whitespace-nowrap text-caption">
      <span className="font-semibold">
        {doneCount} / {todos.length}
      </span>{' '}
      <span className="text-muted">완료</span>
    </span>
  )

  const content = (
    <>
      {sheet ? (
        <p className="flex items-center gap-2">
          {count}
          {remaining > 0 && <Tag color="warning">{remaining}건 남음</Tag>}
        </p>
      ) : (
        <div className="flex items-center gap-2">
          <h2 id={headingId} className="text-card">
            오늘 확인할 일
          </h2>
          {remaining > 0 && <Tag color="warning">{remaining}건 남음</Tag>}
          <span className="ml-auto">{count}</span>
        </div>
      )}
      <WaveProgress
        percent={todos.length ? (doneCount / todos.length) * 100 : 0}
        showInfo={false}
        strokeColor="var(--color-aurora-500)"
        aria-label="오늘 할 일 진행률"
      />
      <ul className="mt-2 flex flex-col divide-y divide-divider">
        {todos.map((todo) => (
          <li key={todo.title} className="flex items-start gap-3 py-3">
            <Checkbox
              checked={Boolean(todo.done)}
              onChange={() => toggleDone(todo.title)}
              className="flex-1"
            >
              <span
                className={`block text-item ${todo.done ? 'text-muted line-through' : ''}`}
              >
                {todo.title}
              </span>
              <span className="mt-1 flex gap-1">
                {todo.strong && (
                  <Tag color="#241a0d" variant="solid">
                    강추
                  </Tag>
                )}
                <Tag>{todo.tag}</Tag>
              </span>
            </Checkbox>
            {/* 삭제 확인(BI-PC-05): 아이콘 없이 제목·설명, 같은 너비의 아니요 / 예, 삭제해요(빨강) */}
            <Popconfirm
              icon={null}
              placement="topRight"
              title={<span className="text-item">정말 삭제할까요?</span>}
              description={
                <span className="text-caption text-muted">
                  ‘{todo.title}’이 오늘 확인할 일에서 빠져요. 추천 할 일에서
                  다시 담을 수 있어요.
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
              onConfirm={() => remove(todo.title)}
            >
              <Button
                type="text"
                size="small"
                aria-label={`${todo.title} 지우기`}
                icon={<LuX aria-hidden />}
              />
            </Popconfirm>
          </li>
        ))}
      </ul>
      <p className="mt-2 text-caption text-muted">
        강추는 AI가 처음에 담아 둔 할 일이에요. 모두 {TODO_LIMIT}개까지 담을 수
        있어요.
      </p>
    </>
  )

  if (sheet) return content

  return (
    <Card>
      <section aria-labelledby={headingId}>{content}</section>
    </Card>
  )
}
