import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { api, formatDate, formatMoney, todayIso } from '../api/client.js'
import { returnApi } from '../api/returnApi.js'
import Pagination, { paginate } from '../components/Pagination.jsx'
import { useToast } from '../components/Toast.jsx'
import {
  daysBetween,
  indexById,
  matches,
  readerTypeLabel,
  ticketBooksText,
  ticketCopies,
  ticketState,
} from '../utils/libraryView.js'

const PAGE_SIZE = 10
const TABS = { BORROWING: 'borrowing', HISTORY: 'history' }
const QUICK_FILTERS = [
  { key: 'ALL', label: 'Tất cả' },
  { key: 'ON_TIME', label: 'Trong hạn' },
  { key: 'DUE_TODAY', label: 'Đến hạn hôm nay' },
  { key: 'OVERDUE', label: 'Quá hạn' },
]

function BookLines({ ticket }) {
  return (ticket?.details || []).map((d) => (
    <div key={d.bookId} className="small">
      {d.bookTitle || d.bookId} <span className="muted">× {d.quantity}</span>
    </div>
  ))
}

export default function ReturnPage() {
  const toast = useToast()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const openedFromUrl = useRef(false)

  const [tab, setTab] = useState(TABS.BORROWING)
  const [tickets, setTickets] = useState([]) // phiếu đang mượn (gồm cả quá hạn)
  const [returnedTickets, setReturnedTickets] = useState([])
  const [records, setRecords] = useState([])
  const [readers, setReaders] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  // Tab Đang mượn
  const [quick, setQuick] = useState('ALL')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)

  // Tab Lịch sử
  const [fineFilter, setFineFilter] = useState('ALL')
  const [fromDate, setFromDate] = useState('')
  const [toDate, setToDate] = useState('')
  const [histSearch, setHistSearch] = useState('')
  const [histPage, setHistPage] = useState(1)

  // Hộp xác nhận và kết quả
  const [selected, setSelected] = useState(null)
  const [returnDate, setReturnDate] = useState(todayIso())
  const [busy, setBusy] = useState(false)
  const [lastResult, setLastResult] = useState(null)

  const today = todayIso()

  /**
   * Nạp lại dữ liệu. Trả về danh sách phiếu đang mượn để dùng cho bước mở sẵn phiếu,
   * hoặc null nếu tải lỗi (khi đó hiện ô báo lỗi kèm nút "Thử lại").
   */
  const load = useCallback(async () => {
    setLoading(true)
    setLoadError('')
    try {
      const [borrowing, returned, recs, rds] = await Promise.all([
        returnApi.getBorrowingTickets(),
        api.get('/api/tickets?status=RETURNED'),
        returnApi.getReturnRecords(),
        api.get('/api/readers'),
      ])
      setTickets(borrowing || [])
      setReturnedTickets(returned || [])
      setRecords(recs || [])
      setReaders(rds || [])
      return borrowing || []
    } catch (e) {
      setLoadError(e.message)
      return null
    } finally {
      setLoading(false)
    }
  }, [])

  const openConfirm = (ticket) => {
    setSelected(ticket)
    setReturnDate(todayIso())
  }

  // Tải lần đầu, và mở sẵn phiếu nếu URL có ?ticketId=... (từ trang Phiếu mượn, Bạn đọc, Thống kê).
  // useRef làm cờ chạy một lần vì ở chế độ dev React chạy effect hai lần, thông báo sẽ hiện trùng.
  useEffect(() => {
    if (openedFromUrl.current) return
    openedFromUrl.current = true
    const ticketId = searchParams.get('ticketId')
    load().then((list) => {
      if (!ticketId || !list) return
      const found = list.find((t) => t.id === ticketId)
      if (found) openConfirm(found)
      else toast.error(`Phiếu ${ticketId} không ở trạng thái đang mượn.`)
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev)
          next.delete('ticketId')
          return next
        },
        { replace: true },
      )
    })
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  // Phím Esc đóng hộp xác nhận (không đóng khi đang gửi yêu cầu)
  useEffect(() => {
    if (!selected) return undefined
    const onKey = (e) => {
      if (e.key === 'Escape' && !busy) setSelected(null)
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [selected, busy])

  const readerById = useMemo(() => indexById(readers), [readers])
  const returnedById = useMemo(() => indexById(returnedTickets), [returnedTickets])

  // ---------- Tab Đang mượn ----------
  const rows = useMemo(
    () => tickets.map((t) => ({ ticket: t, state: ticketState(t, today), reader: readerById.get(t.readerId) })),
    [tickets, readerById, today],
  )

  const counts = useMemo(() => {
    const c = { ALL: rows.length, ON_TIME: 0, DUE_TODAY: 0, OVERDUE: 0 }
    rows.forEach((r) => {
      c[r.state.key] += 1
    })
    return c
  }, [rows])

  const filteredRows = useMemo(() => {
    return rows
      .filter((r) => quick === 'ALL' || r.state.key === quick)
      .filter((r) => matches(search, r.ticket.id, r.ticket.readerId, r.reader?.fullName, ticketBooksText(r.ticket)))
      .sort(
        (a, b) =>
          b.state.lateDays - a.state.lateDays || (a.ticket.dueDate < b.ticket.dueDate ? -1 : a.ticket.dueDate > b.ticket.dueDate ? 1 : 0),
      )
  }, [rows, quick, search])

  // ---------- Tab Lịch sử ----------
  const filteredRecords = useMemo(() => {
    return records
      .map((r) => ({ record: r, ticket: returnedById.get(r.ticketId), reader: readerById.get(r.readerId) }))
      .filter(({ record }) => fineFilter === 'ALL' || (fineFilter === 'FINE' ? record.fineAmount > 0 : record.fineAmount <= 0))
      .filter(({ record }) => (!fromDate || record.returnDate >= fromDate) && (!toDate || record.returnDate <= toDate))
      .filter(({ record, ticket, reader }) =>
        matches(histSearch, record.id, record.ticketId, record.readerId, reader?.fullName, ticket ? ticketBooksText(ticket) : ''),
      )
      .sort((a, b) =>
        a.record.returnDate < b.record.returnDate ? 1 : a.record.returnDate > b.record.returnDate ? -1 : b.record.id.localeCompare(a.record.id),
      )
  }, [records, returnedById, readerById, fineFilter, fromDate, toDate, histSearch])

  // ---------- Hộp xác nhận ----------
  const selectedReader = selected ? readerById.get(selected.readerId) : null
  let dateError = ''
  if (selected) {
    if (!returnDate) dateError = 'Chọn ngày trả'
    else if (returnDate < selected.borrowDate) dateError = `Ngày trả không được trước ngày mượn (${formatDate(selected.borrowDate)})`
  }
  const lateDays = selected && !dateError ? Math.max(0, daysBetween(selected.dueDate, returnDate)) : 0
  const dailyRate = selectedReader?.finePolicy?.dailyRate ?? 0
  const expectedFine = lateDays * dailyRate

  const confirmReturn = async () => {
    setBusy(true)
    try {
      const rec = await returnApi.returnBook(selected.id, returnDate)
      setLastResult({ ...rec, copies: ticketCopies(selected), readerName: selectedReader?.fullName })
      toast.success(`Đã trả phiếu ${rec.ticketId}.`)
      setSelected(null)
      await load()
    } catch (e) {
      toast.error(e.message)
      if (e.status === 409 || e.status === 404) {
        setSelected(null)
        await load()
      }
    } finally {
      setBusy(false)
    }
  }

  const closeModal = () => {
    if (!busy) setSelected(null)
  }

  return (
    <div className="page">
      <header className="page-head">
        <h1>Trả sách</h1>
      </header>

      {loadError && (
        <div
          className="card"
          role="alert"
          style={{
            marginBottom: 16,
            color: 'var(--late)',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: 12,
          }}
        >
          <span>Không tải được dữ liệu: {loadError}</span>
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => load()} disabled={loading}>
            Thử lại
          </button>
        </div>
      )}

      {lastResult && (
        <div
          className="card"
          style={{
            marginBottom: 16,
            background: '#ffffff',
            borderColor: 'var(--ok)',
            display: 'flex',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            alignItems: 'center',
            gap: 16,
          }}
        >
          <div>
            <div className="stat-label">Đã trả phiếu</div>
            <div className="mono" style={{ fontWeight: 700, fontSize: '1.2rem' }}>{lastResult.ticketId}</div>
            <div className="small">
              {lastResult.readerName || lastResult.readerId} · {lastResult.copies} cuốn · ngày trả {formatDate(lastResult.returnDate)}
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div className="stat-label">Tiền phạt đã ghi nhận</div>
            <div className={lastResult.fineAmount > 0 ? 'fine' : ''} style={{ fontWeight: 700, fontSize: '1.2rem' }}>
              {formatMoney(lastResult.fineAmount)}
            </div>
            <div className="muted small">
              {lastResult.overdueDays > 0 ? `Trễ ${lastResult.overdueDays} ngày · ${lastResult.policyName}` : 'Trả đúng hạn'}
            </div>
          </div>
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => setLastResult(null)}>
            Đóng
          </button>
        </div>
      )}

      <div className="tabs" role="tablist">
        <button
          role="tab"
          aria-selected={tab === TABS.BORROWING}
          className={tab === TABS.BORROWING ? 'tab active' : 'tab'}
          onClick={() => setTab(TABS.BORROWING)}
        >
          Đang mượn ({tickets.length})
        </button>
        <button
          role="tab"
          aria-selected={tab === TABS.HISTORY}
          className={tab === TABS.HISTORY ? 'tab active' : 'tab'}
          onClick={() => setTab(TABS.HISTORY)}
        >
          Lịch sử trả ({records.length})
        </button>
      </div>

      {tab === TABS.BORROWING && (
        <section>
          <div className="toolbar">
            <input
              className="input"
              style={{ flex: '1 1 280px' }}
              placeholder="Tìm mã phiếu, mã/tên bạn đọc hoặc tên sách…"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value)
                setPage(1)
              }}
            />
            {QUICK_FILTERS.map((f) => (
              <button
                key={f.key}
                type="button"
                className={quick === f.key ? 'btn btn-primary btn-sm' : 'btn btn-ghost btn-sm'}
                onClick={() => {
                  setQuick(f.key)
                  setPage(1)
                }}
              >
                {f.label} ({counts[f.key]})
              </button>
            ))}
          </div>

          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Mã phiếu</th>
                  <th>Bạn đọc</th>
                  <th>Sách</th>
                  <th className="right">Số cuốn</th>
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
                {!loading && !loadError && filteredRows.length === 0 && (
                  <tr>
                    <td colSpan="7" className="empty">Không có phiếu nào cần trả.</td>
                  </tr>
                )}
                {paginate(filteredRows, page, PAGE_SIZE).map(({ ticket, state, reader }) => (
                  <tr key={ticket.id}>
                    <td className="mono">{ticket.id}</td>
                    <td>
                      <div>{reader?.fullName || '—'}</div>
                      <div className="small muted mono">{ticket.readerId}</div>
                    </td>
                    <td>
                      <BookLines ticket={ticket} />
                    </td>
                    <td className="right">{ticketCopies(ticket)}</td>
                    <td>{formatDate(ticket.dueDate)}</td>
                    <td>
                      <span className={state.badge}>{state.label}</span>
                    </td>
                    <td className="right">
                      <button type="button" className="btn btn-primary btn-sm" onClick={() => openConfirm(ticket)}>
                        Trả sách
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={page} pageSize={PAGE_SIZE} total={filteredRows.length} onChange={setPage} unit="phiếu" />
        </section>
      )}

      {tab === TABS.HISTORY && (
        <section>
          <div className="toolbar">
            <input
              className="input"
              style={{ flex: '1 1 260px' }}
              placeholder="Tìm mã bản ghi, mã phiếu, bạn đọc hoặc sách…"
              value={histSearch}
              onChange={(e) => {
                setHistSearch(e.target.value)
                setHistPage(1)
              }}
            />
            <select
              className="input"
              value={fineFilter}
              onChange={(e) => {
                setFineFilter(e.target.value)
                setHistPage(1)
              }}
            >
              <option value="ALL">Có và không phạt</option>
              <option value="FINE">Có phạt</option>
              <option value="NO_FINE">Không phạt</option>
            </select>
            <label className="small muted">
              Trả từ{' '}
              <input
                type="date"
                className="input"
                value={fromDate}
                onChange={(e) => {
                  setFromDate(e.target.value)
                  setHistPage(1)
                }}
              />
            </label>
            <label className="small muted">
              đến{' '}
              <input
                type="date"
                className="input"
                value={toDate}
                onChange={(e) => {
                  setToDate(e.target.value)
                  setHistPage(1)
                }}
              />
            </label>
          </div>

          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Mã bản ghi</th>
                  <th>Phiếu</th>
                  <th>Bạn đọc</th>
                  <th>Sách</th>
                  <th>Ngày trả</th>
                  <th className="right">Ngày trễ</th>
                  <th>Chính sách phạt</th>
                  <th className="right">Tiền phạt</th>
                </tr>
              </thead>
              <tbody>
                {!loading && !loadError && filteredRecords.length === 0 && (
                  <tr>
                    <td colSpan="8" className="empty">Không có lượt trả nào khớp bộ lọc.</td>
                  </tr>
                )}
                {paginate(filteredRecords, histPage, PAGE_SIZE).map(({ record, ticket, reader }) => (
                  <tr key={record.id}>
                    <td className="mono">{record.id}</td>
                    <td>
                      <button type="button" className="link-btn mono" onClick={() => navigate(`/tickets?id=${record.ticketId}`)}>
                        {record.ticketId}
                      </button>
                    </td>
                    <td>
                      <div>{reader?.fullName || '—'}</div>
                      <div className="small muted mono">{record.readerId}</div>
                    </td>
                    <td className="small">{ticket ? ticketBooksText(ticket) : '—'}</td>
                    <td>{formatDate(record.returnDate)}</td>
                    <td className="right">{record.overdueDays}</td>
                    <td className="small">{record.policyName}</td>
                    <td className={record.fineAmount > 0 ? 'right fine' : 'right'}>{formatMoney(record.fineAmount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={histPage} pageSize={PAGE_SIZE} total={filteredRecords.length} onChange={setHistPage} unit="lượt trả" />
        </section>
      )}

      {selected && (
        <div className="modal-backdrop" onClick={closeModal}>
          <div className="modal modal-wide" role="dialog" aria-modal="true" aria-label={`Trả phiếu ${selected.id}`} onClick={(e) => e.stopPropagation()}>
            <h3>
              Trả phiếu <span className="mono">{selected.id}</span>
            </h3>

            <div className="modal-body">
              <div className="card" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 16 }}>
                <div>
                  <div className="stat-label">Bạn đọc</div>
                  <div style={{ fontWeight: 600 }}>{selectedReader?.fullName || '—'}</div>
                  <div className="small muted">
                    {selected.readerId} · {readerTypeLabel(selectedReader?.type)}
                  </div>
                </div>
                <div>
                  <div className="stat-label">Thời gian</div>
                  <div className="small">Mượn: {formatDate(selected.borrowDate)}</div>
                  <div className="small">Hạn trả: {formatDate(selected.dueDate)}</div>
                </div>
              </div>

              <div className="table-wrap" style={{ marginTop: 12 }}>
                <table className="table">
                  <thead>
                    <tr>
                      <th>Sách cần trả</th>
                      <th className="right">Số lượng</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(selected.details || []).map((d) => (
                      <tr key={d.bookId}>
                        <td>
                          {d.bookTitle || d.bookId} <span className="muted small mono">{d.bookId}</span>
                        </td>
                        <td className="right">{d.quantity}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <label className="field">
                <span>Ngày trả thực tế</span>
                <input
                  type="date"
                  className="input"
                  value={returnDate}
                  min={selected.borrowDate}
                  onChange={(e) => setReturnDate(e.target.value)}
                  aria-invalid={!!dateError}
                />
                {dateError && <span className="fine" role="alert">{dateError}</span>}
              </label>

              {!dateError && (
                <div className="card" style={{ marginTop: 14, background: lateDays > 0 ? 'var(--late-bg)' : 'var(--ok-bg)' }}>
                  {lateDays > 0 ? (
                    <>
                      <div className="small">
                        Trễ {lateDays} ngày × {formatMoney(dailyRate)}/ngày ({readerTypeLabel(selectedReader?.type)})
                      </div>
                      <div className="fine" style={{ fontWeight: 700, fontSize: '1.2rem' }}>
                        Tiền phạt dự kiến: {formatMoney(expectedFine)}
                      </div>
                    </>
                  ) : (
                    <strong>Trả đúng hạn, không bị phạt.</strong>
                  )}
                </div>
              )}
            </div>

            <div className="modal-actions">
              <button type="button" className="btn btn-ghost" onClick={closeModal} disabled={busy}>
                Hủy
              </button>
              <button type="button" className="btn btn-primary" onClick={confirmReturn} disabled={!!dateError || busy}>
                {busy ? 'Đang xử lý…' : 'Xác nhận trả toàn bộ phiếu'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
