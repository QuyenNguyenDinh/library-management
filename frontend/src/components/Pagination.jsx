/** Số trang = làm tròn lên total / pageSize, nhỏ nhất là 1. */
function pageCount(total, pageSize) {
  return Math.max(1, Math.ceil(total / pageSize))
}

/** Cắt danh sách theo trang (page bắt đầu từ 1). Trang vượt quá thì lấy trang cuối. */
export function paginate(list, page, pageSize) {
  const current = Math.min(Math.max(1, page), pageCount(list.length, pageSize))
  const start = (current - 1) * pageSize
  return list.slice(start, start + pageSize)
}

/**
 * Thanh phân trang dùng chung.
 * Props: page (từ 1), pageSize, total, onChange(trangMới), unit (ví dụ "phiếu").
 */
export default function Pagination({ page, pageSize, total, onChange, unit = '' }) {
  if (!total) return null

  const pages = pageCount(total, pageSize)
  const current = Math.min(Math.max(1, page), pages)
  const from = (current - 1) * pageSize + 1
  const to = Math.min(current * pageSize, total)

  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: 8,
        marginTop: 12,
      }}
    >
      <span className="muted small">
        Hiển thị {from}–{to} trên {total} {unit}
      </span>
      {pages > 1 && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            disabled={current <= 1}
            onClick={() => onChange(current - 1)}
          >
            Trước
          </button>
          <span className="small">
            Trang {current}/{pages}
          </span>
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            disabled={current >= pages}
            onClick={() => onChange(current + 1)}
          >
            Sau
          </button>
        </div>
      )}
    </div>
  )
}
