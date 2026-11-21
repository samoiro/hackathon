import dayjs from 'dayjs'
import { LuClock3 } from 'react-icons/lu'

// 준비 기한 배지(피드백 반영안): 시계 아이콘 + '준비 기한 · 11.18(화)까지'. 기한이 없는 원자재 할 일은 이번 주 발주 전
export default function DueBadge({ dueDate }: { dueDate?: string }) {
  return (
    <span className="inline-flex items-center gap-1 rounded-full bg-warning-bg px-2 py-0.5 text-tag font-semibold text-warning">
      <LuClock3 aria-hidden />
      준비 기한 ·{' '}
      {dueDate
        ? `${dayjs(dueDate).format('M.D(ddd)')}까지`
        : '이번 주 발주 전까지'}
    </span>
  )
}
