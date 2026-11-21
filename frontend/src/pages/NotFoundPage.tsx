import { Link } from 'react-router'

export default function NotFoundPage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-4 bg-canvas px-6 text-center">
      <p className="text-sm font-semibold text-brand-text">404</p>
      <h1 className="text-card text-fg">페이지를 찾을 수 없습니다</h1>
      <Link className="text-brand-text underline underline-offset-4" to="/">
        홈으로 돌아가기
      </Link>
    </main>
  )
}
