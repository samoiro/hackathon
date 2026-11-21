import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@/lib/api/client'
import type { AreaResponse, CityDataResponse } from '@/types/api/area'

export const getAreas = async (): Promise<AreaResponse[]> => {
  const res = await apiClient.get<AreaResponse[]>('/api/areas')
  return res.data
}

// area는 AreaResponse.name을 그대로 넘긴다 (예: 서울숲공원)
export const getCityData = async (area: string): Promise<CityDataResponse> => {
  const res = await apiClient.get<CityDataResponse>(
    `/api/seoul/city-data/${encodeURIComponent(area)}`,
  )
  return res.data
}

// 장소 목록은 거의 바뀌지 않아 한 번 받으면 다시 묻지 않는다
export const useGetAreas = () =>
  useQuery({
    queryKey: ['areas'],
    queryFn: getAreas,
    staleTime: Infinity,
  })

export const useGetCityData = (area: string | null) =>
  useQuery({
    queryKey: ['city-data', area],
    queryFn: () => getCityData(area ?? ''),
    enabled: area !== null,
  })
