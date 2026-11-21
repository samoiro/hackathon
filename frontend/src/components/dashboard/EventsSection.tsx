import { Card, Tag } from 'antd'
import { useState } from 'react'
import { LuCalendar } from 'react-icons/lu'
import ChoiceChips from '@/components/common/ChoiceChips'
import EmptyState from '@/components/common/EmptyState'
import RecommendationList from '@/components/dashboard/RecommendationList'

const EVENTS = [
  {
    day: '금',
    date: '21',
    kind: '공연',
    status: '예정',
    statusColor: 'processing',
    title: '서울숲 음악회',
    detail: '11.21(금) 16:00–19:00 · 서울숲 야외무대',
    distance: '650m',
    walk: '도보 약 10분',
  },
  {
    day: '토',
    date: '22',
    kind: '전시',
    status: '진행 중',
    statusColor: 'success',
    title: '가을 사진전',
    detail: '11.22(토)까지 · 성수 갤러리',
    distance: '870m',
    walk: '도보 약 13분',
  },
]

export default function EventsSection({ empty = false }: { empty?: boolean }) {
  const [kind, setKind] = useState('전체')

  return (
    <Card id="events">
      <section aria-labelledby="events-heading">
        <div className="flex items-start justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <h2 id="events-heading" className="text-card">
                주변 예정 행사
              </h2>
              {empty ? (
                <Tag>0건</Tag>
              ) : (
                <Tag color="#241a0d" variant="solid">
                  2건
                </Tag>
              )}
            </div>
            <p className="mt-1 text-caption text-muted">
              {empty
                ? '향후 7일 · 1.5km까지 넓혀 찾았어요 · 11월 17일 기준'
                : '향후 7일 · 1km 이내 · 11월 17일 기준'}
            </p>
          </div>
          {!empty && (
            <ChoiceChips
              label="행사 종류"
              value={kind}
              onChange={setKind}
              options={[
                { value: '전체', label: '전체 2' },
                { value: '공연', label: '공연 1' },
                { value: '전시', label: '전시 1' },
              ]}
            />
          )}
        </div>

        {empty ? (
          <div className="mt-4">
            <EmptyState
              icon={LuCalendar}
              title="조건에 맞는 예정 행사가 없어요."
              description="가격과 지역 현황은 계속 확인할 수 있어요."
            />
          </div>
        ) : (
          <>
            <ul className="mt-4 flex flex-col divide-y divide-divider">
              {EVENTS.filter(
                (event) => kind === '전체' || event.kind === kind,
              ).map((event) => (
                <li key={event.title} className="flex items-center gap-4 py-3">
                  <div className="flex w-12 flex-col items-center rounded-xl bg-canvas py-2 text-caption">
                    <span className="text-muted">{event.day}</span>
                    <span className="text-item">{event.date}</span>
                  </div>
                  <div className="flex-1">
                    <p className="flex items-center gap-2 text-item">
                      <Tag color={event.statusColor}>{event.status}</Tag>
                      {event.title}
                    </p>
                    <p className="mt-1 text-caption text-muted">
                      {event.detail}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-item">{event.distance}</p>
                    <p className="text-caption text-muted">{event.walk}</p>
                  </div>
                </li>
              ))}
            </ul>

            <div className="mt-4">
              <RecommendationList
                showFeedbackNote
                tag="행사"
                title="추천 할 일"
                description="조회된 행사 2건을 종합했어요"
                items={[
                  {
                    strong: true,
                    title: '공연 시간대 근무자 역할 나누기',
                    effect:
                      '주문·수령 안내 혼선을 줄이는 데 도움이 될 수 있어요.',
                  },
                  {
                    title: '식재료 재고 확인',
                    effect: '품절 위험에 미리 대비할 수 있어요.',
                  },
                  {
                    title: '금요일 16–19시 포장 동선 정하기',
                    effect:
                      '붐비는 시간대 대기 줄을 줄이는 데 도움이 될 수 있어요.',
                  },
                ]}
              />
            </div>
          </>
        )}
      </section>
    </Card>
  )
}
