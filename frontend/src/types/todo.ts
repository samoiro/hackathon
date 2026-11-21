export type TodoTag = '행사' | '원자재' | '부자재'

export type Todo = {
  title: string
  tag: TodoTag
  // AI가 처음에 담아 둔 할 일
  strong?: boolean
  done?: boolean
}
