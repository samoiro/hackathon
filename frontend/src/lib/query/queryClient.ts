import { QueryClient } from '@tanstack/react-query'

export const queryClient = new QueryClient()

queryClient.setQueryDefaults(['onboarding-draft'], { gcTime: Infinity })
queryClient.setQueryDefaults(['last-briefing'], { gcTime: Infinity })
queryClient.setQueryDefaults(['analysis-input'], { gcTime: 60 * 60 * 1000 })
