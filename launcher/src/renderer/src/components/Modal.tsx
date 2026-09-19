import { ReactNode } from 'react'
import { useT } from '../lib/i18n'

export default function Modal({
  title,
  children,
  footer,
  onClose,
  width = 980
}: {
  title: string
  children: ReactNode
  footer?: ReactNode
  onClose: () => void
  width?: number
}): JSX.Element {
  const t = useT()
  return (
    <div className="modal-back" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" style={{ width: `min(${width}px, 94vw)` }}>
        <div className="modal-head">
          <span>{title}</span>
          <button className="modal-x" onClick={onClose} aria-label={t('common.close')}>
            ✕
          </button>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-foot">{footer}</div>}
      </div>
    </div>
  )
}
