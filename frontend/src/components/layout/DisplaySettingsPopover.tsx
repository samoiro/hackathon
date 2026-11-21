import { Button, Popover, Radio } from 'antd'
import { useId, useRef, useState, type ReactNode } from 'react'
import type { IconType } from 'react-icons'
import {
  LuCheck,
  LuCircleCheck,
  LuMonitor,
  LuMoon,
  LuSun,
  LuX,
} from 'react-icons/lu'
import {
  FONT_SCALE_OPTIONS,
  useDisplaySettings,
} from '@/lib/display/displaySettings'
import type { ColorMode } from '@/types/display'

const COLOR_MODE_OPTIONS: {
  value: ColorMode
  label: string
  icon: IconType
}[] = [
  { value: 'light', label: '밝게', icon: LuSun },
  { value: 'dark', label: '어둡게', icon: LuMoon },
  { value: 'system', label: '기기 따라', icon: LuMonitor },
]

const COACH_KEY = 'samoiro:coach-seen'

// 선택 표시는 색 + 체크 + 굵기(리뷰 F-02). 동그라미는 숨기고 카드 전체를 라디오 라벨로 쓴다 (화살표 키 이동은 네이티브 라디오 그대로)
const OPTION =
  'm-0 flex flex-1 cursor-pointer rounded-xl border border-line-strong bg-surface has-[:checked]:border-primary-700 has-[:checked]:bg-primary-50 has-[:checked]:font-semibold has-[:checked]:text-brand-text has-[:checked]:ring-1 has-[:checked]:ring-primary-700 has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-focus [&_.ant-radio]:sr-only'

// '가'(글자) + 반쯤 찬 원(밝기). 글자·화면 팝오버의 두 항목과 짝을 이룬다
function DisplaySettingsGlyph() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden>
      <text x="0" y="16" fontSize="15" fontWeight="900" fill="currentColor">
        가
      </text>
      <circle cx="18" cy="18" r="6.5" fill="var(--color-surface)" />
      <circle
        cx="18"
        cy="18"
        r="4.8"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.6"
      />
      <path d="M18 13.2a4.8 4.8 0 0 1 0 9.6z" fill="currentColor" />
    </svg>
  )
}

function readCoachSeen(): boolean {
  try {
    return localStorage.getItem(COACH_KEY) === '1'
  } catch {
    return true
  }
}

function OptionGroup({
  labelId,
  label,
  hint,
  children,
}: {
  labelId: string
  label: string
  hint?: string
  children: ReactNode
}) {
  return (
    <div>
      <div className="mb-2 flex items-baseline justify-between">
        <span id={labelId} className="text-body-strong">
          {label}
        </span>
        {hint && <span className="text-tag text-muted">{hint}</span>}
      </div>
      {children}
    </div>
  )
}

