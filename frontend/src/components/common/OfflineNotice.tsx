import { App } from 'antd'
import { useEffect } from 'react'
import { LuTriangleAlert } from 'react-icons/lu'

const KEY = 'offline'

// 네트워크가 끊기면 다시 연결될 때까지 토스트를 띄워 둔다 (BI-PC-05)
export default function OfflineNotice() {
  const { notification } = App.useApp()

  useEffect(() => {
    const showOffline = () =>
      notification.open({
        key: KEY,
        role: 'alert',
        icon: <LuTriangleAlert aria-hidden className="text-primary-300" />,
        title: (
          <span className="text-body-strong">네트워크 연결이 끊겼어요.</span>
        ),
        description: (
          <span className="text-caption">
            연결 상태를 확인한 뒤 다시 시도해 주세요.
          </span>
        ),
        duration: false,
        closable: false,
      })
    const hideOffline = () => notification.destroy(KEY)

    if (!navigator.onLine) showOffline()
    window.addEventListener('offline', showOffline)
    window.addEventListener('online', hideOffline)
    return () => {
      window.removeEventListener('offline', showOffline)
      window.removeEventListener('online', hideOffline)
    }
  }, [notification])

  return null
}
