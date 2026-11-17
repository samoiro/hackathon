import { StyleProvider } from '@ant-design/cssinjs'
import { QueryClientProvider } from '@tanstack/react-query'
import { ConfigProvider } from 'antd'
import koKR from 'antd/locale/ko_KR'
import dayjs from 'dayjs'
import 'dayjs/locale/ko'
import relativeTime from 'dayjs/plugin/relativeTime'
import type { PropsWithChildren } from 'react'
import { BrowserRouter } from 'react-router'
import { queryClient } from '@/lib/query/queryClient'
import { appTheme } from '@/app/providers/theme'

dayjs.extend(relativeTime)
dayjs.locale('ko')

export default function AppProviders({ children }: PropsWithChildren) {
  return (
    <QueryClientProvider client={queryClient}>
      <StyleProvider layer>
        <ConfigProvider locale={koKR} theme={appTheme}>
          <BrowserRouter>{children}</BrowserRouter>
        </ConfigProvider>
      </StyleProvider>
    </QueryClientProvider>
  )
}
