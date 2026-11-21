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
  fetcher: (keyword: string) => Promise<SearchOption[]>
} & Omit<
  SelectProps,
  'options' | 'showSearch' | 'filterOption' | 'onSearch' | 'loading'
>) {
  const [keyword, setKeyword] = useState('')
  const debouncedKeyword = useDebouncedValue(keyword)
  const { data, isFetching } = useQuery({
    queryKey: ['search', queryKey, debouncedKeyword],
    queryFn: () => fetcher(debouncedKeyword),
    placeholderData: keepPreviousData,
  })

  return (
    <Select
      {...selectProps}
      showSearch
      filterOption={false}
      onSearch={setKeyword}
      loading={isFetching}
      options={data}
      notFoundContent={
        isFetching ? <Spin size="small" /> : '검색 결과가 없어요'
      }
    />
  )
}
