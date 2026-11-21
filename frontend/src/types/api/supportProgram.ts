// 기업마당 공고 한 건. 제목과 기관명은 원문 그대로 (공공누리 제3유형)
export type SupportProgramResponse = {
  id: string
  title: string
  url: string
  agency: string
  field: string | null
  // LocalDate. '예산 소진시까지'처럼 날짜가 없으면 null
  deadline: string | null
  // 마감일이 없을 때 화면에 그대로 보여준다
  applicationPeriod: string
}

// GET /api/support-programs
export type SupportProgramListResponse = {
  // 최대 3개
  items: SupportProgramResponse[]
  // OffsetDateTime
  checkedAt: string
}
