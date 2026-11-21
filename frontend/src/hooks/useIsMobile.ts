import { useSyncExternalStore } from 'react'

// 1024px 미만은 모바일 배치(한 줄 쌓기, 아래 시트). Tailwind의 max-lg:·lg:와 같은 경계다.
// media query의 rem은 기기 기본 글자 크기 기준이라 화면 설정의 글자 크기를 키워도 경계가 바뀌지 않는다
const query = window.matchMedia('(width < 64rem)')
const subscribe = (onChange: () => void) => {
  query.addEventListener('change', onChange)
  return () => query.removeEventListener('change', onChange)
}

export function useIsMobile(): boolean {
  return useSyncExternalStore(subscribe, () => query.matches)
}
