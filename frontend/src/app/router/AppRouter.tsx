import { Route, Routes } from 'react-router'
import BriefingPage from '@/pages/BriefingPage'
import DashboardPage from '@/pages/DashboardPage'
import NotFoundPage from '@/pages/NotFoundPage'
import OnboardingPage from '@/pages/OnboardingPage'

export default function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<OnboardingPage />} />
      <Route path="/briefing" element={<BriefingPage />} />
      <Route path="/dashboard" element={<DashboardPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
