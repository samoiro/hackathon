import { createContext, useContext } from 'react'
import type { Todo } from '@/types/todo'

export const TODO_LIMIT = 12

type TodoContextValue = {
  todos: Todo[]
  add: (todo: Todo) => void
  remove: (id: string) => void
  toggleDone: (id: string) => void
}

export const TodoContext = createContext<TodoContextValue | null>(null)

// 추천 할 일(담기)과 오늘 확인할 일(체크·지우기)이 같은 목록을 본다
export function useTodos(): TodoContextValue {
  const value = useContext(TodoContext)
  if (!value) throw new Error('useTodos는 TodoProvider 안에서만 쓸 수 있어요')
  return value
}
