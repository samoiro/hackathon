import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@/lib/api/client'
import type {
  IndustryResponse,
  RecommendedItemsResponse,
} from '@/types/api/industry'

export const getIndustries = async (): Promise<IndustryResponse[]> => {
  const res = await apiClient.get<IndustryResponse[]>('/api/industries')
  return res.data
}

export const getRecommendedItems = async (
  industryId: number,
): Promise<RecommendedItemsResponse> => {
  const res = await apiClient.get<RecommendedItemsResponse>(
    `/api/industries/${industryId}/recommended-items`,
  )
  return res.data
}

// 업종 목록은 거의 바뀌지 않아 한 번 받으면 다시 묻지 않는다
export const useGetIndustries = () =>
  useQuery({
    queryKey: ['industries'],
    queryFn: getIndustries,
    staleTime: Infinity,
  })

export const useGetRecommendedItems = (industryId: number | null) =>
  useQuery({
    queryKey: ['industries', industryId, 'recommended-items'],
    queryFn: () => getRecommendedItems(industryId ?? 0),
    enabled: industryId !== null,
  })
