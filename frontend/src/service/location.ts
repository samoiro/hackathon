import { useMutation } from '@tanstack/react-query'
import { apiClient } from '@/lib/api/client'
import type {
  LocationValidateRequest,
  LocationValidateResponse,
} from '@/types/api/location'

// 쓸 수 없는 주소도 200 + valid=false. 요청 형식이 틀리면 400
export const postLocationValidate = async ({
  data,
}: {
  data: LocationValidateRequest
}): Promise<LocationValidateResponse> => {
  const res = await apiClient.post<LocationValidateResponse>(
    '/api/locations/validate',
    data,
  )
  return res.data
}

export const usePostLocationValidate = ({
  onSuccess,
  onError,
}: {
  onSuccess?: (data: LocationValidateResponse) => void | Promise<void>
  onError?: (err: Error) => void
} = {}) => {
  const {
    mutate: postLocationValidateMutate,
    isPending: postLocationValidateIsPending,
  } = useMutation({
    mutationFn: (data: LocationValidateRequest) =>
      postLocationValidate({ data }),
    onSuccess,
    onError,
  })
  return { postLocationValidateMutate, postLocationValidateIsPending }
}
