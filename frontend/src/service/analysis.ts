import { useMutation, useQuery } from '@tanstack/react-query'
import { apiClient } from '@/lib/api/client'
import type {
  AnalysisCreatedResponse,
  AnalysisRequest,
  AnalysisResponse,
  SuggestionFeedbackRequest,
} from '@/types/api/analysis'

const POLL_MS = 1500

// 약 1분 걸려서 번호만 바로 돌려준다 (202). 서울 밖 422, 품목 개수 초과 400
export const postAnalysis = async ({
  data,
}: {
  data: AnalysisRequest
}): Promise<AnalysisCreatedResponse> => {
  const res = await apiClient.post<AnalysisCreatedResponse>(
    '/api/analysis',
    data,
  )
  return res.data
}

export const getAnalysis = async (id: string): Promise<AnalysisResponse> => {
  const res = await apiClient.get<AnalysisResponse>(`/api/analysis/${id}`)
  return res.data
}

// 204 No Content
export const postSuggestionFeedback = async ({
  analysisId,
  data,
}: {
  analysisId: string
  data: SuggestionFeedbackRequest
}): Promise<void> => {
  await apiClient.post(`/api/analysis/${analysisId}/feedback`, data)
}

export const usePostAnalysis = ({
  onSuccess,
  onError,
}: {
  onSuccess?: (data: AnalysisCreatedResponse) => void | Promise<void>
  onError?: (err: Error) => void
} = {}) => {
  const { mutate: postAnalysisMutate, isPending: postAnalysisIsPending } =
    useMutation({
      mutationFn: (data: AnalysisRequest) => postAnalysis({ data }),
      onSuccess,
      onError,
    })
  return { postAnalysisMutate, postAnalysisIsPending }
}

// 분석 중에는 1.5초마다 다시 묻고, SUCCEEDED·FAILED가 되면 멈춘다
export const useGetAnalysis = (id: string | null) =>
  useQuery({
    queryKey: ['analysis', id],
    queryFn: () => getAnalysis(id ?? ''),
    enabled: id !== null,
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status === 'SUCCEEDED' || status === 'FAILED' ? false : POLL_MS
    },
  })

export const usePostSuggestionFeedback = ({
  onSuccess,
  onError,
}: {
  onSuccess?: () => void | Promise<void>
  onError?: (err: Error) => void
} = {}) => {
  const {
    mutate: postSuggestionFeedbackMutate,
    isPending: postSuggestionFeedbackIsPending,
  } = useMutation({
    mutationFn: (variables: {
      analysisId: string
      data: SuggestionFeedbackRequest
    }) => postSuggestionFeedback(variables),
    onSuccess,
    onError,
  })
  return { postSuggestionFeedbackMutate, postSuggestionFeedbackIsPending }
}
