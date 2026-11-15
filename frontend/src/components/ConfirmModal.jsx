import { useEffect } from 'react'

/**
 * Hộp thoại xác nhận dùng chung.
 * Props: open, title, confirmText, cancelText, busy, onConfirm, onCancel, children (nội dung thêm, ví dụ ô nhập)
 */
export default function ConfirmModal({
  open,
  title,
  children,
  confirmText = 'Xác nhận',
  cancelText = 'Huỷ',
  busy = false,
  onConfirm,
  onCancel,
}) {
  useEffect(() => {
    if (!open) return
    const onKey = (e) => e.key === 'Escape' && !busy && onCancel()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, busy, onCancel])

  if (!open) return null
  return (
    <div className="modal-backdrop" onClick={() => !busy && onCancel()}>
      <div className="modal" role="dialog" aria-modal="true" aria-label={title} onClick={(e) => e.stopPropagation()}>
        <h3>{title}</h3>
        <div className="modal-body">{children}</div>
        <div className="modal-actions">
          <button className="btn btn-ghost" onClick={onCancel} disabled={busy}>
            {cancelText}
          </button>
          <button className="btn btn-primary" onClick={onConfirm} disabled={busy}>
            {busy ? 'Đang xử lý…' : confirmText}
          </button>
        </div>
      </div>
    </div>
  )
}
