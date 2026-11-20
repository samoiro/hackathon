/// <reference types="vite/client" />

interface ViteTypeOptions {
  strictImportMetaEnv: unknown
}

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_KAKAO_MAP_JS_KEY?: string
}
