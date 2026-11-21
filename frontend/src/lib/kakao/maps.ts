import { Loader } from 'react-kakao-maps-sdk'
import type { Address } from '@/types/search'

const appkey = import.meta.env.VITE_KAKAO_MAP_JS_KEY?.trim()
export const kakaoOptions = appkey
  ? {
      appkey,
      url: 'https://dapi.kakao.com/v2/maps/sdk.js',
      libraries: ['services'] satisfies 'services'[],
    }
  : undefined

export async function searchAddresses(keyword: string): Promise<Address[]> {
  if (!kakaoOptions) throw new Error('지도 설정이 필요해요.')
  await new Loader(kakaoOptions).load()
  const services = kakao.maps.services

  return new Promise((resolve, reject) => {
    const finish = (rows: Address[]) =>
      resolve(
        rows.filter(
          (row) =>
            row.road &&
            Number.isFinite(row.latitude) &&
            Number.isFinite(row.longitude),
        ),
      )
    const fail = () => reject(new Error('주소를 불러오지 못했어요.'))
    new services.Geocoder().addressSearch(keyword, (rows, status) => {
      if (status === services.Status.OK) {
        finish(
          rows.map((row) => ({
            road: row.road_address?.address_name || row.address_name,
            jibun: row.address?.address_name ?? '',
            latitude: Number(row.y),
            longitude: Number(row.x),
          })),
        )
      } else if (status === services.Status.ZERO_RESULT) {
        new services.Places().keywordSearch(keyword, (places, placeStatus) => {
          if (placeStatus === services.Status.ERROR) fail()
          else
            finish(
              places.map((place) => ({
                road: place.road_address_name || place.address_name,
                jibun: place.address_name,
                latitude: Number(place.y),
                longitude: Number(place.x),
              })),
            )
        })
      } else fail()
    })
  })
}
