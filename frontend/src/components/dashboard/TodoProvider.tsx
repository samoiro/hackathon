import { useMemo, useState, type ReactNode } from 'react'
import { TODO_LIMIT, TodoContext } from '@/lib/dashboard/todos'
import type { Todo } from '@/types/todo'

// shortcut: 별도 할 일 저장 API가 나오면 서버 목록과 변경 요청으로 바꾼다.
export default function TodoProvider({
  children,
  initial,
}: {
  children: ReactNode
  initial: Todo[]
}) {
  const [todos, setTodos] = useState(initial)

  const value = useMemo(
    () => ({
      todos,
      add: (todo: Todo) =>
        setTodos((prev) =>
          prev.some((item) => item.id === todo.id) || prev.length >= TODO_LIMIT
            ? prev
            : [...prev, todo],
        ),
      remove: (id: string) =>
        setTodos((prev) => prev.filter((item) => item.id !== id)),
      toggleDone: (id: string) =>
        setTodos((prev) =>
          prev.map((item) =>
            item.id === id ? { ...item, done: !item.done } : item,
          ),
        ),
    }),
    [todos],
  )

  return <TodoContext value={value}>{children}</TodoContext>
}
