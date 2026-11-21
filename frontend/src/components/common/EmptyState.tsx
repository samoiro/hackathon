import type { ReactNode } from 'react'
import type { IconType } from 'react-icons'

// 빈 상태 안내 블록: 아이콘(선택) · 무엇이 없는지 · 다음에 할 수 있는 일
export default function EmptyState({
  icon: Icon,
  title,
  description,
  actions,
}: {
  icon?: IconType
  title: string
  description: ReactNode
  actions?: ReactNode
}) {
  return (
    <div className="flex flex-col items-center rounded-xl bg-canvas px-4 py-8 text-center">
      {Icon && (
        <span className="mb-4 flex size-12 items-center justify-center rounded-full border border-divider bg-surface">
          <Icon aria-hidden className="size-5" />
        </span>
      )}
      <p className="text-body-strong">{title}</p>
      <p className="mt-2 text-caption text-muted">{description}</p>
      {actions && (
        <div className="mt-4 flex flex-wrap items-center justify-center gap-x-4 gap-y-2">
          {actions}
        </div>
      )}
    </div>
  )
}
