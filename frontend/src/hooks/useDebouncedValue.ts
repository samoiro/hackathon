import { useEffect, useState } from 'react'

// 입력이 delay(ms) 동안 멈춘 뒤의 값만 돌려준다. 검색 API를 글자마다 부르지 않게 한다
export function useDebouncedValue<T>(value: T, delay = 300): T {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(timer)
  }, [value, delay])

  return debounced
}
