import { Button, Card, Tag } from 'antd'
import {
  LuChevronUp,
  LuInfo,
  LuMapPin,
  LuRefreshCw,
  LuSun,
} from 'react-icons/lu'
import SearchSelect from '@/components/common/SearchSelect'
import AreaMap from '@/components/dashboard/AreaMap'
import { searchAreas } from '@/mocks/search'
import WaveProgress from '@/components/common/WaveProgress'

const FORECAST = [
  {
    time: '12:00',
    level: '보통',
    color: 'processing',
    range: '32,000~34,000명',
  },
  {
    time: '18:00',
    level: '약간 붐빔',
    color: 'warning',
    range: '38,000~40,000명',
  },
  { time: '21:00', level: '여유', color: 'success', range: '24,000~26,000명' },
]

const CROWD_LEVELS = [
  { label: '여유', color: 'success', current: true },
  { label: '보통', color: 'processing' },
  { label: '약간 붐빔', color: 'warning' },
  { label: '붐빔', color: 'default' },
]

const AGES = [
  ['10대', 4],
  ['20대', 31],
  ['30대', 34],
  ['40대', 17],
  ['50대', 9],
  ['60대', 5],
] as const

export default function AreaStatus() {
  return (
    <Card id="area">
      <section aria-labelledby="area-heading">
        <div className="flex items-center justify-between">
          <h2 id="area-heading" className="text-card">
            지금 이 지역은 어떤가요?
          </h2>
          <Button
            size="small"
            aria-label="새로고침"
            icon={<LuRefreshCw aria-hidden />}
          />
        </div>
        <SearchSelect
          queryKey="area"
          fetcher={searchAreas}
          className="mt-4 w-full"
          aria-labelledby="area-heading"
          defaultValue="서울숲공원"
          prefix={<LuMapPin aria-hidden />}
        />
        <p className="mt-2 text-caption text-muted">
          가게가 포함된 집계 영역 기준이에요.
        </p>

        <div className="mt-4 flex items-center justify-between rounded-xl bg-canvas p-4 text-caption">
          <span className="flex items-center gap-2 font-semibold">
            <LuSun aria-hidden /> 맑음 · 8°C
          </span>
          <span className="text-caption text-muted">강수 확률 10%</span>
        </div>
        <AreaMap />

        <div className="mt-4 flex justify-between text-caption">
          <h3 className="text-body-strong">현재 인구·혼잡도</h3>
          <span className="text-muted">11.17 07:00 기준</span>
        </div>
        <p className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-2">
          <span className="rounded-xl bg-success-bg px-3 py-1 text-item text-success">
            ● 여유
          </span>
          <span className="text-item">28,000~30,000명</span>
        </p>
        {/* 혼잡도 4단계 범례. 지금 단계는 굵게 + '지금' 글자로 함께 알린다 */}
        <ul aria-label="혼잡도 단계" className="mt-3 flex gap-2">
          {CROWD_LEVELS.map((level) => (
            <li key={level.label}>
              <Tag
                color={level.color}
                className={level.current ? 'font-bold' : ''}
              >
                {level.label}
                {level.current && <span className="sr-only"> (지금)</span>}
              </Tag>
            </li>
          ))}
        </ul>

        <h3 className="mt-4 border-t border-divider pt-4 text-body-strong">
          이후 예상 혼잡도
        </h3>
        <dl className="mt-2 flex flex-col gap-2 text-caption">
          {FORECAST.map((row) => (
            <div key={row.time} className="flex items-center gap-3">
              <dt className="w-12 font-semibold">{row.time}</dt>
              <dd className="flex flex-1 justify-between">
                <Tag color={row.color}>{row.level}</Tag>
                <span className="text-muted">{row.range}</span>
              </dd>
            </div>
          ))}
        </dl>

        <div className="mt-4 flex items-center justify-between border-t border-divider pt-4">
          <h3 className="text-body-strong">연령별 소비 비율</h3>
          <LuChevronUp aria-hidden />
        </div>
        <dl className="mt-2 flex flex-col gap-1 text-caption">
          {AGES.map(([age, percent]) => (
            <div key={age} className="flex items-center gap-3">
              <dt className="w-8 text-muted">{age}</dt>
              <dd className="flex flex-1 items-center gap-3">
                <WaveProgress
                  percent={percent}
                  showInfo={false}
                  strokeColor="var(--color-muted)"
                  className="m-0 flex-1"
                />
                <span className="w-8 text-right font-semibold">{percent}%</span>
              </dd>
            </div>
          ))}
        </dl>
        <p className="mt-4 flex gap-2 rounded-xl bg-canvas p-4 text-caption text-muted">
          <LuInfo aria-hidden className="mt-0.5 shrink-0" />이 집계 영역
          사람들의 연령 구성이에요. 우리 가게 고객 비율이 아니고, 혼잡도가 곧
          매출 증가를 뜻하지도 않아요.
        </p>
      </section>
    </Card>
  )
}
