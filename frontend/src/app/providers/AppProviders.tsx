import { StyleProvider } from '@ant-design/cssinjs'
import { QueryClientProvider } from '@tanstack/react-query'
import { ConfigProvider } from 'antd'
import koKR from 'antd/locale/ko_KR'
import dayjs from 'dayjs'
import 'dayjs/locale/ko'
import relativeTime from 'dayjs/plugin/relativeTime'
import {
  useEffect,
  useMemo,
  useState,
  useSyncExternalStore,
  type PropsWithChildren,
} from 'react'
import { BrowserRouter } from 'react-router'
import { queryClient } from '@/lib/query/queryClient'
import { FONT_SCALE, getAppTheme } from '@/app/providers/theme'
import {
  DisplaySettingsContext,
  loadDisplaySettings,
  saveDisplaySettings,
} from '@/lib/display/displaySettings'

dayjs.extend(relativeTime)
dayjs.locale('ko')

const darkQuery = window.matchMedia('(prefers-color-scheme: dark)')
const subscribeSystemDark = (onChange: () => void) => {
  darkQuery.addEventListener('change', onChange)
  return () => darkQuery.removeEventListener('change', onChange)
}

export default function AppProviders({ children }: PropsWithChildren) {
  const [settings, setSettings] = useState(loadDisplaySettings)
  const systemDark = useSyncExternalStore(
    subscribeSystemDark,
    () => darkQuery.matches,
  )
  const dark =
    settings.colorMode === 'dark' ||
    (settings.colorMode === 'system' && systemDark)
  const scale = FONT_SCALE[settings.fontScale]
  const appTheme = useMemo(() => getAppTheme(dark, scale), [dark, scale])
  const displayContext = useMemo(() => ({ settings, setSettings }), [settings])

  // Tailwind 색 토큰(data-theme)과 rem 크기(html font-size)를 설정에 맞춘다
  useEffect(() => {
    const root = document.documentElement
    root.dataset.theme = dark ? 'dark' : 'light'
    root.style.fontSize = `${scale * 100}%`
    saveDisplaySettings(settings)
  }, [dark, scale, settings])

  return (
    <QueryClientProvider client={queryClient}>
      <StyleProvider layer>
        <ConfigProvider locale={koKR} theme={appTheme}>
          <DisplaySettingsContext value={displayContext}>
            <BrowserRouter>{children}</BrowserRouter>
          </DisplaySettingsContext>
        </ConfigProvider>
      </StyleProvider>
    </QueryClientProvider>
  )
}
