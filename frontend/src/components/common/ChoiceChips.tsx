import { Radio } from 'antd'
import { LuCheck } from 'react-icons/lu'

// 하나만 고르는 칩(필터 등). 네이티브 라디오라 화살표 키로 옮겨 다니고, 선택은 색 + 체크 + 굵기로 보여준다(리뷰 F-02)
export default function ChoiceChips<T extends string>({
  label,
  options,
  value,
  onChange,
  block = false,
}: {
  label: string
  options: { value: T; label: string }[]
  value?: T
  onChange: (value: T) => void
  // 칸을 꽉 채우는 세그먼트 모양 (운영 형태 등)
  block?: boolean
}) {
  return (
    <Radio.Group
      aria-label={label}
      value={value}
      onChange={(event) => onChange(event.target.value)}
      className={
        block
          ? 'grid grid-cols-3 gap-1 rounded-xl border border-divider bg-canvas p-1'
          : 'flex flex-wrap gap-2'
      }
    >
      {options.map((option) => (
        <Radio
          key={option.value}
          value={option.value}
          className={`m-0 flex cursor-pointer items-center text-caption has-[:checked]:border-primary-700 ${
            block
              ? 'h-12 justify-center rounded-lg border border-transparent has-[:checked]:bg-surface'
              : 'h-9 rounded-full border border-line-strong bg-surface px-3 has-[:checked]:bg-primary-50'
          } has-[:checked]:font-semibold has-[:checked]:text-brand-text has-[:checked]:ring-1 has-[:checked]:ring-primary-700 has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-focus [&_.ant-radio]:sr-only`}
        >
          <span className="flex items-center gap-1">
            {value === option.value && <LuCheck aria-hidden />}
            {option.label}
          </span>
        </Radio>
      ))}
    </Radio.Group>
  )
}
