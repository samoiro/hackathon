import { useId } from 'react'
import {
  Area,
  AreaChart,
  CartesianGrid,
  LabelList,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { usePrefersReducedMotion } from '@/hooks/usePrefersReducedMotion'

type TrendPoint = { week: string; price: number | null }

const won = (value: unknown) =>
  typeof value === 'number' ? `${value.toLocaleString()}원` : '자료 없음'

// 4주 가격 추이. 점마다 값을 적어 두고(리뷰 F-08), 마우스·키보드(화살표)로 고르면 말풍선이 뜬다.
// 화면 낭독기에는 같은 값을 목록으로 읽어 준다
// color: 상승·하락 색(CSS 변수). 선·점·아래 그라디언트가 모두 이 색을 따른다
export default function PriceTrendChart({
  data,
  color,
}: {
  data: TrendPoint[]
  color: string
}) {
  // useId 값의 ':'는 url(#…) 참조를 깨뜨리므로 뺀다
  const gradientId = useId().replaceAll(':', '')
  const reduceMotion = usePrefersReducedMotion()
  const lastIndex = data.length - 1

  return (
    <figure className="mt-2">
      <div aria-hidden className="h-40">
        <ResponsiveContainer>
          {/* 낭독기용 목록(figcaption)이 따로 있으니 차트 자체는 탭 순서에서 뺀다 */}
          <AreaChart
            accessibilityLayer={false}
            data={data}
            margin={{ top: 24, right: 28, left: 28, bottom: 0 }}
          >
            <defs>
              <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={color} stopOpacity={0.25} />
                <stop offset="100%" stopColor={color} stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid
              vertical={false}
              strokeDasharray="4 4"
              stroke="var(--color-divider)"
            />
            <XAxis
              dataKey="week"
              tickLine={false}
              axisLine={false}
              tick={{ fontSize: 12, fill: 'var(--color-muted)' }}
            />
            <YAxis hide domain={['dataMin - 250', 'dataMax + 120']} />
            <Tooltip
              cursor={{ stroke: 'var(--color-line-strong)' }}
              formatter={(value) => [won(value), '가격']}
              contentStyle={{
                borderRadius: 12,
                border: '1px solid var(--color-divider)',
                background: 'var(--color-surface)',
                color: 'var(--color-fg)',
              }}
            />
            <Area
              type="monotone"
              dataKey="price"
              stroke={color}
              strokeWidth={2.5}
              fill={`url(#${gradientId})`}
              isAnimationActive={!reduceMotion}
              animationDuration={900}
              dot={({ cx, cy, index }) => (
                <circle
                  key={index}
                  cx={cx}
                  cy={cy}
                  r={index === lastIndex ? 6 : 4}
                  fill={index === lastIndex ? color : 'var(--color-surface)'}
                  stroke={color}
                  strokeWidth={2}
                />
              )}
              activeDot={{
                r: 7,
                stroke: 'var(--color-surface)',
                strokeWidth: 2,
              }}
            >
              <LabelList
                dataKey="price"
                position="top"
                offset={10}
                fontSize={12}
                fill="var(--color-fg)"
                formatter={won}
              />
            </Area>
          </AreaChart>
        </ResponsiveContainer>
      </div>
      <figcaption className="sr-only">
        4주 가격 추이:{' '}
        {data.map((point) => `${point.week} ${won(point.price)}`).join(', ')}
      </figcaption>
    </figure>
  )
}
