// 서버 오류 응답: Spring ProblemDetail (RFC 9457)
export type ProblemDetail = {
  type?: string
  title?: string
  status: number
  detail?: string
  instance?: string
}
