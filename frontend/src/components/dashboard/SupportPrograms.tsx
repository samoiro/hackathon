import { Button, Card, Tag } from 'antd'
import { useEffect, useRef, useState } from 'react'
import {
  LuChevronLeft,
  LuChevronRight,
  LuExternalLink,
  LuPause,
  LuPlay,
} from 'react-icons/lu'
import { usePrefersReducedMotion } from '@/hooks/usePrefersReducedMotion'

// 마감이 가까운 순서. 기준일 11.17(월)에서 센 D-day
const PROGRAMS = [
  {
    dDay: '마감 D-3',
    kind: '컨설팅',
    title: '소상공인 경영 컨설팅 지원',
    detail: '신청 마감 11.20(목) · 소상공인시장진흥공단',
  },
  {
    dDay: '마감 D-5',
    kind: '정책자금',
    title: '소상공인 정책자금 · 일반경영안정자금',
    detail: '신청 마감 11.22(토) · 중소벤처기업부',
  },
  {
    dDay: '마감 D-7',
    kind: '교육',
    title: '스마트상점 기술보급 사업',
    detail: '신청 마감 11.24(월) · 소상공인시장진흥공단',
  },
]

const AUTO_MS = 3000

// 가로 스크롤 목록을 이전·다음 버튼, 점, 손으로 밀기(스크롤) 세 가지로 넘기고, 3초마다 자동으로 다음 공고로 돈다.
// 자동 넘김은 멈춤 버튼으로 끌 수 있고, 마우스를 올리거나 키보드로 안에 들어오면 잠시 멈춘다(WCAG 2.2.2).
// 동작 줄이기 설정이면 처음부터 꺼 둔다
// 지금 위치는 스크롤 위치에서 계산하므로 어떤 방법으로 넘겨도 숫자와 점이 맞는다
// shortcut: 공고는 가짜 데이터다. 기능 구현 때 지원사업 API로 바꾼다
// 목록 상자만 가로로 옮긴다. scrollIntoView는 페이지 전체까지 캐러셀 쪽으로 끌고 가서 쓰지 않는다
function scrollListTo(
  list: HTMLUListElement | null,
  index: number,
  smooth: boolean,
) {
  const items = [...(list?.querySelectorAll('li') ?? [])]
  if (!list || !items[index]) return
  list.scrollTo({
    left: items[index].offsetLeft - items[0].offsetLeft,
    behavior: smooth ? 'smooth' : 'auto',
  })
}

export default function SupportPrograms() {
  const listRef = useRef<HTMLUListElement>(null)
  const [index, setIndex] = useState(0)
  const reduceMotion = usePrefersReducedMotion()
  const [playing, setPlaying] = useState(!reduceMotion)
  const [hovered, setHovered] = useState(false)
  const [focused, setFocused] = useState(false)
  const rotating = playing && !hovered && !focused

  const cards = () => [...(listRef.current?.querySelectorAll('li') ?? [])]

  const goTo = (next: number) =>
    scrollListTo(listRef.current, next, !reduceMotion)

  const step = (delta: number) =>
    goTo((index + delta + PROGRAMS.length) % PROGRAMS.length)

  useEffect(() => {
    if (!rotating) return
    const timer = setTimeout(
      () => scrollListTo(listRef.current, (index + 1) % PROGRAMS.length, true),
      AUTO_MS,
    )
    return () => clearTimeout(timer)
  }, [index, rotating])

  const syncIndex = () => {
    const list = listRef.current
    const items = cards()
    if (!list || items.length === 0) return
    const start = items[0].offsetLeft
    const distances = items.map((item) =>
      Math.abs(item.offsetLeft - start - list.scrollLeft),
    )
    setIndex(distances.indexOf(Math.min(...distances)))
  }

  return (
    <Card>
      <section
        aria-labelledby="support-heading"
        aria-roledescription="캐러셀"
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
        onFocus={() => setFocused(true)}
        onBlur={(event) =>
          !event.currentTarget.contains(event.relatedTarget) &&
          setFocused(false)
        }
      >
        <div className="flex flex-wrap items-center gap-2">
          <h2
            id="support-heading"
            className="flex-1 text-card max-lg:basis-full"
          >
            마감 임박 지원사업
          </h2>
          {/* 자동으로 돌 때는 위치를 읽지 않는다. 멈췄을 때만 넘길 때마다 알려준다 */}
          <span
            aria-live={rotating ? 'off' : 'polite'}
            className="text-caption text-muted"
          >
            <span className="sr-only">공고 </span>
            {index + 1} / {PROGRAMS.length}
          </span>
          <Button
            size="small"
            aria-label={playing ? '자동 넘김 멈추기' : '자동 넘김 켜기'}
            icon={playing ? <LuPause aria-hidden /> : <LuPlay aria-hidden />}
            onClick={() => setPlaying((value) => !value)}
          />
          <Button
            size="small"
            aria-label="이전 공고"
            icon={<LuChevronLeft aria-hidden />}
            onClick={() => step(-1)}
          />
          <Button
            size="small"
            aria-label="다음 공고"
            icon={<LuChevronRight aria-hidden />}
            onClick={() => step(1)}
          />
        </div>
        <p className="mt-1 text-caption text-muted">
          서울 · 일반 음식점 조건에 맞는 공고 {PROGRAMS.length}건
        </p>
        <ul
          ref={listRef}
          onScroll={syncIndex}
          className="mt-4 flex snap-x snap-mandatory gap-3 overflow-x-auto [scrollbar-width:none]"
        >
          {PROGRAMS.map((program, position) => (
            <li
              key={program.title}
              aria-label={`${PROGRAMS.length}개 중 ${position + 1}번째`}
              className="w-[85%] shrink-0 snap-start rounded-xl bg-canvas p-4"
            >
              <span className="flex gap-1">
                <Tag color="warning">{program.dDay}</Tag>
                <Tag>{program.kind}</Tag>
              </span>
              <p className="mt-2 text-body-strong">{program.title}</p>
              <p className="mt-1 text-caption text-muted">{program.detail}</p>
              <a
                href="/"
                className="mt-3 inline-flex items-center gap-1 text-caption font-semibold underline"
              >
                공고 보기 <LuExternalLink aria-hidden />
              </a>
            </li>
          ))}
        </ul>
        <div className="mt-3 flex justify-center gap-1">
          {PROGRAMS.map((program, position) => (
            <button
              key={program.title}
              type="button"
              aria-label={`${position + 1}번째 공고 보기`}
              aria-current={position === index}
              onClick={() => goTo(position)}
              className="flex size-6 items-center justify-center"
            >
              <span
                className={`size-2 rounded-full ${position === index ? 'bg-fg' : 'bg-line-strong'}`}
              />
            </button>
          ))}
        </div>
        <p className="mt-4 text-caption text-muted">
          마감 7일 이내 공고만 보여드려요. 주 1회 갱신 · 중소벤처기업부 지원사업
          공고
        </p>
      </section>
    </Card>
  )
}
