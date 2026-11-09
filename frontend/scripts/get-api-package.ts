// 서버 OpenAPI 문서(/v3/api-docs)로 src/lib/api/generated 에 DTO 타입과 API 함수를 생성합니다.
// 사용: tsx scripts/get-api-package.ts <development|production>
// VITE_API_BASE_URL 은 Vite와 같은 규칙으로 읽습니다 (.env, .env.local, .env.<mode>, 환경변수 순으로 우선).
import { rm } from 'node:fs/promises'
import { createClient } from '@hey-api/openapi-ts'
import { loadEnv } from 'vite'

const mode = process.argv[2] ?? 'development'
const baseUrl = loadEnv(mode, process.cwd(), '').VITE_API_BASE_URL
if (!baseUrl) {
  throw new Error(
    `VITE_API_BASE_URL 이 없습니다. .env.local 또는 환경변수에 설정하세요. (mode: ${mode})`,
  )
}

const openApiUrl = `${baseUrl.replace(/\/$/, '')}/v3/api-docs`
console.log('OPENAPI_URL:', openApiUrl)

const response = await fetch(openApiUrl).catch((error: unknown) => {
  throw new Error(
    `서버에 연결할 수 없습니다. 서버를 먼저 실행하세요. (${openApiUrl}: ${String(error)})`,
  )
})
if (!response.ok) {
  throw new Error(
    `OpenAPI 문서를 가져오지 못했습니다: ${response.status} ${response.statusText}`,
  )
}

const output = './src/lib/api/generated'
await rm(output, { recursive: true, force: true })
await createClient({
  input: await response.json(),
  output,
  plugins: [
    // 생성된 함수가 src/lib/api/client.ts 의 axios 인스턴스를 쓰도록 연결
    {
      name: '@hey-api/client-axios',
      runtimeConfigPath: './src/lib/api/heyApiConfig.ts',
    },
    '@hey-api/typescript',
    '@hey-api/sdk',
  ],
})
