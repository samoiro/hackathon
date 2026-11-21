import { useQuery } from '@tanstack/react-query'
import { useMemo, useState, type ReactNode } from 'react'
import { TODO_LIMIT, TodoContext } from '@/lib/dashboard/todos'
import { fetchStrongTodos } from '@/mocks/todos'
import type { Todo } from '@/types/todo'

// 사장님이 추천 할 일에서 담은 목록. 강추는 따로 API로 받아오고 여기에 넣지 않는다
const INITIAL_ADDED: Todo[] = [
  { title: '식재료 재고 확인', tag: '행사', done: true },
]

// 오늘 확인할 일 = 강추(API, 지울 수 없음) + 담은 할 일(추가·삭제 가능)
// shortcut: 담기·체크는 화면 안에서만 유지된다. 기능 구현 때 추가·체크·삭제 mutation으로 바꾼다
export default function TodoProvider({ children }: { children: ReactNode }) {
  const { data: strongTodos = [] } = useQuery({
    queryKey: ['todos', 'strong'],
    queryFn: fetchStrongTodos,
  })
  const [added, setAdded] = useState(INITIAL_ADDED)
  // 강추의 체크 여부. 서버 목록을 복사하지 않고 체크한 제목만 따로 둔다
  const [doneStrong, setDoneStrong] = useState<string[]>([])

  const value = useMemo(() => {
    const todos = [
      ...strongTodos.map((todo) => ({
        ...todo,
        done: doneStrong.includes(todo.title),
      })),
      ...added,
    ]
    const isStrong = (title: string) =>
      strongTodos.some((todo) => todo.title === title)

    return {
      todos,
      add: (todo: Todo) => {
        if (
          todos.length >= TODO_LIMIT ||
          todos.some((item) => item.title === todo.title)
        )
          return
        setAdded((prev) => [...prev, { ...todo, strong: false }])
      },
      // 강추는 added에 없으므로 지워지지 않는다
      remove: (title: string) =>
        setAdded((prev) => prev.filter((item) => item.title !== title)),
      toggleDone: (title: string) =>
        isStrong(title)
          ? setDoneStrong((prev) =>
              prev.includes(title)
                ? prev.filter((item) => item !== title)
                : [...prev, title],
            )
          : setAdded((prev) =>
              prev.map((item) =>
                item.title === title ? { ...item, done: !item.done } : item,
              ),
            ),
    }
  }, [strongTodos, added, doneStrong])

  return <TodoContext value={value}>{children}</TodoContext>
}
