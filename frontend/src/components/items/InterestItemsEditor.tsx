import { App } from 'antd'
import { LuSearch, LuTriangleAlert } from 'react-icons/lu'
import RemovableChip from '@/components/common/RemovableChip'
import SearchSelect from '@/components/common/SearchSelect'
import {
  search as searchSupplies,
  search1 as searchIngredients,
} from '@/lib/api/generated'
import type {
  InterestItem,
  InterestItemGroup,
  InterestItems,
} from '@/types/search'

const GROUPS: {
  key: InterestItemGroup
  title: string
  max: number
  placeholder: string
  search: typeof searchIngredients | typeof searchSupplies
  note: string
}[] = [
  {
    key: 'raw',
    title: '원자재',
    max: 3,
    placeholder: '원자재 검색·추가',
    search: searchIngredients,
    note: '조사 가격이 있는 품목은 가격과 4주 추이를 보여드려요.',
  },
  {
    key: 'sub',
    title: '부자재',
    max: 2,
    placeholder: '부자재 검색·추가',
    search: searchSupplies,
    note: '가격 자료가 없어 준비 시점으로 안내해요.',
  },
]

// 관심 품목(원자재·부자재) 고르기. 가게 정보 입력 화면과 품목 수정 팝업이 함께 쓴다
export default function InterestItemsEditor({
  value,
  onChange,
}: {
  value: InterestItems
  onChange: (value: InterestItems) => void
}) {
  const { notification } = App.useApp()

  const add = (
    { key, title, max }: (typeof GROUPS)[number],
    item: InterestItem,
  ) => {
    if (value[key].length >= max) {
      notification.open({
        key: `interest-items-limit-${key}`,
        role: 'status',
        icon: <LuTriangleAlert aria-hidden className="text-primary-300" />,
        title: `${title}는 최대 ${max}개까지 고를 수 있어요.`,
        description: '다른 품목을 담으려면 하나를 빼 주세요.',
      })
      return
    }
    if (value[key].some(({ id }) => id === item.id)) return
    onChange({ ...value, [key]: [...value[key], item] })
  }
  const remove = (key: InterestItemGroup, id: number) =>
    onChange({ ...value, [key]: value[key].filter((item) => item.id !== id) })

  return (
    <div className="grid gap-4 lg:grid-cols-2">
      {GROUPS.map((group) => {
        const full = value[group.key].length >= group.max
        return (
          <div key={group.key} className="rounded-xl bg-canvas p-4">
            <div className="flex justify-between">
              <h4 className="text-body-strong">{group.title}</h4>
              <span className="text-caption text-muted">
                {value[group.key].length} / {group.max}
              </span>
            </div>
            <SearchSelect
              // 고른 뒤 입력칸을 비우려고 개수가 바뀔 때마다 새로 그린다
              key={value[group.key].length}
              queryKey={group.key}
              fetcher={async (keyword, signal) => {
                const { data } = await group.search({
                  query: { query: keyword.trim() || undefined },
                  signal,
                  throwOnError: true,
                })
                return data.map(({ id, name }) => ({
                  value: String(id),
                  label: name,
                }))
              }}
              aria-label={group.placeholder}
              className="mt-3 w-full"
              placeholder={group.placeholder}
              prefix={<LuSearch aria-hidden />}
              onSelect={(id, option) =>
                add(group, { id: Number(id), name: option.label })
              }
            />
            <div className="mt-3 flex flex-wrap gap-2">
              {value[group.key].map((item) => (
                <RemovableChip
                  key={item.id}
                  label={item.name}
                  onRemove={() => remove(group.key, item.id)}
                />
              ))}
            </div>
            {/* 가득 찼을 때도 입력칸은 열어 두고, 왜 더 담기지 않는지 알려준다 */}
            <p aria-live="polite" className="mt-3 text-caption text-muted">
              {full
                ? `${group.title}는 ${group.max}개까지 고를 수 있어요. 하나를 빼면 다른 품목을 담을 수 있어요.`
                : group.note}
            </p>
          </div>
        )
      })}
    </div>
  )
}
