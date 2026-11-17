import { Tag } from 'antd'

export default function HomePage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-canvas px-6 py-12">
      <section className="w-full max-w-xl rounded-2xl border border-divider bg-surface p-8">
        <p className="mb-3 text-sm font-bold text-brand-text">사모이로</p>
        <h1 className="mb-4 text-3xl font-bold tracking-tight text-fg">
          사장님의 1분 경영회의
        </h1>
        <p className="mb-6 leading-7 text-muted">
          프론트엔드 개발 환경이 준비되었습니다.
        </p>
        <Tag color="success">초기 설정 완료</Tag>
      </section>
    </main>
  )
}
