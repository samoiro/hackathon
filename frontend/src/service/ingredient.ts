import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { apiClient } from '@/lib/api/client'
import type { IngredientResponse } from '@/types/api/ingredient'

// 원자재 검색 (최대 20개)
export const getIngredients = async (
  query: string,
): Promise<IngredientResponse[]> => {
  const res = await apiClient.get<IngredientResponse[]>('/api/ingredients', {
    params: { query },
  })
  return res.data
}

// query는 화면에서 useDebouncedValue로 늦춘 값을 넘긴다
export const useGetIngredients = (query: string) =>
  useQuery({
    queryKey: ['ingredients', query],
    queryFn: () => getIngredients(query),
    placeholderData: keepPreviousData,
  })
