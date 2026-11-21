import { useEffect, useState } from 'react'

const SECTIONS = [
  { id: 'events', label: '행사' },
  { id: 'prices', label: '관심 품목' },
]

// 행사·관심 품목 섹션 바로가기(피드백 반영안). 내용을 바꾸는 탭이 아니라 링크라서 nav로 두고(리뷰 F-03),
// 화면에 보이는 섹션에 밑줄을 그어 지금 위치를 알려준다
export default function SectionTabs() {
  const [current, setCurrent] = useState(SECTIONS[0].id)

  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries.find((entry) => entry.isIntersecting)
        if (visible) setCurrent(visible.target.id)
      },
      { rootMargin: '-30% 0px -60% 0px' },
    )
    SECTIONS.forEach(({ id }) => {
      const section = document.getElementById(id)
      if (section) observer.observe(section)
    })
    return () => observer.disconnect()
  }, [])

  return (
    <nav aria-label="섹션 바로가기" className="border-b border-divider">
      <ul className="flex gap-6">
        {SECTIONS.map(({ id, label }) => (
          <li key={id}>
            <a
              href={`#${id}`}
              aria-current={current === id ? 'location' : undefined}
              className="-mb-px flex min-h-11 items-center border-b-2 border-transparent text-body text-muted aria-[current]:border-fg aria-[current]:font-semibold aria-[current]:text-fg"
            >
              {label}
            </a>
          </li>
        ))}
      </ul>
    </nav>
  )
}
