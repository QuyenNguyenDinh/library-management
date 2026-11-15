import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, formatDate, formatMoney, todayIso } from '../api/client.js'
import ConfirmModal from '../components/ConfirmModal.jsx'
import { useToast } from '../components/Toast.jsx'

const TABS = { BORROWING: 'borrowing', HISTORY: 'history' }

export default function ReturnPage() {
  const toast = useToast()
  const [tab, setTab] = useState(TABS.BORROWING)
  const [tickets, setTickets] = useState([])
  const [records, setRecords] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [selected, setSelected] = useState(null)
  const [returnDate, setReturnDate] = useState(todayIso())
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      // /api/tickets do Sang làm (hợp đồng API); /api/return-records do phần trả sách.
      const [t, r] = await Promise.all([api.get('/api/tickets?status=BORROWING'), api.get('/api/return-records')])
      setTickets(t || [])
      setRecords(r || [])
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
    }
  }, [toast])

  useEffect(() => {
    load()
  }, [load])

  const filteredTickets = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return tickets
    return tickets.filter((t) => `${t.id} ${t.readerId}`.toLowerCase().includes(q))
  }, [tickets, search])

  const sortedRecords = useMemo(() => [...records].reverse(), [records])

  const openConfirm = (ticket) => {
    setSelected(ticket)
    setReturnDate(todayIso())
  }

  const confirmReturn = async () => {
    setBusy(true)
    try {
      const rec = await api.post(`/api/return/${selected.id}`, { returnDate })
      toast.success(
        rec.fineAmount > 0
          ? `Đã trả phiếu ${rec.ticketId}. Trễ ${rec.overdueDays} ngày, phạt ${formatMoney(rec.fineAmount)}.`
          : `Đã trả phiếu ${rec.ticketId}. Không bị phạt.`,
      )
      setSelected(null)
      await load()
    } catch (e) {
      toast.error(e.message) // 404 không thấy phiếu, 409 đã trả, 400 ngày sai
    } finally {
      setBusy(false)
    }
  }

  const isOverdue = (t) => t.dueDate && t.dueDate < todayIso()

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <h1>Trả sách</h1>
          <p className="muted">Ghi nhận trả sách, hệ thống tự tính tiền phạt theo nhóm bạn đọc.</p>
        </div>
        <button className="btn btn-ghost" onClick={load} disabled={loading}>
          Tải lại
        </button>
      </header>

      <div className="tabs" role="tablist">
        <button role="tab" aria-selected={tab === TABS.BORROWING} className={tab === TABS.BORROWING ? 'tab active' : 'tab'} onClick={() => setTab(TABS.BORROWING)}>
          Đang mượn ({tickets.length})
        </button>
        <button role="tab" aria-selected={tab === TABS.HISTORY} className={tab === TABS.HISTORY ? 'tab active' : 'tab'} onClick={() => setTab(TABS.HISTORY)}>
          Lịch sử trả ({records.length})
        </button>
      </div>

      {tab === TABS.BORROWING && (
        <section>
          <input
            className="input search"
            placeholder="Tìm theo mã phiếu hoặc mã bạn đọc…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Mã phiếu</th>
                  <th>Bạn đọc</th>
                  <th>Sách</th>
                  <th>Ngày mượn</th>
                  <th>Hạn trả</th>
                  <th>Tình trạng</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {loading && (
                  <tr>
                    <td colSpan="7" className="empty">Đang tải…</td>
                  </tr>
                )}
                {!loading && filteredTickets.length === 0 && (
                  <tr>
                    <td colSpan="7" className="empty">Không có phiếu nào đang mượn.</td>
                  </tr>
                )}
                {filteredTickets.map((t) => (
                  <tr key={t.id}>
                    <td className="mono">{t.id}</td>
                    <td className="mono">{t.readerId}</td>
                    <td>
                      {(t.details || []).map((d) => (
                        <div key={d.bookId}>
                          {d.bookTitle || d.bookId} <span className="muted">× {d.quantity}</span>
                        </div>
                      ))}
                    </td>
                    <td>{formatDate(t.borrowDate)}</td>
                    <td>{formatDate(t.dueDate)}</td>
                    <td>
                      <span className={isOverdue(t) ? 'badge badge-late' : 'badge badge-ok'}>
                        {isOverdue(t) ? 'Quá hạn' : 'Trong hạn'}
                      </span>
                    </td>
                    <td className="right">
                      <button className="btn btn-primary btn-sm" onClick={() => openConfirm(t)}>
                        Trả sách
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {tab === TABS.HISTORY && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Mã bản ghi</th>
                <th>Phiếu</th>
                <th>Bạn đọc</th>
                <th>Ngày trả</th>
                <th className="right">Ngày trễ</th>
                <th>Chính sách phạt</th>
                <th className="right">Tiền phạt</th>
              </tr>
            </thead>
            <tbody>
              {!loading && sortedRecords.length === 0 && (
                <tr>
                  <td colSpan="7" className="empty">Chưa có lượt trả nào.</td>
                </tr>
              )}
              {sortedRecords.map((r) => (
                <tr key={r.id}>
                  <td className="mono">{r.id}</td>
                  <td className="mono">{r.ticketId}</td>
                  <td className="mono">{r.readerId}</td>
                  <td>{formatDate(r.returnDate)}</td>
                  <td className="right">{r.overdueDays}</td>
                  <td>{r.policyName}</td>
                  <td className={r.fineAmount > 0 ? 'right fine' : 'right'}>{formatMoney(r.fineAmount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <ConfirmModal
        open={!!selected}
        title={`Trả phiếu ${selected?.id ?? ''}`}
        confirmText="Xác nhận trả"
        busy={busy}
        onConfirm={confirmReturn}
        onCancel={() => setSelected(null)}
      >
        <p>
          Bạn đọc <strong className="mono">{selected?.readerId}</strong>, hạn trả{' '}
          <strong>{formatDate(selected?.dueDate)}</strong>.
        </p>
        <label className="field">
          <span>Ngày trả</span>
          <input
            type="date"
            className="input"
            value={returnDate}
            min={selected?.borrowDate}
            onChange={(e) => setReturnDate(e.target.value)}
          />
        </label>
        <p className="muted small">Tiền phạt do hệ thống tính khi xác nhận, dựa trên nhóm bạn đọc.</p>
      </ConfirmModal>
    </div>
  )
}