// 헤더의 글자·화면 설정. 고르면 바로 적용되고, X·Esc로 닫으면 포커스가 버튼으로 돌아온다
export default function DisplaySettingsPopover({
  showCoach = false,
}: {
  showCoach?: boolean
}) {
  const { settings, setSettings } = useDisplaySettings()
  const [open, setOpen] = useState(false)
  const [coachVisible, setCoachVisible] = useState(
    () => showCoach && !readCoachSeen(),
  )
  const triggerRef = useRef<HTMLButtonElement>(null)
  const panelRef = useRef<HTMLDivElement>(null)
  const titleId = useId()
  const sizeLabelId = useId()
  const modeLabelId = useId()
  const coachId = useId()

  const closeAndReturnFocus = () => {
    setOpen(false)
    triggerRef.current?.focus()
  }

  const dismissCoach = () => {
    setCoachVisible(false)
    try {
      localStorage.setItem(COACH_KEY, '1')
    } catch {
      // 저장하지 못하면 다음 방문에 한 번 더 보일 뿐이다
    }
  }

  const panel = (
    <div
      ref={panelRef}
      role="dialog"
      aria-labelledby={titleId}
      className="flex w-80 flex-col gap-4 p-2"
      onKeyDown={(event) => event.key === 'Escape' && closeAndReturnFocus()}
    >
      <div className="flex items-center justify-between">
        <h2 id={titleId} className="text-item">
          글자·화면
        </h2>
        <Button
          type="text"
          size="small"
          aria-label="닫기"
          icon={<LuX aria-hidden />}
          onClick={closeAndReturnFocus}
        />
      </div>

      <OptionGroup
        labelId={sizeLabelId}
        label="글자 크기"
        hint="고르면 바로 바뀌어요"
      >
        <Radio.Group
          aria-labelledby={sizeLabelId}
          value={settings.fontScale}
          onChange={(event) =>
            setSettings({ ...settings, fontScale: event.target.value })
          }
          className="flex gap-2"
        >
          {FONT_SCALE_OPTIONS.map((option) => (
            <Radio key={option.value} value={option.value} className={OPTION}>
              <span className="relative flex h-16 w-full flex-col items-center justify-center">
                {settings.fontScale === option.value && (
                  <LuCircleCheck
                    aria-hidden
                    className="absolute top-1.5 right-1.5 text-primary-700"
                  />
                )}
                <span aria-hidden className={`font-semibold ${option.glyph}`}>
                  가
                </span>
                <span className="text-caption">{option.label}</span>
              </span>
            </Radio>
          ))}
        </Radio.Group>
      </OptionGroup>

      <OptionGroup labelId={modeLabelId} label="화면 모드">
        <Radio.Group
          aria-labelledby={modeLabelId}
          value={settings.colorMode}
          onChange={(event) =>
            setSettings({ ...settings, colorMode: event.target.value })
          }
          className="flex gap-2"
        >
          {COLOR_MODE_OPTIONS.map(({ value, label, icon: Icon }) => {
            const ModeIcon = settings.colorMode === value ? LuCheck : Icon
            return (
              <Radio key={value} value={value} className={OPTION}>
                <span className="flex h-10 w-full items-center justify-center gap-1 text-caption whitespace-nowrap">
                  <ModeIcon aria-hidden />
                  {label}
                </span>
              </Radio>
            )
          })}
        </Radio.Group>
      </OptionGroup>

      <p className="border-t border-divider pt-3 text-tag text-muted">
        이 기기에 저장돼요. 다른 기기에서는 따로 정해요.
      </p>
    </div>
  )

  return (
    <div className="relative">
      <Popover
        open={open}
        onOpenChange={setOpen}
        trigger="click"
        placement="bottomRight"
        arrow={false}
        content={panel}
        afterOpenChange={(visible) =>
          visible &&
          panelRef.current
            ?.querySelector<HTMLInputElement>('input[type=radio]:checked')
            ?.focus()
        }
      >
        <Button
          ref={triggerRef}
          size="small"
          aria-label="글자·화면 설정"
          aria-haspopup="dialog"
          aria-expanded={open}
          aria-describedby={coachVisible ? coachId : undefined}
          title="글자·화면 설정"
          icon={<DisplaySettingsGlyph />}
          onClick={dismissCoach}
        />
      </Popover>

      {coachVisible && (
        <div
          id={coachId}
          className="absolute top-full right-0 z-10 mt-3 flex w-72 flex-col gap-2 rounded-xl bg-hero p-4 text-on-hero"
        >
          <span
            aria-hidden
            className="absolute -top-1 right-3 size-2.5 rotate-45 bg-hero"
          />
          <p className="text-body-strong">
            글자가 작게 느껴지면 여기서 키울 수 있어요
          </p>
          <p className="text-caption opacity-90">
            어두운 화면도 여기서 바꿔요.
          </p>
          <Button
            type="text"
            size="small"
            className="self-end text-primary-300"
            onClick={dismissCoach}
          >
            알겠어요
          </Button>
        </div>
      )}
    </div>
  )
}
