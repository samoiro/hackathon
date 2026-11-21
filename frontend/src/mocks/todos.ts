import type { Todo } from '@/types/todo'

// shortcut: 강추 할 일 API가 없어 가짜 목록을 0.4초 늦게 돌려준다. 백엔드 연동 때 생성된 SDK 함수로 바꾼다
const STRONG_TODOS: Todo[] = [
  { title: '공연 시간대 근무자 역할 나누기', tag: '행사', strong: true },
  { title: '배추 거래처 단가 확인', tag: '원자재', strong: true },
  { title: '포장 용기 재고 확인', tag: '부자재', strong: true },
]

export const fetchStrongTodos = (): Promise<Todo[]> =>
  new Promise((resolve) => setTimeout(() => resolve(STRONG_TODOS), 400))
