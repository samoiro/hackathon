import { Button, Tag } from 'antd'
import { useState, type ReactNode } from 'react'
import { LuInfo } from 'react-icons/lu'
import ResponsiveDialog from '@/components/common/ResponsiveDialog'
import InterestItemsEditor from '@/components/items/InterestItemsEditor'
import type { InterestItems } from '@/types/search'

// BI-PC-04 품목 수정. 고친 내용은 '수정'을 눌러야 반영되고, 취소·닫기는 열기 전 그대로 둔다
export default function ItemEditModal({
  open,
  items,
  onClose,
  onSave,
  saving,
  children,
}: {
  open: boolean
  items: InterestItems
  onClose: () => void
  onSave: (items: InterestItems) => void
  saving: boolean
  children?: ReactNode
}) {
  const [draft, setDraft] = useState<InterestItems>()
  const selection = draft ?? items

  return (
    <ResponsiveDialog
      open={open}
      onClose={onClose}
      afterOpenChange={(visible) => {
        if (!visible) setDraft(undefined)
      }}
      title="품목 수정"
      width={760}
      footer={
        <>
          <Button className="max-lg:hidden" onClick={onClose}>
            취소
          </Button>
          <Button
            type="primary"
            loading={saving}
            onClick={() => onSave(selection)}
          >
            수정
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4 py-2">
        <div className="flex items-center gap-2">
          <h3 className="text-body-strong">관심 품목</h3>
          <Tag>선택</Tag>
          <span className="ml-auto text-caption text-muted">
            고를수록 브리핑이 구체적이 돼요
          </span>
        </div>
        {children}
        <InterestItemsEditor value={selection} onChange={setDraft} />
        <p className="flex items-center gap-2 rounded-xl bg-canvas p-4 text-caption text-muted">
          <LuInfo aria-hidden />
          조사 가격은 실제 매입가와 다를 수 있어요.
        </p>
        <p className="text-caption text-muted">
          수정하면 바뀐 품목 기준으로 브리핑을 다시 만들어요.
        </p>
      </div>
    </ResponsiveDialog>
  )
}
