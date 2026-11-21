import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Alert, Button, Input, Radio, Spin, Tag } from 'antd'
import { useState } from 'react'
import { LuInfo, LuSearch, LuStore } from 'react-icons/lu'
import EmptyState from '@/components/common/EmptyState'
import ResponsiveDialog from '@/components/common/ResponsiveDialog'
import { useDebouncedValue } from '@/hooks/useDebouncedValue'
import { searchAddresses } from '@/mocks/search'

export default function LocationSearchModal({
  open,
  onClose,
}: {
  open: boolean
  onClose: () => void
}) {
  const [keyword, setKeyword] = useState('서울숲2길')
  const [selected, setSelected] = useState('서울 성동구 서울숲2길 32')
  const debouncedKeyword = useDebouncedValue(keyword)
  const { data: results = [], isFetching } = useQuery({
    queryKey: ['search', 'address', debouncedKeyword],
    queryFn: () => searchAddresses(debouncedKeyword),
    placeholderData: keepPreviousData,
  })

  // shortcut: 서울 여부를 도로명 앞글자로 본다. 주소 API 연동 때 시·도 코드로 바꾼다
  const outsideSeoul = !selected.startsWith('서울')

  return (
    <ResponsiveDialog
      open={open}
      onClose={onClose}
      title="가게 위치 검색"
      width={560}
      height="42.3125rem" // 기본 글자 크기에서 검색 결과 두 건이 들어가는 팝업 높이
      footer={
        <>
          <Button className="max-lg:hidden" onClick={onClose}>
            취소
          </Button>
          <Button type="primary" disabled={outsideSeoul} onClick={onClose}>
            이 주소로 변경
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-6 py-2">
        <section aria-labelledby="location-label">
          <div className="mb-2 flex items-center gap-2">
            <label
              id="location-label"
              htmlFor="location-search"
              className="text-item"
            >
              가게 위치
            </label>
            <Tag>필수</Tag>
          </div>
          <Input
            id="location-search"
            allowClear
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            suffix={isFetching ? <Spin size="small" /> : null}
            prefix={<LuSearch aria-hidden />}
          />
          <div className="mt-3 mb-2 flex justify-between text-caption text-muted">
            <span className="font-semibold" aria-live="polite">
              {isFetching ? '찾는 중…' : `검색 결과 ${results.length}건`}
            </span>
            <span>도로명 · 지번 · 건물명으로 찾아요</span>
          </div>
          {results.length === 0 && !isFetching && (
            <EmptyState
              title="검색 결과가 없어요."
              description="도로명이나 지번, 건물명으로 다시 찾아 주세요."
            />
          )}
          <Radio.Group
            value={selected}
            onChange={(event) => setSelected(event.target.value)}
            className="flex max-h-[calc(10.25rem+4px)] w-full flex-col gap-2 overflow-y-auto"
            aria-label="검색 결과"
          >
            {results.map((result) => (
              <Radio
                key={result.road}
                value={result.road}
                className="m-0 flex shrink-0 items-center rounded-xl border border-divider p-4 has-[:checked]:border-primary-700 has-[:checked]:bg-primary-50"
              >
                <span className="block text-body-strong">{result.road}</span>
                <span className="block text-caption text-muted">
                  {result.jibun}
                </span>
              </Radio>
            ))}
          </Radio.Group>
          {outsideSeoul ? (
            <Alert
              className="mt-3"
              type="error"
              showIcon
              title="서울 밖 주소는 아직 지원하지 않아요. 서울 안의 가게 주소를 골라 주세요."
            />
          ) : (
            <p className="mt-3 flex items-center gap-2 rounded-xl bg-canvas p-4 text-caption text-muted">
              <LuInfo aria-hidden />
              현재 서울 지역만 지원해요. 주소를 바꾸면 주변 행사를 다시 찾아요.
            </p>
          )}
        </section>

        <section className="border-t border-divider pt-6">
          <div className="mb-2 flex items-center gap-2">
            <label htmlFor="store-name" className="text-item">
              가게 이름
            </label>
            <Tag>필수</Tag>
          </div>
          <Input
            id="store-name"
            defaultValue="성수동 한식당"
            prefix={<LuStore aria-hidden />}
          />
          <p className="mt-2 text-caption text-muted">
            브리핑과 대시보드 위쪽에 이 이름으로 보여드려요.
          </p>
        </section>
      </div>
    </ResponsiveDialog>
  )
}
