import { Button, Card, Tag } from 'antd'
import { useState } from 'react'
import {
  LuArrowRight,
  LuCheck,
  LuChevronDown,
  LuChevronUp,
  LuClock,
  LuInfo,
  LuListChecks,
  LuMapPin,
  LuPlus,
  LuSearch,
} from 'react-icons/lu'
import { useNavigate } from 'react-router'
import ChoiceChips from '@/components/common/ChoiceChips'
import SearchSelect from '@/components/common/SearchSelect'
import AppLayout from '@/components/layout/AppLayout'
import StepIndicator from '@/components/layout/StepIndicator'
import InterestItemsEditor from '@/components/items/InterestItemsEditor'
import LocationSearchModal from '@/components/onboarding/LocationSearchModal'
import type { InterestItems } from '@/types/search'
import { searchCategories } from '@/mocks/search'

const BENEFITS = [
  {
    icon: LuClock,
    title: '1분',
    body: '길게 읽지 않아도 오늘 할 일이 보여요',
  },
  { icon: LuInfo, title: '근거', body: '출처와 기준 시각을 함께 보여드려요' },
  {
    icon: LuListChecks,
    title: '준비',
    body: '읽고 끝이 아니라 체크할 일로 남겨요',
  },
]

type SalesType = 'hall' | 'delivery' | 'both'

const SALES_TYPES: { value: SalesType; label: string }[] = [
  { value: 'hall', label: '매장만' },
  { value: 'delivery', label: '배달만' },
  { value: 'both', label: '매장·배달 모두' },
]

// 카드 테두리만 브랜드색으로 바꾼다. hover: 변형은 마우스가 있는 기기에서만 적용된다
const CARD_HOVER = 'transition-colors duration-150 hover:border-primary-500'

function RequiredTag() {
  return <Tag>필수</Tag>
}

