import { Button, Tag } from 'antd'
import { useState } from 'react'
import { LuChevronUp } from 'react-icons/lu'
import ResponsiveDialog from '@/components/common/ResponsiveDialog'
import WaveProgress from '@/components/common/WaveProgress'
import TodayChecklist from '@/components/dashboard/TodayChecklist'
import { useTodos } from '@/lib/dashboard/todos'

// 모바일 대시보드 아래에 붙는 '오늘 확인할 일' 줄(BI-MO-03). 열기를 누르면 아래 시트로 목록을 띄운다
export default function TodayChecklistBar() {
  const { todos } = useTodos()
  const [open, setOpen] = useState(false)
  const doneCount = todos.filter((todo) => todo.done).length
  const remaining = todos.length - doneCount

  return (
    <div className="sticky bottom-0 -mx-4 flex items-center gap-4 border-t border-divider bg-surface px-4 py-3 lg:hidden">
      <div className="min-w-0 flex-1">
        <p className="flex flex-wrap items-center gap-x-2 gap-y-1">
          <span className="text-body-strong">오늘 확인할 일</span>
          {remaining > 0 && <Tag color="warning">{remaining}건 남음</Tag>}
          <span className="text-caption text-muted">
            {doneCount} / {todos.length} 완료
          </span>
        </p>
        <WaveProgress
          percent={todos.length ? (doneCount / todos.length) * 100 : 0}
          showInfo={false}
          strokeColor="var(--color-aurora-500)"
          aria-label="오늘 할 일 진행률"
        />
      </div>
      <Button
        type="primary"
        aria-haspopup="dialog"
        icon={<LuChevronUp aria-hidden />}
        iconPlacement="end"
        onClick={() => setOpen(true)}
      >
        열기
      </Button>
      <ResponsiveDialog
        open={open}
        onClose={() => setOpen(false)}
        title="오늘 확인할 일"
        footer={
          <Button type="primary" onClick={() => setOpen(false)}>
            확인
          </Button>
        }
      >
        <TodayChecklist sheet />
      </ResponsiveDialog>
    </div>
  )
}
