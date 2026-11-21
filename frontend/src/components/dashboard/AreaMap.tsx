import { Map, MapMarker, useKakaoLoader } from 'react-kakao-maps-sdk'

// shortcut: 서울숲 예시 좌표는 가게 위경도 API 연결 시 실제 좌표로 교체합니다.
const center = { lat: 37.5444, lng: 127.0374 }
const statusClassName =
  'grid h-40 place-items-center bg-canvas px-4 text-center text-caption text-muted'

function KakaoAreaMap({ appkey }: { appkey: string }) {
  const [loading, error] = useKakaoLoader({
    appkey,
    url: 'https://dapi.kakao.com/v2/maps/sdk.js',
  })

  if (error || loading) {
    return (
      <p role="status" className={statusClassName}>
        {error
          ? '카카오 지도를 불러오지 못했어요. 연결 상태를 확인해 주세요.'
          : '카카오 지도를 불러오고 있어요.'}
      </p>
    )
  }

  return (
    <Map
      aria-label="서울숲 카카오 지도"
      center={center}
      className="h-40 w-full"
      keyboardShortcuts
      level={4}
      scrollwheel={false}
      tabIndex={0}
    >
      <MapMarker position={center} title="서울숲 예시 위치" />
    </Map>
  )
}

export default function AreaMap() {
  const appkey = import.meta.env.VITE_KAKAO_MAP_JS_KEY?.trim()

  return (
    <figure
      aria-labelledby="area-map-caption"
      className="mt-3 overflow-hidden rounded-xl border border-divider"
    >
      {appkey ? (
        <KakaoAreaMap appkey={appkey} />
      ) : (
        <p className={statusClassName}>지도 설정이 필요해요.</p>
      )}
      <figcaption
        id="area-map-caption"
        className="bg-surface px-3 py-1 text-tag text-muted"
      >
        카카오맵 · 서울숲 예시 위치
      </figcaption>
    </figure>
  )
}
