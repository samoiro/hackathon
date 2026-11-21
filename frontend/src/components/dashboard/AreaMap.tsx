import {
  Map as KakaoMap,
  MapMarker,
  useKakaoLoader,
} from 'react-kakao-maps-sdk'
import { kakaoOptions } from '@/lib/kakao/maps'
import type { Address } from '@/types/search'
import type { AreaResponse } from '@/lib/api/generated'

const statusClassName =
  'grid h-40 place-items-center bg-canvas px-4 text-center text-caption text-muted'

type MapEvent = { title: string; latitude: number; longitude: number }
type MapProps = {
  address?: Address
  area?: AreaResponse
  events: MapEvent[]
}

function KakaoAreaMap({
  options,
  address,
  center,
  events,
}: Pick<MapProps, 'address' | 'events'> & {
  options: NonNullable<typeof kakaoOptions>
  center: { lat: number; lng: number }
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

  return (
    <KakaoMap
      aria-label="지역 현황 카카오 지도"
      center={center}
      className="h-40 w-full"
      keyboardShortcuts
      level={5}
      scrollwheel={false}
      tabIndex={0}
    >
      {address && (
        <MapMarker
          position={{ lat: address.latitude, lng: address.longitude }}
          title="우리 가게"
        />
      )}
      {events.map((event) => (
        <MapMarker
          key={`${event.title}:${event.latitude}:${event.longitude}`}
          title={event.title}
          position={{ lat: event.latitude, lng: event.longitude }}
        />
      ))}
    </KakaoMap>
  )
}

export default function AreaMap({ address, area, events }: MapProps) {
  const point = area ?? address
  if (!point) return null
  const uniqueEvents = new Map(
    events.map((event) => [
      `${event.title}:${event.latitude}:${event.longitude}`,
      event,
    ]),
  )
  return (
    <figure
      aria-labelledby="area-map-caption"
      className="mt-3 overflow-hidden rounded-xl border border-divider"
    >
      {kakaoOptions ? (
        <KakaoAreaMap
          options={kakaoOptions}
          address={address}
          center={{ lat: point.latitude, lng: point.longitude }}
          events={[...uniqueEvents.values()]}
        />
      ) : (
        <p className={statusClassName}>지도 설정이 필요해요.</p>
      )}
      <figcaption
        id="area-map-caption"
        className="bg-surface px-3 py-1 text-tag text-muted"
      >
        카카오맵 · {area?.name ?? address?.road}
      </figcaption>
    </figure>
  )
}
