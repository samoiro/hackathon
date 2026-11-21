import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@/lib/api/client'
import type { SupportProgramListResponse } from '@/types/api/supportProgram'

// 서버가 하루 한 번 모아 두고 내주는 마감 임박 공고 (최대 3개)
export const getSupportPrograms =
  async (): Promise<SupportProgramListResponse> => {
    const res = await apiClient.get<SupportProgramListResponse>(
      '/api/support-programs',
    )
    return res.data
  }

export const useGetSupportPrograms = () =>
  useQuery({
    queryKey: ['support-programs'],
    queryFn: getSupportPrograms,
  })
