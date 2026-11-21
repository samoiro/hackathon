import {
  Circle,
  CustomOverlayMap,
  Map as KakaoMap,
  MapMarker,
  useKakaoLoader,
} from 'react-kakao-maps-sdk'
import { LuStore } from 'react-icons/lu'
import { nearbyEventRadiusKm } from '@/lib/api/analysis'
import { kakaoOptions } from '@/lib/kakao/maps'
import type { Address } from '@/types/search'
import type { AreaResponse } from '@/lib/api/generated'

const mapHeight = 'h-72'
const statusClassName = `grid ${mapHeight} place-items-center bg-canvas px-4 text-center text-caption text-muted`

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
      aria-label={
        address ? '우리 가게 주변 카카오 지도' : '지역 현황 카카오 지도'
      }
      center={center}
      className={`${mapHeight} w-full`}
      keyboardShortcuts
      level={address ? 7 : 5}
      scrollwheel={false}
      tabIndex={0}
    >
      {address && (
        <>
          <Circle
            center={center}
            radius={nearbyEventRadiusKm * 1000}
            strokeWeight={3}
            strokeColor="#64594b"
            strokeOpacity={1}
            strokeStyle="dash"
            fillColor="#ffe8b8"
            fillOpacity={0.16}
          />
          <CustomOverlayMap
            position={center}
            xAnchor={0.5}
            yAnchor={0.5}
            zIndex={2}
          >
            <span className="pointer-events-none flex items-center gap-2 rounded-full bg-fg px-3 py-2 text-caption font-semibold whitespace-nowrap text-surface shadow-sm">
              <LuStore aria-hidden className="size-4" />
              우리 가게
            </span>
          </CustomOverlayMap>
        </>
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
  const point = address ?? area
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
        카카오맵 · {address?.road ?? area?.name}
        {address && ` · 가게 기준 반경 ${nearbyEventRadiusKm}km`}
      </figcaption>
    </figure>
  )
}
