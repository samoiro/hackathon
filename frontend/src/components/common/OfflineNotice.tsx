import { notification } from 'antd'
import { useEffect } from 'react'

const KEY = 'offline'

// 네트워크가 끊기면 다시 연결될 때까지 안내를 띄워 둔다
export default function OfflineNotice() {
  const [api, contextHolder] = notification.useNotification()

  useEffect(() => {
    const showOffline = () =>
      api.warning({
        key: KEY,
        title: '네트워크 연결이 끊겼어요.',
        description: '연결 상태를 확인한 뒤 다시 시도해 주세요.',
        duration: 0,
        placement: 'bottomRight',
      })
    const hideOffline = () => api.destroy(KEY)

    if (!navigator.onLine) showOffline()
    window.addEventListener('offline', showOffline)
    window.addEventListener('online', hideOffline)
    return () => {
      window.removeEventListener('offline', showOffline)
      window.removeEventListener('online', hideOffline)
    }
  }, [api])

  return contextHolder
}
