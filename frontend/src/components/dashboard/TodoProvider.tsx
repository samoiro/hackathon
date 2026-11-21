import { useMemo, useState, type ReactNode } from 'react'
import { TODO_LIMIT, TodoContext } from '@/lib/dashboard/todos'
import type { Todo } from '@/types/todo'

const INITIAL_TODOS: Todo[] = [
  { title: '공연 시간대 근무자 역할 나누기', tag: '행사', strong: true },
  { title: '배추 거래처 단가 확인', tag: '원자재', strong: true },
  { title: '포장 용기 재고 확인', tag: '부자재', strong: true },
  { title: '식재료 재고 확인', tag: '행사', done: true },
]

// shortcut: 할 일은 화면 안에서만 유지된다. 기능 구현 때 서버 목록(useQuery)과 추가·체크·삭제 mutation으로 바꾼다
export default function TodoProvider({ children }: { children: ReactNode }) {
  const [todos, setTodos] = useState(INITIAL_TODOS)

  const value = useMemo(
    () => ({
      todos,
      add: (todo: Todo) =>
        setTodos((prev) =>
          prev.some((item) => item.title === todo.title) ||
          prev.length >= TODO_LIMIT
            ? prev
            : [...prev, todo],
        ),
      remove: (title: string) =>
        setTodos((prev) => prev.filter((item) => item.title !== title)),
      toggleDone: (title: string) =>
        setTodos((prev) =>
          prev.map((item) =>
            item.title === title ? { ...item, done: !item.done } : item,
          ),
        ),
    }),
    [todos],
  )

  return <TodoContext value={value}>{children}</TodoContext>
}
