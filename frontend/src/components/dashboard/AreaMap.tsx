import {
  CustomOverlayMap,
  Map as KakaoMap,
  Polygon,
  useKakaoLoader,
} from 'react-kakao-maps-sdk'
import { Tooltip } from 'antd'
import { LuCalendar, LuStore } from 'react-icons/lu'
import { kakaoOptions } from '@/lib/kakao/maps'
import type { Address } from '@/types/search'
import type { AreaResponse } from '@/lib/api/generated'

const mapHeight = 'h-72'
const statusClassName = `grid ${mapHeight} place-items-center bg-canvas px-4 text-center text-caption text-muted`

type LatLng = { lat: number; lng: number }

// 장소 영역 경계(GeoJSON MultiPolygon, [경도, 위도]) → 카카오 폴리곤 경로(폴리곤마다 [바깥 고리, 구멍...]).
// 생성 타입은 string이지만 서버는 객체로 보내서 둘 다 받는다. 모양이 다르면 그리지 않는다
function toPolygons(boundary?: unknown): LatLng[][][] {
  const geo: unknown =
    typeof boundary === 'string' ? JSON.parse(boundary) : boundary
  if (
    !geo ||
    typeof geo !== 'object' ||
    !('coordinates' in geo) ||
    !Array.isArray(geo.coordinates)
  )
    return []
  return geo.coordinates.map((polygon: number[][][]) =>
    polygon.map((ring) => ring.map(([lng, lat]) => ({ lat, lng }))),
  )
}

// 점이 고리 안에 있는지 (반직선 교차 수). 폴리곤은 바깥 고리 안이고 구멍 밖일 때 안이다
const inRing = ({ lat, lng }: LatLng, ring: LatLng[]) =>
  ring.reduce((inside, a, index) => {
    const b = ring[(index + ring.length - 1) % ring.length]
    return a.lat > lat !== b.lat > lat &&
      lng < ((b.lng - a.lng) * (lat - a.lat)) / (b.lat - a.lat) + a.lng
      ? !inside
      : inside
  }, false)
const inPolygons = (point: LatLng, polygons: LatLng[][][]) =>
  polygons.some(
    ([outer, ...holes]) =>
      inRing(point, outer) && !holes.some((hole) => inRing(point, hole)),
  )

const groupByPlace = (events: MapEvent[]) =>
  events.reduce((groups, event) => {
    const key = `${event.latitude}:${event.longitude}`
    return groups.set(key, [...(groups.get(key) ?? []), event])
  }, new Map<string, MapEvent[]>())

type MapEvent = { title: string; latitude: number; longitude: number }
type MapProps = {
  address?: Address
  area?: AreaResponse
  events: MapEvent[]
}

function KakaoAreaMap({
  options,
  address,
  area,
  center,
  events,
}: MapProps & {
  options: NonNullable<typeof kakaoOptions>
  center: { lat: number; lng: number }
}) {
  const [loading, error] = useKakaoLoader(options)
  const polygons = toPolygons(area?.boundary)
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
      {polygons.map((path, index) => (
        <Polygon
          key={index}
          path={path}
          strokeWeight={2}
          strokeColor="#d47d00"
          strokeOpacity={1}
          fillColor="#ffe8b8"
          fillOpacity={0.45}
        />
      ))}
      {address && (
        <>
          <CustomOverlayMap
            position={center}
            xAnchor={0.5}
            yAnchor={0.5}
            zIndex={2}
          >
            <span className="pointer-events-none flex select-none items-center gap-2 rounded-full bg-fg px-3 py-2 text-caption font-semibold whitespace-nowrap text-surface shadow-sm">
              <LuStore aria-hidden className="size-4" />
              우리 가게
            </span>
          </CustomOverlayMap>
        </>
      )}
      {/* 같은 좌표의 행사는 한 이름표로 묶는다(광화문·시청처럼 한 점에 여럿). 이름이 길어 이름표엔 건수만, 이름은 툴팁으로 */}
      {[...groupByPlace(events)].map(([key, group]) => (
        <CustomOverlayMap
          key={key}
          position={{ lat: group[0].latitude, lng: group[0].longitude }}
          yAnchor={1}
          zIndex={1}
        >
          <Tooltip
            trigger={['hover', 'focus', 'click']}
            title={
              <ul className="flex flex-col gap-1">
                {group.map((event) => (
                  <li key={event.title}>{event.title}</li>
                ))}
              </ul>
            }
          >
            <button
              type="button"
              aria-label={`행사 ${group.length}건: ${group.map((event) => event.title).join(', ')}`}
              className="flex items-center gap-1 rounded-full border-2 border-primary-700 bg-surface px-2 py-1 text-tag font-semibold whitespace-nowrap text-fg shadow-sm select-none"
            >
              <LuCalendar aria-hidden />
              행사 {group.length}
            </button>
          </Tooltip>
        </CustomOverlayMap>
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
  // 가게 핀 말고는 집계 영역 안의 행사만 핀으로 찍는다
  const polygons = toPolygons(area?.boundary)
  const pins = [...uniqueEvents.values()].filter((event) =>
    inPolygons({ lat: event.latitude, lng: event.longitude }, polygons),
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
          area={area}
          center={{ lat: point.latitude, lng: point.longitude }}
          events={pins}
        />
      ) : (
        <p className={statusClassName}>지도 설정이 필요해요.</p>
      )}
      <figcaption
        id="area-map-caption"
        className="bg-surface px-3 py-1 text-tag text-muted"
      >
        카카오맵 · {address?.road ?? area?.name}
        {address && area && ` · 칠한 곳은 ${area.name} 집계 영역`}
      </figcaption>
    </figure>
  )
}
