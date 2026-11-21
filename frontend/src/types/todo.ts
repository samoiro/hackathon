export type TodoTag = '행사' | '원자재' | '부자재'

export type Todo = {
  id: string
  title: string
  tag: TodoTag
  // 강추: 따로 API로 받아오는 급하고 중요한 할 일. 오늘 확인할 일에서 지울 수 없다
  strong?: boolean
  done?: boolean
}
