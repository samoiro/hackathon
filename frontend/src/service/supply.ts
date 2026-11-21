import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { apiClient } from '@/lib/api/client'
import type { SupplyResponse } from '@/types/api/supply'

// 부자재 검색 (최대 20개)
export const getSupplies = async (query: string): Promise<SupplyResponse[]> => {
  const res = await apiClient.get<SupplyResponse[]>('/api/supplies', {
    params: { query },
  })
  return res.data
}

// query는 화면에서 useDebouncedValue로 늦춘 값을 넘긴다
export const useGetSupplies = (query: string) =>
  useQuery({
    queryKey: ['supplies', query],
    queryFn: () => getSupplies(query),
    placeholderData: keepPreviousData,
  })
