import { Button, Tag } from 'antd'
import { useState } from 'react'
import { LuInfo } from 'react-icons/lu'
import ResponsiveDialog from '@/components/common/ResponsiveDialog'
import InterestItemsEditor from '@/components/items/InterestItemsEditor'
import type { InterestItems } from '@/types/search'

// BI-PC-04 품목 수정. 고친 내용은 '수정'을 눌러야 반영되고, 취소·닫기는 열기 전 그대로 둔다
// shortcut: 반영은 화면 안에서만 된다. 기능 구현 때 품목 저장 mutation과 브리핑 재생성에 연결한다
export default function ItemEditModal({
  open,
  items,
  onClose,
  onSave,
}: {
  open: boolean
  items: InterestItems
  onClose: () => void
  onSave: (items: InterestItems) => void
}) {
  const [draft, setDraft] = useState(items)

  return (
    <ResponsiveDialog
      open={open}
      onClose={onClose}
      afterOpenChange={(visible) => visible && setDraft(items)}
      title="품목 수정"
      width={760}
      footer={
        <>
          <Button className="max-lg:hidden" onClick={onClose}>
            취소
          </Button>
          <Button
            type="primary"
            onClick={() => {
              onSave(draft)
              onClose()
            }}
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
        <InterestItemsEditor value={draft} onChange={setDraft} />
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
