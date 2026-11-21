import { useSyncExternalStore } from 'react'

const query = window.matchMedia('(prefers-reduced-motion: reduce)')
const subscribe = (onChange: () => void) => {
  query.addEventListener('change', onChange)
  return () => query.removeEventListener('change', onChange)
}

// 기기의 '동작 줄이기' 설정. 켜져 있으면 JS로 돌리는 애니메이션(차트 등)을 끈다
export function usePrefersReducedMotion(): boolean {
  return useSyncExternalStore(subscribe, () => query.matches)
}
