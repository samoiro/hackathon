import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  create,
  feedback,
  get,
  type AnalysisRequest,
  type SuggestionFeedbackRequest,
} from '@/lib/api/generated'

type FeedbackVote = SuggestionFeedbackRequest['vote']

export function useAnalysis(id: string, poll = false) {
  return useQuery({
    queryKey: ['analysis', id],
    enabled: Boolean(id),
    queryFn: async ({ signal }) =>
      (await get({ path: { id }, signal, throwOnError: true })).data,
    refetchInterval: poll
      ? ({ state }) =>
          state.status === 'error' ||
          state.data?.status === 'SUCCEEDED' ||
          state.data?.status === 'FAILED'
            ? false
            : 1000
      : false,
    retry: false,
  })
}

export function useStartAnalysis() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: AnalysisRequest) =>
      (await create({ body, throwOnError: true })).data,
    onSuccess: ({ analysisId }, request) => {
      queryClient.setQueryData(['analysis-input', analysisId], request)
    },
  })
}

export function readExcludedCards(): string[] {
  try {
    const value: unknown = JSON.parse(
      localStorage.getItem('samoiro:excluded-cards') ?? '[]',
    )
    return Array.isArray(value)
      ? value.filter((item): item is string => typeof item === 'string')
      : []
  } catch {
    return []
  }
}

export function saveExcludedCard(key: string, excluded: boolean) {
  const cards = new Set(readExcludedCards())
  if (excluded) cards.add(key)
  else cards.delete(key)
  try {
    localStorage.setItem('samoiro:excluded-cards', JSON.stringify([...cards]))
  } catch {
    /* 저장소를 사용할 수 없으면 현재 분석에서만 적용한다. */
  }
}

// 추천 카드 피드백(도움돼요·해당 없어요). 해당 없어요는 다음 분석에서 빼도록 이 기기에 카드 키를 남긴다
export function useSuggestionFeedback(
  analysisId: string,
  cardKey: string,
  {
    onSuccess,
    onError,
  }: {
    onSuccess?: (vote: FeedbackVote) => void
    onError?: (err: Error) => void
  } = {},
) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (vote: FeedbackVote) => {
      await feedback({
        path: { id: analysisId },
        body: { cardKey, vote },
        throwOnError: true,
      })
      saveExcludedCard(cardKey, vote === 'NOT_RELEVANT')
    },
    onSuccess: async (_, vote) => {
      onSuccess?.(vote)
      await queryClient.invalidateQueries({
        queryKey: ['analysis', analysisId],
      })
    },
    onError,
  })
}
