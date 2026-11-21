import { Card, Tag } from 'antd'
import dayjs from 'dayjs'
import { useState } from 'react'
import { LuCalendar } from 'react-icons/lu'
import ChoiceChips from '@/components/common/ChoiceChips'
import EmptyState from '@/components/common/EmptyState'
import RecommendationList from '@/components/dashboard/RecommendationList'
import type { BriefingResponse, EventSearch } from '@/lib/api/generated'

export default function EventsSection({
  id,
  briefing,
  eventSearch,
}: {
  id: string
  briefing: BriefingResponse
  eventSearch?: EventSearch
}) {
  const [kind, setKind] = useState('전체')
  const { events, seasonEvents, weather } = briefing
  const categories = [
    '전체',
    ...new Set(
      events
        .map(({ category }) => category)
        .filter((category) => category !== '전체'),
    ),
  ]
  const visible =
    kind === '전체'
      ? events
      : events.filter(({ category }) => category === kind)
  const recommendations = briefing.suggestions.filter(
    (item) =>
      item.category === 'EVENT' &&
      (kind === '전체' ||
        visible.some(({ evidenceId }) =>
          item.evidenceIds.includes(evidenceId),
        )),
  )
  return (
    <Card id="events">
      <section aria-labelledby="events-heading">
        <h2 id="events-heading" className="flex items-center gap-2 text-card">
          주변 예정 행사 <Tag>{events.length}건</Tag>
        </h2>
        {eventSearch && (
          <p className="mt-1 text-caption text-muted">
            향후 {eventSearch.days}일 ·{' '}
            {dayjs(eventSearch.baseDate).format('M월 D일')} 기준
          </p>
        )}
        {events.length > 0 ? (
          <>
            <div className="mt-4">
              <ChoiceChips
                label="행사 종류"
                value={kind}
                onChange={setKind}
                options={categories.map((value) => ({
                  value,
                  label: `${value} ${value === '전체' ? events.length : events.filter(({ category }) => category === value).length}`,
                }))}
              />
            </div>
            <ul className="mt-4 flex flex-col divide-y divide-divider">
              {visible.map((event) => (
                <li
                  key={event.evidenceId}
                  className="flex items-center gap-4 py-3"
                >
                  <div className="flex w-12 shrink-0 flex-col items-center rounded-xl bg-canvas py-2">
                    <span className="text-caption text-muted">
                      {dayjs(event.startDate).format('ddd')}
                    </span>
                    <span className="text-item">
                      {dayjs(event.startDate).format('D')}
                    </span>
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-item">
                      <Tag
                        color={
                          event.status === 'ONGOING' ? 'success' : 'processing'
                        }
                      >
                        {event.status === 'ONGOING' ? '진행 중' : '예정'}
                      </Tag>
                      {event.title}
                    </p>
                    <p className="mt-1 text-caption text-muted">
                      {dayjs(event.startDate).format('M.D(ddd)')} ·{' '}
                      {event.timeText && `${event.timeText} · `}
                      {event.place}
                    </p>
                    <p className="mt-1 text-caption lg:hidden">
                      {event.distanceM}m · 도보 약 {event.walkMinutes}분
                    </p>
                  </div>
                  <div className="text-right max-lg:hidden">
                    <p className="text-item">{event.distanceM}m</p>
                    <p className="text-caption text-muted">
                      도보 약 {event.walkMinutes}분
                    </p>
                  </div>
                </li>
              ))}
            </ul>
          </>
        ) : (
          <div className="mt-4">
            <EmptyState
              icon={LuCalendar}
              title="조건에 맞는 주변 행사가 없어요."
              description="시즌 행사와 날씨 정보도 함께 확인해 보세요."
            />
          </div>
        )}
        {!events.length && seasonEvents.length > 0 && (
          <div className="mt-4 rounded-xl bg-canvas p-4">
            <h3 className="text-body-strong">시즌 행사</h3>
            <ul className="mt-2 grid grid-cols-[minmax(0,1fr)] gap-2">
              {seasonEvents.map((event) => (
                <li key={event.evidenceId} className="text-caption">
                  {event.label} · {dayjs(event.startDate).format('M월 D일')}
                </li>
              ))}
            </ul>
          </div>
        )}
        {!events.length && !seasonEvents.length && weather?.forecasts[0] && (
          <p className="mt-4 rounded-xl bg-canvas p-4 text-caption">
            {dayjs(weather.forecasts[0].date).format('M월 D일')} 예보 ·{' '}
            {weather.forecasts[0].sky}
          </p>
        )}
        {recommendations.length > 0 && (
          <div className="mt-4">
            <RecommendationList
              analysisId={id}
              showFeedbackNote
              tag="행사"
              title="추천 할 일"
              description="브리핑에서 확인한 근거로 골랐어요."
              items={recommendations}
            />
          </div>
        )}
      </section>
    </Card>
  )
}
