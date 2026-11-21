import { Map, MapMarker, useKakaoLoader } from 'react-kakao-maps-sdk'
import { kakaoOptions } from '@/lib/kakao/maps'
import type { Address } from '@/types/search'
import type { EventResponse } from '@/lib/api/generated'

const statusClassName =
  'grid h-40 place-items-center bg-canvas px-4 text-center text-caption text-muted'

function KakaoAreaMap({
  options,
  address,
  events,
}: {
  options: NonNullable<typeof kakaoOptions>
  address: Address
  events: EventResponse[]
}) {
  const [loading, error] = useKakaoLoader(options)
  if (error || loading)
    return (
      <p role="status" className={statusClassName}>
        {error
          ? '카카오 지도를 불러오지 못했어요.'
          : '카카오 지도를 불러오고 있어요.'}
      </p>
    )

  const center = { lat: address.latitude, lng: address.longitude }
  return (
    <Map
      aria-label="가게 주변 카카오 지도"
      center={center}
      className="h-40 w-full"
      keyboardShortcuts
      level={5}
      scrollwheel={false}
      tabIndex={0}
    >
      <MapMarker position={center} title="우리 가게" />
      {events.map((event) => (
        <MapMarker
          key={event.evidenceId}
          title={event.title}
          position={{ lat: event.latitude, lng: event.longitude }}
        />
      ))}
    </Map>
  )
}

export default function AreaMap({
  address,
  events,
}: {
  address: Address
  events: EventResponse[]
}) {
  return (
    <figure
      aria-labelledby="area-map-caption"
      className="mt-3 overflow-hidden rounded-xl border border-divider"
    >
      {kakaoOptions ? (
        <KakaoAreaMap
          options={kakaoOptions}
          address={address}
          events={events}
        />
      ) : (
        <p className={statusClassName}>지도 설정이 필요해요.</p>
      )}
      <figcaption
        id="area-map-caption"
        className="bg-surface px-3 py-1 text-tag text-muted"
      >
        카카오맵 · {address.road}
      </figcaption>
    </figure>
  )
}
