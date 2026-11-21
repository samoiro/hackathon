import { Button, Drawer, Modal } from 'antd'
import type { ReactNode } from 'react'
import { LuX } from 'react-icons/lu'
import { useIsMobile } from '@/hooks/useIsMobile'

// PC는 가운데 팝업(Modal), 모바일은 아래에서 올라오는 시트(Drawer)로 같은 내용을 띄운다 (BI-MO-01-1·03-1·04·06).
// 시트는 위 모서리 20, 손잡이, 오른쪽 위 닫기 버튼, 아래 고정 버튼 줄. 닫히면 antd가 연 버튼으로 포커스를 돌려준다
export default function ResponsiveDialog({
  open,
  onClose,
  title,
  width,
  footer,
  afterOpenChange,
  children,
}: {
  open: boolean
  onClose: () => void
  title: ReactNode
  width?: number
  // 모바일에서는 버튼이 줄을 꽉 채운다. PC에서만 보일 버튼은 className="max-lg:hidden"
  footer?: ReactNode
  afterOpenChange?: (open: boolean) => void
  children: ReactNode
}) {
  const mobile = useIsMobile()

  if (!mobile)
    return (
      <Modal
        open={open}
        onCancel={onClose}
        afterOpenChange={afterOpenChange}
        title={title}
        width={width}
        centered
        footer={
          footer ? <div className="flex justify-end gap-2">{footer}</div> : null
        }
      >
        {children}
      </Modal>
    )

  return (
    <Drawer
      open={open}
      onClose={onClose}
      afterOpenChange={afterOpenChange}
      placement="bottom"
      focusable={{ trap: true, focusTriggerAfterClose: true }}
      size="auto"
      closable={false}
      title={<span className="text-title">{title}</span>}
      extra={
        <Button
          aria-label="닫기"
          icon={<LuX aria-hidden />}
          onClick={onClose}
        />
      }
      footer={
        footer ? <div className="flex gap-2 [&>*]:flex-1">{footer}</div> : null
      }
      classNames={{
        section:
          'rounded-t-[20px] before:mx-auto before:mt-3 before:block before:h-1 before:w-10 before:rounded-full before:bg-line-strong',
      }}
      styles={{
        section: { maxHeight: '90dvh' },
        header: { padding: '12px 20px' },
        body: { padding: '20px' },
        footer: { padding: '12px 20px 20px' },
      }}
    >
      {children}
    </Drawer>
  )
}
