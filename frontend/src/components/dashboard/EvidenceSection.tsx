import { Alert, Card } from 'antd'
import dayjs from 'dayjs'
import { LuClock } from 'react-icons/lu'
import type { SourceResponse } from '@/lib/api/generated'

export default function EvidenceSection({
  sources,
}: {
  sources: SourceResponse[]
}) {
  return (
    <Card>
      <section aria-labelledby="evidence-heading">
        <h2 id="evidence-heading" className="text-card">
          어떤 근거를 확인했나요?
        </h2>
        <p className="mt-1 text-caption text-muted">
          출처와 확인 시각을 함께 보여드려요.
        </p>
        {sources.length === 0 && (
          <p className="mt-4 text-caption text-muted">
            표시할 근거 자료가 없어요.
          </p>
        )}
        <ul className="mt-4 grid gap-3 lg:grid-cols-3">
          {sources.map((source) => (
            <li
              key={`${source.provider}:${source.name}`}
              className="rounded-xl border border-divider p-4"
            >
              <p className="text-body-strong">{source.name}</p>
              <p className="mt-1 text-caption text-muted">
                {source.provider} · {source.updateCycle}
                {source.baseDate &&
                  ` · 기준 ${dayjs(source.baseDate).format('M.D')}`}
              </p>
              <p className="mt-2 flex items-center gap-2 text-caption text-muted">
                <LuClock aria-hidden /> 확인{' '}
                {dayjs(source.checkedAt).format('M.D HH:mm')}
              </p>
            </li>
          ))}
        </ul>
        <Alert
          className="mt-4"
          type="warning"
          showIcon
          title="일정은 주최 측 사정으로 바뀔 수 있어요."
        />
      </section>
    </Card>
  )
}
