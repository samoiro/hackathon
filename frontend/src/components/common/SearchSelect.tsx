import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Select, Spin } from 'antd'
import type { SelectProps } from 'antd'
import { useState } from 'react'
import { useDebouncedValue } from '@/hooks/useDebouncedValue'
import type { SearchOption } from '@/types/search'

// 입력을 0.3초 기다린 뒤 서버 검색 결과로 옵션을 채우는 Select. 옵션을 화면에서 다시 거르지 않는다
export default function SearchSelect({
  queryKey,
  fetcher,
  ...selectProps
}: {
  queryKey: string
  fetcher: (keyword: string, signal: AbortSignal) => Promise<SearchOption[]>
} & Omit<
  SelectProps<string, SearchOption>,
  'options' | 'showSearch' | 'filterOption' | 'onSearch' | 'loading'
>) {
  const [keyword, setKeyword] = useState('')
  const debouncedKeyword = useDebouncedValue(keyword)
  const { data, isError, isFetching } = useQuery({
    queryKey: ['search', queryKey, debouncedKeyword],
    queryFn: ({ signal }) => fetcher(debouncedKeyword, signal),
    placeholderData: keepPreviousData,
    retry: false,
  })

  return (
    <Select<string, SearchOption>
      {...selectProps}
      showSearch
      filterOption={false}
      onSearch={setKeyword}
      loading={isFetching}
      options={data}
      notFoundContent={
        isFetching ? (
          <Spin size="small" />
        ) : isError ? (
          '검색 결과를 불러오지 못했어요. 검색어를 다시 입력해 주세요.'
        ) : (
          '검색 결과가 없어요'
        )
      }
    />
  )
}
