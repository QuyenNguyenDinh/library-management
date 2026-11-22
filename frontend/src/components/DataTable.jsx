
export default function DataTable({
  columns,
  rows = [],
  emptyText = 'Không có dữ liệu',
  loading = false
}) {
  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            {columns.map((c) => (
              <th
                key={c.key}
                className={c.align === 'right' ? 'right' : undefined}
              >
                {c.label}
              </th>
            ))}
          </tr>
        </thead>

        <tbody>
          {loading && (
            <tr>
              <td colSpan={columns.length} className="empty">
                Đang tải…
              </td>
            </tr>
          )}

          {!loading && rows.length === 0 && (
            <tr>
              <td colSpan={columns.length} className="empty">
                {emptyText}
              </td>
            </tr>
          )}

          {!loading && rows.map((r, i) => (
            <tr key={r.id || i}>
              {columns.map((c) => (
                <td
                  key={c.key}
                  className={c.align === 'right' ? 'right' : undefined}
                >
                  {c.render ? c.render(r) : r[c.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
