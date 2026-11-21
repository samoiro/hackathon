import type { PriceStage } from '@/types/price'

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
