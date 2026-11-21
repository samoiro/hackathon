import type { IngredientGroup, PriceStage } from '@/types/price'

// 부류별 4주 변동률(%) 경계: 하위 5 · 12.5 · 20 / 상위 20 · 12.5 · 5.
// shortcut: aT 2019-01~2026-11 분포로 계산한 표를 옮겨 둔 값이다. API가 priceStage를 주면 그 값을 쓰고 이 표는 지운다
const BANDS: Record<IngredientGroup, readonly number[]> = {
  채소류: [-26.8, -17.4, -11.8, 13.1, 21.2, 38.4],
  수산물: [-8.6, -4.1, -2.1, 2.6, 4.7, 9.1],
  식량작물: [-8.5, -4.4, -2.6, 2.9, 4.9, 9.0],
  특용작물: [-9.0, -4.7, -2.8, 3.4, 5.4, 11.2],
  과일류: [-18.3, -10.3, -6.4, 7.2, 10.8, 18.1],
  식품: [-14.1, -7.0, -3.8, 4.3, 8.4, 18.7],
  축산물: [-8.2, -4.9, -2.7, 3.4, 5.4, 8.6],
}

export function stageOf(change4w: number, group: IngredientGroup): PriceStage {
  const [fall5, fall12, fall20, rise20, rise12, rise5] = BANDS[group]
  if (change4w <= fall5) return 'FALL_SEVERE'
  if (change4w <= fall12) return 'FALL_ALERT'
  if (change4w <= fall20) return 'FALL_CAUTION'
  if (change4w < rise20) return 'STABLE'
  if (change4w < rise12) return 'RISE_CAUTION'
  if (change4w < rise5) return 'RISE_ALERT'
  return 'RISE_SEVERE'
}

// 심각·경계는 채운 배지 + 흰 글자, 주의·안정은 옅은 배지 + 진한 글자. ink는 차트 선·수치 글자색
export const PRICE_STAGE_STYLE: Record<
  PriceStage,
  { label: string; chip: string; ink: string; inkClass: string }
> = {
  RISE_SEVERE: {
    label: '상승 심각',
    chip: 'bg-stage-rise-3 text-on-stage',
    ink: 'var(--color-stage-rise-ink)',
    inkClass: 'text-stage-rise-ink',
  },
  RISE_ALERT: {
    label: '상승 경계',
    chip: 'bg-stage-rise-2 text-on-stage',
    ink: 'var(--color-stage-rise-ink)',
    inkClass: 'text-stage-rise-ink',
  },
  RISE_CAUTION: {
    label: '상승 주의',
    chip: 'bg-stage-rise-1 text-stage-rise-ink',
    ink: 'var(--color-stage-rise-ink)',
    inkClass: 'text-stage-rise-ink',
  },
  STABLE: {
    label: '안정',
    chip: 'bg-stage-flat text-stage-flat-ink',
    ink: 'var(--color-stage-flat-ink)',
    inkClass: 'text-stage-flat-ink',
  },
  FALL_CAUTION: {
    label: '하락 주의',
    chip: 'bg-stage-fall-1 text-stage-fall-ink',
    ink: 'var(--color-stage-fall-ink)',
    inkClass: 'text-stage-fall-ink',
  },
  FALL_ALERT: {
    label: '하락 경계',
    chip: 'bg-stage-fall-2 text-on-stage',
    ink: 'var(--color-stage-fall-ink)',
    inkClass: 'text-stage-fall-ink',
  },
  FALL_SEVERE: {
    label: '하락 심각',
    chip: 'bg-stage-fall-3 text-on-stage',
    ink: 'var(--color-stage-fall-ink)',
    inkClass: 'text-stage-fall-ink',
  },
}
