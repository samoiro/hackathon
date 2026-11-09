import type { CreateClientConfig } from './generated/client.gen'
import { apiClient } from './client'

// hey-api 생성 함수들이 공통 axios 인스턴스(인터셉터·타임아웃 포함)로 요청하도록 설정
export const createClientConfig: CreateClientConfig = (config) => ({
  ...config,
  axios: apiClient,
  baseURL: import.meta.env.VITE_API_BASE_URL,
})
