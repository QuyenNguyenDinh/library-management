
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { api, formatDate, formatMoney, todayIso } from '../api/client.js'
import { useToast } from '../components/Toast.jsx'
import TicketDetailModal from '../components/TicketDetailModal.jsx'
import Pagination, { paginate } from '../components/Pagination.jsx'
import {
  indexById,
  matches,
  ticketBooksText,
  ticketCopies,
  ticketState,
} from '../utils/libraryView.js'

const PAGE_SIZE = 10

const STATUS_OPTIONS = [
  { value: '', label: 'Mọi trạng thái' },
  { value: 'BORROWING', label: 'Đang mượn (trong hạn)' },
  { value: 'OVERDUE', label: 'Quá hạn' },
  { value: 'RETURNED', label: 'Đã trả' },
]

export default function TicketsPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const toast = useToast()

  const [tickets, setTickets] = useState([])
  const [readers, setReaders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  const [keyword, setKeyword] = useState('')
  const [fromDate, setFromDate] = useState('')
  const [toDate, setToDate] = useState('')
  const [page, setPage] = useState(1)

  const statusParam = searchParams.get('status') || ''
  const status = STATUS_OPTIONS.some(
    (option) => option.value === statusParam
  )
    ? statusParam
    : ''

  const selectedId = searchParams.get('id')
  const today = todayIso()

  useEffect(() => {
    let active = true

    async function load() {
      setLoading(true)
      setError('')

      try {
        const [ticketData, readerData] = await Promise.all([
          api.get('/api/tickets'),
          api.get('/api/readers'),
        ])

        if (!active) return

        setTickets(Array.isArray(ticketData) ? ticketData : [])
        setReaders(Array.isArray(readerData) ? readerData : [])
      } catch (err) {
        if (!active) return

        const message = err.message || 'Không thể tải dữ liệu'
        setError(message)
        toast.error(message)
      } finally {
        if (active) setLoading(false)
      }
    }

    load()

    return () => {
      active = false
    }
  }, [toast, reloadKey])

  const readersById = useMemo(
    () => indexById(readers),
    [readers]
  )

  const ticketRows = useMemo(
    () =>
      tickets.map((ticket) => ({
        ...ticket,
        viewState: ticketState(ticket, today),
        reader: readersById.get(ticket.readerId) || null,
      })),
    [tickets, readersById, today]
  )

  const counts = useMemo(
    () => ({
      ALL: ticketRows.length,
      BORROWING: ticketRows.filter(
        (ticket) =>
          ticket.viewState.key === 'ON_TIME' ||
          ticket.viewState.key === 'DUE_TODAY'
      ).length,
      OVERDUE: ticketRows.filter(
        (ticket) => ticket.viewState.key === 'OVERDUE'
      ).length,
      RETURNED: ticketRows.filter(
        (ticket) => ticket.viewState.key === 'RETURNED'
      ).length,
    }),
    [ticketRows]
  )

  const filtered = useMemo(() => {
    return ticketRows
      .filter((ticket) => {
        const key = ticket.viewState.key

        if (status === 'BORROWING') {
          if (key !== 'ON_TIME' && key !== 'DUE_TODAY') {
            return false
          }
        } else if (status && key !== status) {
          return false
        }

        if (
          !matches(
            keyword,
            ticket.id,
            ticket.readerId,
            ticket.reader?.fullName,
            ticketBooksText(ticket)
          )
        ) {
          return false
        }

        if (fromDate && ticket.borrowDate < fromDate) {
          return false
        }

        if (toDate && ticket.borrowDate > toDate) {
          return false
        }

        return true
      })
      .sort((a, b) => {
        const dateOrder = (b.borrowDate || '').localeCompare(
          a.borrowDate || ''
        )

        if (dateOrder !== 0) return dateOrder

        return String(b.id).localeCompare(String(a.id))
      })
  }, [ticketRows, status, keyword, fromDate, toDate])

  const visibleRows = paginate(filtered, page, PAGE_SIZE)

  const selectedTicket =
    ticketRows.find((ticket) => ticket.id === selectedId) || null

  const hasFilters = Boolean(
    status || keyword || fromDate || toDate
  )

  function updateParams(changes) {
    const next = new URLSearchParams(searchParams)

    Object.entries(changes).forEach(([key, value]) => {
      if (value) {
        next.set(key, value)
      } else {
        next.delete(key)
      }
    })

    setSearchParams(next)
  }

  function changeStatus(value) {
    updateParams({ status: value })
    setPage(1)
  }

  function clearFilters() {
    setKeyword('')
    setFromDate('')
    setToDate('')
    updateParams({ status: '' })
    setPage(1)
  }

  function openDetail(ticket) {
    updateParams({ id: ticket.id })
  }

  function closeDetail() {
    updateParams({ id: '' })
  }

  function goToReturn(ticket) {
    navigate(
      `/return?ticketId=${encodeURIComponent(ticket.id)}`
    )
  }

  return (
    <section className="page">
      <header className="page-head">
        <h1>Phiếu mượn</h1>
      </header>

      <div className="stat-grid">
        {[
          {
            key: '',
            label: 'Tổng phiếu',
            value: counts.ALL,
          },
          {
            key: 'BORROWING',
            label: 'Đang mượn (trong hạn)',
            value: counts.BORROWING,
          },
          {
            key: 'OVERDUE',
            label: 'Quá hạn',
            value: counts.OVERDUE,
          },
          {
            key: 'RETURNED',
            label: 'Đã trả',
            value: counts.RETURNED,
          },
        ].map((item) => (
          <button
            key={item.label}
            type="button"
            className={`stat-card ${
              status === item.key ? 'active' : ''
            }`}
            onClick={() => changeStatus(item.key)}
          >
            <div className="stat-label">{item.label}</div>
            <div className={`stat-value ${item.key === 'OVERDUE' && item.value > 0 ? 'fine' : ''}`}>
              {loading ? '…' : item.value}
            </div>
          </button>
        ))}
      </div>

      <div className="toolbar" style={{ marginTop: 20 }}>
        <input
          className="input"
          type="search"
          placeholder="Tìm mã phiếu, bạn đọc, tên sách..."
          value={keyword}
          onChange={(event) => {
            setKeyword(event.target.value)
            setPage(1)
          }}
        />

        <select
          className="input"
          value={status}
          onChange={(event) =>
            changeStatus(event.target.value)
          }
        >
          {STATUS_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>

        <label>
          Mượn từ
          <input
            className="input"
            type="date"
            value={fromDate}
            onChange={(event) => {
              setFromDate(event.target.value)
              setPage(1)
            }}
          />
        </label>

        <label>
          Đến
          <input
            className="input"
            type="date"
            value={toDate}
            onChange={(event) => {
              setToDate(event.target.value)
              setPage(1)
            }}
          />
        </label>

        {hasFilters && (
          <button
            type="button"
            className="btn btn-ghost"
            onClick={clearFilters}
          >
            Xóa bộ lọc
          </button>
        )}
      </div>

      {error && (
        <div className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12 }}>
          <span style={{ color: 'var(--late)' }}>{error}</span>
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => setReloadKey((key) => key + 1)}>
            Thử lại
          </button>
        </div>
      )}

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Mã phiếu</th>
              <th>Bạn đọc</th>
              <th style={{ textAlign: 'right' }}>Số cuốn</th>
              <th>Ngày mượn</th>
              <th>Hạn trả</th>
              <th>Trạng thái</th>
              <th style={{ textAlign: 'right' }}>Tiền phạt</th>
              <th aria-label="Thao tác" />
            </tr>
          </thead>

          <tbody>
            {loading ? (
              <tr>
                <td colSpan={8} className="empty">
                  Đang tải dữ liệu...
                </td>
              </tr>
            ) : visibleRows.length === 0 ? (
              <tr>
                <td colSpan={8} className="empty">
                  {hasFilters ? 'Không có phiếu nào khớp bộ lọc.' : 'Chưa có phiếu mượn nào.'}
                </td>
              </tr>
            ) : (
              visibleRows.map((ticket) => (
                <tr key={ticket.id}>
                  <td>
                    <button
                      type="button"
                      className="link-btn mono"
                      onClick={() => openDetail(ticket)}
                    >
                      {ticket.id}
                    </button>
                  </td>

                  <td>
                    <strong>
                      {ticket.reader?.fullName ||
                        ticket.readerId}
                    </strong>
                    <div className="muted">
                      {ticket.readerId}
                    </div>
                  </td>

                  <td style={{ textAlign: 'right' }}>{ticketCopies(ticket)}</td>

                  <td>{formatDate(ticket.borrowDate)}</td>
                  <td>{formatDate(ticket.dueDate)}</td>

                  <td>
                    <span className={ticket.viewState.badge}>
                      {ticket.viewState.label}
                    </span>
                  </td>

                  <td style={{ textAlign: 'right' }}>
                    {ticket.status === 'RETURNED'
                      ? formatMoney(ticket.fineAmount)
                      : '—'}
                  </td>

                  <td>
                    <div
                      style={{
                        display: 'flex',
                        gap: 8,
                        flexWrap: 'wrap',
                      }}
                    >
                      <button
                        type="button"
                        className="btn btn-ghost btn-sm"
                        onClick={() => openDetail(ticket)}
                      >
                        Chi tiết
                      </button>

                      {ticket.status !== 'RETURNED' && (
                        <button
                          type="button"
                          className="btn btn-primary btn-sm"
                          onClick={() => goToReturn(ticket)}
                        >
                          Trả sách
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {!loading && (
        <Pagination
          page={page}
          pageSize={PAGE_SIZE}
          total={filtered.length}
          onChange={setPage}
          unit="phiếu"
        />
      )}

      <TicketDetailModal
        ticket={selectedTicket}
        reader={selectedTicket?.reader || null}
        onClose={closeDetail}
        onReturn={goToReturn}
      />
    </section>
  )
}
