import { createContext, useContext } from 'react'
import type { ColorMode, DisplaySettings, FontScale } from '@/types/display'

const STORAGE_KEY = 'samoiro:display'
const FONT_SCALES: FontScale[] = ['normal', 'large', 'xlarge']
const COLOR_MODES: ColorMode[] = ['light', 'dark', 'system']

export const DEFAULT_DISPLAY_SETTINGS: DisplaySettings = {
  fontScale: 'normal',
  colorMode: 'light',
}

// 저장 값은 사용자가 고칠 수 있으니 알려진 값만 받는다. 저장소를 못 쓰는 환경(사생활 보호 모드 등)에서는 기본값
export function loadDisplaySettings(): DisplaySettings {
  try {
    const saved: unknown = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}')
    if (typeof saved !== 'object' || saved === null)
      return DEFAULT_DISPLAY_SETTINGS
    const fontScale = FONT_SCALES.find(
      (v) => v === Reflect.get(saved, 'fontScale'),
    )
    const colorMode = COLOR_MODES.find(
      (v) => v === Reflect.get(saved, 'colorMode'),
    )
    return {
      fontScale: fontScale ?? DEFAULT_DISPLAY_SETTINGS.fontScale,
      colorMode: colorMode ?? DEFAULT_DISPLAY_SETTINGS.colorMode,
    }
  } catch {
    return DEFAULT_DISPLAY_SETTINGS
  }
}

export function saveDisplaySettings(settings: DisplaySettings): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(settings))
  } catch {
    // 저장하지 못해도 이번 방문 동안은 설정이 적용된다
  }
}

export const DisplaySettingsContext = createContext<{
  settings: DisplaySettings
  setSettings: (settings: DisplaySettings) => void
}>({ settings: DEFAULT_DISPLAY_SETTINGS, setSettings: () => {} })

export const useDisplaySettings = () => useContext(DisplaySettingsContext)

// 헤더 단축 버튼과 화면 설정 모달이 같은 목록을 쓴다. glyph는 '가' 글자를 보여줄 크기
export const FONT_SCALE_OPTIONS: {
  value: FontScale
  label: string
  glyph: string
}[] = [
  { value: 'normal', label: '보통', glyph: 'text-[14px]' },
  { value: 'large', label: '크게', glyph: 'text-[17px]' },
  { value: 'xlarge', label: '아주 크게', glyph: 'text-[20px]' },
]
