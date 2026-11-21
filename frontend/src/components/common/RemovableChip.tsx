import { LuX } from 'react-icons/lu'

// 고른 항목 칩(높이 36, 글자 14). antd Tag의 닫기 아이콘은 키보드로 누를 수 없어서, 빼기는 진짜 버튼으로 둔다
export default function RemovableChip({
  label,
  onRemove,
}: {
  label: string
  onRemove: () => void
}) {
  return (
    <span className="inline-flex h-9 items-center gap-1 rounded-full border border-line-strong bg-surface pr-1 pl-3 text-caption">
      {label}
      <button
        type="button"
        aria-label={`${label} 빼기`}
        onClick={onRemove}
        className="flex size-7 items-center justify-center rounded-full hover:bg-canvas"
      >
        <LuX aria-hidden />
      </button>
    </span>
  )
}
