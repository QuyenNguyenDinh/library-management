import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { api, formatDate, formatMoney, todayIso } from '../api/client.js'
import { returnApi } from '../api/returnApi.js'
import Pagination, { paginate } from '../components/Pagination.jsx'
import { useToast } from '../components/Toast.jsx'
import {
  READER_TYPES,
  daysBetween,
  indexById,
  matches,
  readerTypeLabel,
  ticketBooksText,
  ticketCopies,
  ticketState,
} from '../utils/libraryView.js'

const TABS = { BORROWING: 'borrowing', HISTORY: 'history' }
const PAGE_SIZE = 8

const STATE_FILTERS = [
  { key: 'ALL', label: 'Tất cả' },
  { key: 'ON_TIME', label: 'Trong hạn' },
  { key: 'DUE_TODAY', label: 'Đến hạn hôm nay' },
  { key: 'OVERDUE', label: 'Quá hạn' },
]

const dailyRateOf = (reader) => reader?.finePolicy?.dailyRate ?? READER_TYPES[reader?.type]?.dailyRate ?? 0

export default function ReturnPage() {
  const toast = useToast()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const openedRef = useRef(false) // dev chạy effect 2 lần -> chỉ mở phiếu từ URL một lần

  const [tab, setTab] = useState(TABS.BORROWING)
  const [tickets, setTickets] = useState([]) // phiếu đang mượn (gồm cả quá hạn)
  const [returnedTickets, setReturnedTickets] = useState([])
  const [records, setRecords] = useState([])
  const [readers, setReaders] = useState([])
  const [loading, setLoading] = useState(true)

  // Tab Đang mượn
  const [stateFilter, setStateFilter] = useState('ALL')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)

  // Tab Lịch sử
  const [fineFilter, setFineFilter] = useState('ALL')
  const [fromDate, setFromDate] = useState('')
  const [toDate, setToDate] = useState('')
  const [hSearch, setHSearch] = useState('')
  const [hPage, setHPage] = useState(1)

  // Hộp xác nhận + kết quả
  const [selected, setSelected] = useState(null)
  const [returnDate, setReturnDate] = useState(todayIso())
  const [busy, setBusy] = useState(false)
  const [lastResult, setLastResult] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      // /api/tickets và /api/readers do phần khác làm (hợp đồng API); /api/return-records là của trả sách.
      const [t, rt, r, rd] = await Promise.all([
        returnApi.getBorrowingTickets(),
        api.get('/api/tickets?status=RETURNED'),
        returnApi.getReturnRecords(),
        api.get('/api/readers'),
      ])
      setTickets(t || [])
      setReturnedTickets(rt || [])
      setRecords(r || [])
      setReaders(rd || [])
      return t || []
    } catch (e) {
      toast.error(e.message)
      return []
    } finally {
      setLoading(false)
    }
  }, [toast])

  const openConfirm = (ticket) => {
    setSelected(ticket)
    setReturnDate(todayIso())
  }

  const closeConfirm = () => {
    if (!busy) setSelected(null)
  }

  // Tải dữ liệu, rồi mở sẵn phiếu nếu URL có ?ticketId=...
  useEffect(() => {
    load().then((list) => {
      const id = searchParams.get('ticketId')
      if (!id || openedRef.current) return
      openedRef.current = true
      const found = list.find((t) => t.id === id)
      if (found) openConfirm(found)
      else toast.error(`Phiếu ${id} không ở trạng thái đang mượn.`)
      const next = new URLSearchParams(searchParams)
      next.delete('ticketId')
      setSearchParams(next, { replace: true })
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [load])

  useEffect(() => {
    if (!selected) return
    const onKey = (e) => e.key === 'Escape' && !busy && setSelected(null)
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [selected, busy])

  const today = todayIso()
  const readerMap = useMemo(() => indexById(readers), [readers])
  const returnedMap = useMemo(() => indexById(returnedTickets), [returnedTickets])

  // ---- Tab Đang mượn ----
  const rows = useMemo(
    () => tickets.map((t) => ({ t, st: ticketState(t, today), reader: readerMap.get(t.readerId) })),
    [tickets, readerMap, today],
  )

  const counts = useMemo(() => {
    const c = { ALL: rows.length, ON_TIME: 0, DUE_TODAY: 0, OVERDUE: 0 }
    rows.forEach((r) => {
      if (c[r.st.key] !== undefined) c[r.st.key] += 1
    })
    return c
  }, [rows])

  const filteredRows = useMemo(() => {
    return rows
      .filter((r) => stateFilter === 'ALL' || r.st.key === stateFilter)
      .filter((r) => matches(search, r.t.id, r.t.readerId, r.reader?.fullName, ticketBooksText(r.t)))
      .sort((a, b) => b.st.lateDays - a.st.lateDays || (a.t.dueDate || '').localeCompare(b.t.dueDate || ''))
  }, [rows, stateFilter, search])

  // ---- Tab Lịch sử ----
  const historyRows = useMemo(() => {
    return records
      .map((r) => ({ r, ticket: returnedMap.get(r.ticketId), reader: readerMap.get(r.readerId) }))
      .filter((x) => fineFilter === 'ALL' || (fineFilter === 'FINE' ? x.r.fineAmount > 0 : !(x.r.fineAmount > 0)))
      .filter((x) => !fromDate || x.r.returnDate >= fromDate)
      .filter((x) => !toDate || x.r.returnDate <= toDate)
      .filter((x) =>
        matches(hSearch, x.r.id, x.r.ticketId, x.r.readerId, x.reader?.fullName, x.ticket ? ticketBooksText(x.ticket) : ''),
      )
      .sort((a, b) => b.r.returnDate.localeCompare(a.r.returnDate) || b.r.id.localeCompare(a.r.id))
  }, [records, returnedMap, readerMap, fineFilter, fromDate, toDate, hSearch])

  // ---- Hộp xác nhận ----
  const selReader = selected ? readerMap.get(selected.readerId) : null
  const dateError = !selected
    ? ''
    : !returnDate
      ? 'Chọn ngày trả'
      : returnDate < selected.borrowDate
        ? `Ngày trả không được trước ngày mượn (${formatDate(selected.borrowDate)})`
        : ''
  const lateDays = selected && !dateError ? Math.max(0, daysBetween(selected.dueDate, returnDate)) : 0
  const expectedFine = lateDays * dailyRateOf(selReader)

  const confirmReturn = async () => {
    setBusy(true)
    try {
      const rec = await returnApi.returnBook(selected.id, returnDate)
      setLastResult({ ...rec, copies: ticketCopies(selected), readerName: selReader?.fullName })
      setSelected(null)
      await load()
    } catch (e) {
      toast.error(e.message) // 404 không thấy phiếu, 409 đã trả, 400 ngày sai
      if (e.status === 409 || e.status === 404) {
        setSelected(null)
        await load()
      }
    } finally {
      setBusy(false)
    }
  }

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

      {lastResult && (
        <div
          className="card"
          style={{ background: lastResult.fineAmount > 0 ? 'var(--late-bg)' : 'var(--ok-bg)', marginBottom: 16 }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'flex-start' }}>
            <div>
              <strong>Đã trả phiếu {lastResult.ticketId}</strong>
              <div className="small">
                {lastResult.readerName ? `${lastResult.readerName} (${lastResult.readerId})` : lastResult.readerId} ·{' '}
                {lastResult.copies} cuốn · ngày trả {formatDate(lastResult.returnDate)}
              </div>
              <div className="small">
                {lastResult.fineAmount > 0 ? (
                  <>
                    Trễ {lastResult.overdueDays} ngày, phạt{' '}
                    <strong className="fine">{formatMoney(lastResult.fineAmount)}</strong>
                  </>
                ) : (
                  'Trả đúng hạn, không bị phạt.'
                )}{' '}
                <span className="muted">({lastResult.policyName})</span>
              </div>
            </div>
            <button className="btn btn-ghost btn-sm" onClick={() => setLastResult(null)}>
              Đóng
            </button>
          </div>
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
          <div className="stat-grid">
            {STATE_FILTERS.map((f) => (
              <button
                key={f.key}
                type="button"
                className={stateFilter === f.key ? 'stat-card active' : 'stat-card'}
                onClick={() => {
                  setStateFilter(f.key)
                  setPage(1)
                }}
              >
                <div className="stat-label">{f.label}</div>
                <div className="stat-value" style={f.key === 'OVERDUE' && counts.OVERDUE > 0 ? { color: 'var(--late)' } : undefined}>
                  {counts[f.key]}
                </div>
              </button>
            ))}
          </div>

          <div className="toolbar">
            <input
              className="input"
              style={{ width: 'min(380px, 100%)' }}
              placeholder="Tìm theo mã phiếu, bạn đọc hoặc tên sách…"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value)
                setPage(1)
              }}
            />
          </div>

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
                {!loading && filteredRows.length === 0 && (
                  <tr>
                    <td colSpan="7" className="empty">Không có phiếu nào phù hợp.</td>
                  </tr>
                )}
                {paginate(filteredRows, page, PAGE_SIZE).map(({ t, st, reader }) => (
                  <tr key={t.id}>
                    <td className="mono">{t.id}</td>
                    <td>
                      <div>{reader?.fullName ?? '—'}</div>
                      <div className="muted small mono">{t.readerId}</div>
                    </td>
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
                      <span className={st.badge}>{st.label}</span>
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
          <Pagination page={page} pageSize={PAGE_SIZE} total={filteredRows.length} onChange={setPage} unit="phiếu" />
        </section>
      )}

      {tab === TABS.HISTORY && (
        <section>
          <div className="toolbar">
            <input
              className="input"
              style={{ width: 'min(320px, 100%)' }}
              placeholder="Tìm theo mã, bạn đọc hoặc tên sách…"
              value={hSearch}
              onChange={(e) => {
                setHSearch(e.target.value)
                setHPage(1)
              }}
            />
            <select
              className="input"
              value={fineFilter}
              onChange={(e) => {
                setFineFilter(e.target.value)
                setHPage(1)
              }}
            >
              <option value="ALL">Tất cả</option>
              <option value="FINE">Có phạt</option>
              <option value="NOFINE">Không phạt</option>
            </select>
            <label className="small muted" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              Trả từ
              <input
                type="date"
                className="input"
                value={fromDate}
                onChange={(e) => {
                  setFromDate(e.target.value)
                  setHPage(1)
                }}
              />
            </label>
            <label className="small muted" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              đến
              <input
                type="date"
                className="input"
                value={toDate}
                onChange={(e) => {
                  setToDate(e.target.value)
                  setHPage(1)
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
                {!loading && historyRows.length === 0 && (
                  <tr>
                    <td colSpan="8" className="empty">Không có lượt trả nào phù hợp.</td>
                  </tr>
                )}
                {paginate(historyRows, hPage, PAGE_SIZE).map(({ r, ticket, reader }) => (
                  <tr key={r.id}>
                    <td className="mono">{r.id}</td>
                    <td>
                      <button type="button" className="link-btn mono" onClick={() => navigate(`/tickets?id=${r.ticketId}`)}>
                        {r.ticketId}
                      </button>
                    </td>
                    <td>
                      <div>{reader?.fullName ?? '—'}</div>
                      <div className="muted small mono">
                        {r.readerId}
                        {reader ? ` · ${readerTypeLabel(reader.type)}` : ''}
                      </div>
                    </td>
                    <td>
                      {ticket ? (
                        (ticket.details || []).map((d) => (
                          <div key={d.bookId}>
                            {d.bookTitle || d.bookId} <span className="muted">× {d.quantity}</span>
                          </div>
                        ))
                      ) : (
                        <span className="muted">—</span>
                      )}
                    </td>
                    <td>{formatDate(r.returnDate)}</td>
                    <td className="right">{r.overdueDays}</td>
                    <td>{r.policyName}</td>
                    <td className={r.fineAmount > 0 ? 'right fine' : 'right'}>{formatMoney(r.fineAmount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={hPage} pageSize={PAGE_SIZE} total={historyRows.length} onChange={setHPage} unit="lượt trả" />
        </section>
      )}

      {selected && (
        <div className="modal-backdrop" onClick={closeConfirm}>
          <div
            className="modal modal-wide"
            role="dialog"
            aria-modal="true"
            aria-label={`Trả phiếu ${selected.id}`}
            onClick={(e) => e.stopPropagation()}
          >
            <h3>Trả phiếu {selected.id}</h3>
            <div className="modal-body">
              <p>
                Bạn đọc <strong>{selReader?.fullName ?? selected.readerId}</strong>{' '}
                <span className="muted mono">({selected.readerId}</span>
                {selReader && <span className="muted">, {readerTypeLabel(selReader.type)}</span>}
                <span className="muted">)</span> · mượn {formatDate(selected.borrowDate)} · hạn trả{' '}
                <strong>{formatDate(selected.dueDate)}</strong>
              </p>

              <div className="table-wrap" style={{ marginBottom: 12 }}>
                <table className="table">
                  <thead>
                    <tr>
                      <th>Mã sách</th>
                      <th>Tên sách</th>
                      <th className="right">Số lượng</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(selected.details || []).map((d) => (
                      <tr key={d.bookId}>
                        <td className="mono">{d.bookId}</td>
                        <td>{d.bookTitle || '—'}</td>
                        <td className="right">{d.quantity}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <label className="field">
                <span>Ngày trả</span>
                <input
                  type="date"
                  className="input"
                  value={returnDate}
                  min={selected.borrowDate}
                  onChange={(e) => setReturnDate(e.target.value)}
                />
              </label>
              {dateError && (
                <p className="small" style={{ color: 'var(--late)', margin: '6px 0 0' }}>
                  {dateError}
                </p>
              )}

              {!dateError && (
                <div
                  className="card"
                  style={{ marginTop: 12, background: lateDays > 0 ? 'var(--late-bg)' : 'var(--ok-bg)' }}
                >
                  {lateDays > 0 ? (
                    <>
                      Trễ <strong>{lateDays} ngày</strong> × {formatMoney(dailyRateOf(selReader))}/ngày → tiền phạt dự kiến{' '}
                      <strong className="fine">{formatMoney(expectedFine)}</strong>
                    </>
                  ) : (
                    <>Trả đúng hạn, không bị phạt.</>
                  )}
                </div>
              )}
              <p className="muted small">Tiền phạt chính thức do hệ thống tính khi xác nhận.</p>
            </div>
            <div className="modal-actions">
              <button className="btn btn-ghost" onClick={closeConfirm} disabled={busy}>
                Huỷ
              </button>
              <button className="btn btn-primary" onClick={confirmReturn} disabled={!!dateError || busy}>
                {busy ? 'Đang xử lý…' : 'Xác nhận trả'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
