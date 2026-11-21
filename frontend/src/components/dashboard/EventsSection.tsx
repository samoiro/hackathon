import { Card, Tag } from 'antd'
import dayjs from 'dayjs'
import { useState } from 'react'
import { LuCalendar } from 'react-icons/lu'
import ChoiceChips from '@/components/common/ChoiceChips'
import EmptyState from '@/components/common/EmptyState'
import RecommendationList, {
  FeedbackLegend,
} from '@/components/dashboard/RecommendationList'
import type {
  BriefingResponse,
  EventSearch,
  SeasonEventResponse,
} from '@/lib/api/generated'

const seasonTypes: Record<SeasonEventResponse['type'], string> = {
  HOLIDAY: '명절·공휴일',
  SCHOOL: '학사 일정',
  SEASON: '계절',
  WEATHER: '날씨',
  EVENT: '행사',
}

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
  const { events, weather } = briefing
  const base = dayjs(eventSearch?.baseDate).startOf('day')
  // 이미 끝난 시즌은 빼고, 시작이 가까운 순으로
  const seasonEvents = briefing.seasonEvents
    .filter((event) => !dayjs(event.endDate).isBefore(base, 'day'))
    .sort((a, b) => a.startDate.localeCompare(b.startDate))
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
  const eventSuggestions = briefing.suggestions.filter(
    (item) => item.category === 'EVENT',
  )
  // 추천 할 일은 대표 근거(evidenceIds 첫 번째)인 행사 아래에 묶는다 (피드백 반영안)
  const forEvent = (evidenceId: string) =>
    eventSuggestions.filter((item) => item.evidenceIds[0] === evidenceId)
  // 행사에 묶이지 않은 할 일(시즌 행사·날씨 근거)은 목록 아래에 따로
  const others = eventSuggestions.filter(
    (item) => !events.some((event) => event.evidenceId === item.evidenceIds[0]),
  )
  const groupNote = (event: (typeof events)[number]) =>
    event.status === 'ONGOING'
      ? `${dayjs(event.endDate).format('M.D(ddd)')}까지 진행 중이에요`
      : `${[dayjs(event.startDate).format('M.D(ddd)'), event.timeText?.split('–')[0]].filter(Boolean).join(' ')} 시작`
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
              {visible.map((event) => {
                const items = forEvent(event.evidenceId)
                return (
                  <li key={event.evidenceId} className="py-3">
                    <div className="flex items-center gap-4">
                      <div className="flex w-12 shrink-0 flex-col items-center rounded-xl bg-canvas py-2">
                        <span className="text-caption text-muted">
                          {dayjs(event.startDate).format('ddd')}
                        </span>
                        <span className="text-item">
                          {dayjs(event.startDate).format('D')}
                        </span>
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="flex flex-wrap items-center gap-x-2 text-item">
                          <Tag
                            color={
                              event.status === 'ONGOING'
                                ? 'success'
                                : 'processing'
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
                        <p className="mt-1 text-body-strong lg:hidden">
                          {event.distanceM}m · 도보 약 {event.walkMinutes}분
                        </p>
                      </div>
                      <div className="text-right max-lg:hidden">
                        <p className="text-item">{event.distanceM}m</p>
                        <p className="text-caption text-muted">
                          도보 약 {event.walkMinutes}분
                        </p>
                      </div>
                    </div>
                    {items.length > 0 && (
                      <div className="mt-3">
                        <RecommendationList
                          analysisId={id}
                          tag="행사"
                          title={`${event.title} 준비 할 일`}
                          description={groupNote(event)}
                          items={items}
                          bulkAdd={false}
                        />
                      </div>
                    )}
                  </li>
                )
              })}
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
            <p className="text-caption text-muted">
              주변 행사 대신 준비에 참고할 계절·명절 일정이에요.
            </p>
            <ul className="mt-2 flex flex-col divide-y divide-divider">
              {seasonEvents.map((event) => {
                const start = dayjs(event.startDate)
                const end = dayjs(event.endDate)
                const daysLeft = start.diff(base, 'day')
                return (
                  <li
                    key={event.evidenceId}
                    className="flex items-center gap-4 py-3"
                  >
                    <div className="flex w-12 shrink-0 flex-col items-center rounded-xl bg-surface py-2">
                      <span className="text-caption text-muted">
                        {start.format('M월')}
                      </span>
                      <span className="text-item">{start.format('D')}</span>
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="flex flex-wrap items-center gap-x-2 text-item">
                        <Tag color={daysLeft > 0 ? 'processing' : 'success'}>
                          {daysLeft > 0 ? `D-${daysLeft}` : '진행 중'}
                        </Tag>
                        {event.label}
                      </p>
                      <p className="mt-1 text-caption text-muted">
                        {seasonTypes[event.type]} · {start.format('M.D(ddd)')}
                        {!end.isSame(start, 'day') &&
                          ` ~ ${end.format('M.D(ddd)')}`}
                      </p>
                    </div>
                  </li>
                )
              })}
            </ul>
          </div>
        )}
        {!events.length && !seasonEvents.length && weather?.forecasts[0] && (
          <p className="mt-4 rounded-xl bg-canvas p-4 text-caption">
            {dayjs(weather.forecasts[0].date).format('M월 D일')} 예보 ·{' '}
            {weather.forecasts[0].sky}
          </p>
        )}
        {others.length > 0 && (
          <div className="mt-4">
            <RecommendationList
              analysisId={id}
              tag="행사"
              title="추천 할 일"
              description="시즌 행사와 날씨에서 확인한 준비 항목이에요."
              items={others}
            />
          </div>
        )}
        {eventSuggestions.length > 0 && <FeedbackLegend />}
      </section>
    </Card>
  )
}
