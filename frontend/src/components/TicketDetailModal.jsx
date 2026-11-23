import { formatDate, formatMoney } from '../api/client'
import { readerTypeLabel, ticketCopies, ticketState } from '../utils/libraryView'

export default function TicketDetailModal({
  ticket,
  reader = null,
  onClose,
  onReturn,
}) {
  if (!ticket) return null

  const state = ticketState(ticket)
  const returned = ticket.status === 'RETURNED'

  const dailyRate = Number(reader?.finePolicy?.dailyRate || 0)
  const estimatedFine = state.lateDays * dailyRate



  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal modal-wide"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="page-head">
          <div>
            <h2>Phiếu {ticket.id}</h2>
            <span className={state.badge}>{state.label}</span>
          </div>

          <button
            type="button"
            className="btn btn-ghost"
            onClick={onClose}
          >
            Đóng
          </button>
        </div>

        <div
          className="card"
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
            gap: 16,
            marginTop: 16,
          }}
        >
          <div>
            <h3>Bạn đọc</h3>
            <p>{reader?.fullName || 'Không xác định'}</p>
            <p>Mã: {ticket.readerId}</p>
            <p>Loại: {readerTypeLabel(reader?.type)}</p>
            <p>SĐT: {reader?.phone || '—'}</p>
          </div>

          <div>
            <h3>Thời gian</h3>
            <p>Mượn: {ticket.borrowDate ? formatDate(ticket.borrowDate) : '—'}</p>
            <p>Hạn trả: {ticket.dueDate ? formatDate(ticket.dueDate) : '—'}</p>
            <p>Ngày trả: {ticket.returnDate ? formatDate(ticket.returnDate) : 'Chưa trả'}</p>
          </div>

          <div>
            <h3>Tiền phạt</h3>

            {returned ? (
              <>
                <p>{formatMoney(ticket.fineAmount)}</p>
                <p>
                  {state.lateDays > 0
                    ? `Trả trễ ${state.lateDays} ngày`
                    : 'Trả đúng hạn'}
                </p>
              </>
            ) : state.key === 'OVERDUE' && dailyRate > 0 ? (
              <>
               <p>{formatMoney(estimatedFine)}</p>
                <p>Tạm tính nếu trả hôm nay</p>
              </>
            ) : (
              <p>Chưa phát sinh</p>
            )}
          </div>
        </div>

        <h3 style={{ marginTop: 20 }}>Danh sách sách</h3>

       <div className="table-wrap">
         <table className="table">
            <thead>
              <tr>
                <th>Mã sách</th>
                <th>Tên sách</th>
                <th>Số lượng</th>
              </tr>
            </thead>

            <tbody>
              {(ticket.details || []).map((item, index) => (
                <tr key={`${item.bookId}-${index}`}>
                  <td>{item.bookId}</td>
                  <td>{item.bookTitle || item.bookName || '—'}</td>
                  <td>{item.quantity}</td>
                </tr>
              ))}

              <tr>
                <td colSpan={2}>
                  <strong>Tổng số cuốn</strong>
                </td>
                <td>
                  <strong>{ticketCopies(ticket)}</strong>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        {!returned && onReturn && (
          <div style={{ marginTop: 20 }}>
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => onReturn(ticket)}
            >
              Trả sách cho phiếu này
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