export default function OnboardingPage() {
  const navigate = useNavigate()
  const [locationOpen, setLocationOpen] = useState(false)
  const [moreOpen, setMoreOpen] = useState(false)
  const [salesType, setSalesType] = useState<SalesType>('both')
  const [items, setItems] = useState<InterestItems>({
    raw: ['쌀', '돼지고기', '배추'],
    sub: ['포장 용기'],
  })

  const summary = [
    ['가게 이름', '성수동 한식당'],
    ['위치', '성수동 · 서울숲 인근'],
    ['업종', '일반 음식점'],
    ...(moreOpen
      ? [
          [
            '운영 형태',
            SALES_TYPES.find((type) => type.value === salesType)?.label ?? '',
          ],
          ['원자재', items.raw.join(' · ') || '없음'],
          ['부자재', items.sub.join(' · ') || '없음'],
        ]
      : [['선택 항목', '아직 고르지 않았어요']]),
  ]

  return (
    <AppLayout
      title="가게 정보 입력"
      headerContext={
        <div className="mx-auto">
          <StepIndicator current={1} />
        </div>
      }
    >
      <p className="font-semibold text-brand-text">
        사장님의 하루를 바꾸는 1분
      </p>
      <h1 className="mt-2 text-display">가게 정보를 알려주세요</h1>
      <p className="mt-3 text-muted">
        가게와 업종만 알려주시면 바로 시작할 수 있어요.
      </p>

      <div className="mt-8 grid grid-cols-[minmax(0,736fr)_minmax(0,360fr)] items-start gap-6">
        <div className="flex flex-col gap-6">
          <Card className={CARD_HOVER}>
            <section aria-labelledby="store-heading">
              <div className="flex items-center gap-2">
                <h2 id="store-heading" className="text-item font-bold">
                  가게 이름과 위치
                </h2>
                <RequiredTag />
              </div>
              <div className="mt-4 flex items-center gap-4 rounded-xl border border-border px-6 py-5">
                <LuMapPin aria-hidden className="size-6 shrink-0" />
                <div className="flex-1">
                  <p className="text-body-strong">성수동 한식당</p>
                  <p>서울 성동구 서울숲2길 32</p>
                  <p className="text-caption text-muted">
                    지번 · 성수동1가 ○○○-○
                  </p>
                </div>
                <Button size="small" onClick={() => setLocationOpen(true)}>
                  변경
                </Button>
              </div>
              <p className="mt-3 text-caption text-muted">
                주소를 기준으로 도보 15분(직선 1km) 안의 행사를 찾아요. 현재
                서울 지역만 지원해요.
              </p>
            </section>

            <section
              aria-labelledby="category-heading"
              className="mt-6 border-t border-divider pt-6"
            >
              <div className="flex items-center gap-2">
                <h2 id="category-heading" className="text-item font-bold">
                  업종
                </h2>
                <RequiredTag />
              </div>
              <SearchSelect
                queryKey="category"
                fetcher={searchCategories}
                aria-labelledby="category-heading"
                className="mt-4 w-full"
                defaultValue="일반 음식점 · 한식"
                prefix={<LuSearch aria-hidden />}
              />
              <p className="mt-3 text-caption text-muted">
                업종에 맞는 기능만 골라 보여드려요.
              </p>
            </section>
          </Card>

          <Card className={CARD_HOVER}>
            <section aria-labelledby="more-heading">
              <div className="flex items-start gap-4">
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <h2 id="more-heading" className="text-item font-bold">
                      더 알려주시면 브리핑이 구체적이 돼요
                    </h2>
                    <Tag>선택 2개</Tag>
                  </div>
                  <p className="mt-1 text-caption text-muted">
                    운영 형태 · 관심 품목. 나중에 대시보드에서도 고칠 수 있어요.
                  </p>
                </div>
                <Button
                  aria-expanded={moreOpen}
                  aria-controls="more-panel"
                  aria-label={moreOpen ? '선택 항목 접기' : '선택 항목 펼치기'}
                  icon={
                    moreOpen ? (
                      <LuChevronUp aria-hidden />
                    ) : (
                      <LuChevronDown aria-hidden />
                    )
                  }
                  onClick={() => setMoreOpen((open) => !open)}
                />
              </div>

              {moreOpen ? (
                <div id="more-panel" className="mt-6 border-t border-divider">
                  <div className="border-b border-divider py-6">
                    <div className="flex items-center gap-2">
                      <h3 className="text-item">운영 형태</h3>
                      <Tag>선택</Tag>
                    </div>
                    <div className="mt-4">
                      <ChoiceChips
                        block
                        label="운영 형태"
                        value={salesType}
                        onChange={setSalesType}
                        options={SALES_TYPES}
                      />
                    </div>
                    <p className="mt-3 text-caption text-muted">
                      선택한 운영 방식에 맞춰 준비할 일을 제안해요.
                    </p>
                  </div>

                  <div className="pt-6">
                    <div className="flex items-center gap-2">
                      <h3 className="text-item">관심 품목</h3>
                      <Tag>선택</Tag>
                    </div>
                    <div className="mt-4">
                      <InterestItemsEditor value={items} onChange={setItems} />
                    </div>
                    <p className="mt-4 flex items-center gap-2 rounded-xl bg-canvas p-4 text-caption text-muted">
                      <LuInfo aria-hidden />
                      조사 가격은 실제 매입가와 다를 수 있어요.
                    </p>
                  </div>
                </div>
              ) : (
                <div
                  id="more-panel"
                  className="mt-6 flex items-center justify-between rounded-xl bg-canvas p-4"
                >
                  <div>
                    <p className="text-caption font-semibold text-muted">
                      일반 음식점에서 많이 고르는 품목
                    </p>
                    <p className="mt-1">쌀 · 돼지고기 · 배추</p>
                  </div>
                  <Button
                    size="small"
                    icon={<LuPlus aria-hidden />}
                    onClick={() => setMoreOpen(true)}
                  >
                    한 번에 담기
                  </Button>
                </div>
              )}
            </section>
          </Card>
        </div>

        <div className="flex flex-col gap-6">
          <Card className={CARD_HOVER}>
            <section aria-labelledby="summary-heading">
              <h2 id="summary-heading" className="text-item font-bold">
                선택한 가게 요약
              </h2>
              <dl className="mt-4 flex flex-col gap-3 border-b border-divider pb-5 text-caption">
                {summary.map(([term, value]) => (
                  <div key={term} className="flex justify-between gap-4">
                    <dt className="text-muted">{term}</dt>
                    <dd className="text-right font-semibold">{value}</dd>
                  </div>
                ))}
              </dl>
              <p className="mt-4 flex items-center gap-2 text-caption text-muted">
                <Tag color="success" icon={<LuCheck aria-hidden />}>
                  필수 입력 완료
                </Tag>
                가게 · 업종
              </p>
              <Button
                type="primary"
                block
                className="mt-4"
                icon={<LuArrowRight aria-hidden />}
                iconPlacement="end"
                onClick={() => navigate('/briefing')}
              >
                이번 주 브리핑 받기
              </Button>
              <p className="mt-4 text-center text-caption text-muted">
                브리핑은 약 1분이면 준비돼요
              </p>
            </section>
          </Card>

          <Card className={CARD_HOVER}>
            <ul className="flex flex-col gap-4">
              {BENEFITS.map(({ icon: Icon, title, body }) => (
                <li key={title} className="flex items-start gap-4">
                  <span className="flex size-10 shrink-0 items-center justify-center rounded-full border border-divider">
                    <Icon aria-hidden className="size-5" />
                  </span>
                  <span>
                    <span className="block text-body-strong">{title}</span>
                    <span className="block text-caption text-muted">
                      {body}
                    </span>
                  </span>
                </li>
              ))}
            </ul>
          </Card>
        </div>
      </div>
      <LocationSearchModal
        open={locationOpen}
        onClose={() => setLocationOpen(false)}
      />
    </AppLayout>
  )
}
