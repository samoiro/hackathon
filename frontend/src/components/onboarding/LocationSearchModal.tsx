import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Alert, Button, Input, Radio, Spin, Tag } from 'antd'
import { useState } from 'react'
import { LuInfo, LuSearch, LuStore } from 'react-icons/lu'
import EmptyState from '@/components/common/EmptyState'
import ResponsiveDialog from '@/components/common/ResponsiveDialog'
import { useDebouncedValue } from '@/hooks/useDebouncedValue'
import { searchAddresses } from '@/lib/kakao/maps'
import type { StoreLocation } from '@/types/search'

export default function LocationSearchModal({
  open,
  onClose,
  location,
  onSelect,
}: {
  open: boolean
  onClose: () => void
  location?: StoreLocation
  onSelect: (value: StoreLocation) => void
}) {
  const [keyword, setKeyword] = useState(location?.address.road ?? '')
  const [selected, setSelected] = useState(location?.address)
  const [name, setName] = useState(location?.name ?? '')
  const query = useDebouncedValue(keyword.trim())
  const {
    data: results = [],
    isFetching,
    isError,
  } = useQuery({
    queryKey: ['search', 'kakao-address', query],
    queryFn: () => searchAddresses(query),
    enabled: open && Boolean(query),
    placeholderData: keepPreviousData,
    retry: false,
  })

  const outsideSeoul = Boolean(
    selected && !/^서울(?:특별시)?\s/.test(selected.road),
  )

  return (
    <ResponsiveDialog
      open={open}
      onClose={onClose}
      afterOpenChange={(visible) => {
        if (visible) return
        setKeyword(location?.address.road ?? '')
        setSelected(location?.address)
        setName(location?.name ?? '')
      }}
      title="가게 위치 검색"
      width={560}
      height="42.3125rem" // 기본 글자 크기에서 검색 결과 두 건이 들어가는 팝업 높이
      footer={
        <>
          <Button className="max-lg:hidden" onClick={onClose}>
            취소
          </Button>
          <Button
            type="primary"
            disabled={!selected || !name.trim() || outsideSeoul}
            onClick={() => {
              if (selected) onSelect({ name: name.trim(), address: selected })
            }}
          >
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
          {isError && (
            <Alert type="error" showIcon title="주소를 불러오지 못했어요." />
          )}
          {Boolean(query) &&
            results.length === 0 &&
            !isFetching &&
            !isError && (
              <EmptyState
                title="검색 결과가 없어요."
                description="도로명이나 지번, 건물명으로 다시 찾아 주세요."
              />
            )}
          <Radio.Group
            value={
              selected
                ? `${selected.latitude}:${selected.longitude}`
                : undefined
            }
            onChange={(event) =>
              setSelected(
                results.find(
                  (row) =>
                    `${row.latitude}:${row.longitude}` === event.target.value,
                ),
              )
            }
            className="flex max-h-[calc(10.25rem+4px)] w-full flex-col gap-2 overflow-y-auto"
            aria-label="검색 결과"
          >
            {results.map((result) => (
              <Radio
                key={`${result.latitude}:${result.longitude}:${result.road}`}
                value={`${result.latitude}:${result.longitude}`}
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
            value={name}
            onChange={(event) => setName(event.target.value)}
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
