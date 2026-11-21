import { Progress, type ProgressProps } from 'antd'
import { useEffect, useState } from 'react'
import { usePrefersReducedMotion } from '@/hooks/usePrefersReducedMotion'

// antd Progress에 두 가지 움직임을 더한다. 나타날 때 0에서 값까지 차오르고, 멈춰 있을 때는 막대 위로 물결이 천천히 흐른다.
// 움직임은 index.css의 .progress-wave가 맡고, 동작 줄이기 설정에서는 둘 다 멈춘다
export default function WaveProgress({
  percent = 0,
  className = '',
  ...props
}: ProgressProps) {
  const reduceMotion = usePrefersReducedMotion()
  const [shownPercent, setShownPercent] = useState(reduceMotion ? percent : 0)

  // 첫 그림을 0으로 그린 다음 프레임에 값을 넣어야 너비 전환이 보인다
  useEffect(() => {
    const frame = requestAnimationFrame(() => setShownPercent(percent))
    return () => cancelAnimationFrame(frame)
  }, [percent])

  return (
    <Progress
      {...props}
      percent={shownPercent}
      className={`progress-wave ${className}`}
    />
  )
}
