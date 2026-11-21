import { Alert, Card } from 'antd'
import { LuClock, LuExternalLink } from 'react-icons/lu'

const SOURCES = [
  {
    title: '서울시 문화행사 정보',
    detail: '서울 열린데이터광장 · 매일 갱신',
    link: true,
  },
  {
    title: '농수축산물 가격',
    detail: '한국농수산식품유통공사 · 기준일 11.16',
  },
  {
    title: '서울 실시간 도시데이터',
    detail: '서울 열린데이터광장 · 5분 단위 갱신',
  },
]

export default function EvidenceSection() {
  return (
    <Card>
      <section aria-labelledby="evidence-heading">
        <h2 id="evidence-heading" className="text-card">
          어떤 근거를 확인했나요?
        </h2>
        <p className="mt-1 text-caption text-muted">
          출처와 확인 시각을 함께 보여드려요.
        </p>
        <ul className="mt-4 grid grid-cols-3 gap-3">
          {SOURCES.map((source) => (
            <li
              key={source.title}
              className="rounded-xl border border-divider p-4"
            >
              <p className="text-body-strong">{source.title}</p>
              <p className="mt-1 text-caption text-muted">{source.detail}</p>
              <p className="mt-2 flex items-center gap-2 text-caption text-muted">
                <LuClock aria-hidden /> 확인 11.17 07:00
              </p>
              {source.link && (
                <a
                  href="/"
                  className="mt-3 inline-flex items-center gap-1 text-caption font-semibold underline"
                >
                  원문 보기 <LuExternalLink aria-hidden />
                </a>
              )}
            </li>
          ))}
        </ul>
        <Alert
          className="mt-4"
          type="warning"
          showIcon
          title="일정은 주최 측 사정으로 바뀔 수 있어요. 방문객이나 매출을 예측한 값이 아니에요."
        />
      </section>
    </Card>
  )
}
