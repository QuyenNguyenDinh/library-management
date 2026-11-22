
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, formatDate, formatMoney, todayIso } from '../api/client.js'
import { useToast } from '../components/Toast.jsx'
import {
  ticketCopies,
  ticketState,
} from '../utils/libraryView.js'

function periodStart(period, today) {
  if (period === 'ALL') return null

  const [year, month] = today.split('-').map(Number)

  if (period === 'THIS_MONTH') {
    return `${year}-${String(month).padStart(2, '0')}-01`
  }

  if (period === 'THREE_MONTHS') {
    const date = new Date(Date.UTC(year, month - 3, 1))
    return date.toISOString().slice(0, 10)
  }

  if (period === 'THIS_YEAR') {
    return `${year}-01-01`
  }

  return null
}

function BarRow({ label, value, max, color = 'var(--accent)' }) {
  const percent = max > 0 ? (value / max) * 100 : 0

  return (
    <div style={{ marginBottom: 14 }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          gap: 12,
          marginBottom: 6,
        }}
      >
        <span>{label}</span>
        <strong>{value}</strong>
      </div>

      <div
        style={{
          height: 12,
          background: 'var(--line)',
          borderRadius: 8,
          overflow: 'hidden',
        }}
      >
        <div
          style={{
            width: `${percent}%`,
            height: '100%',
            background: color,
            borderRadius: 8,
          }}
        />
      </div>
    </div>
  )
}

export default function StatsPage() {
  const navigate = useNavigate()
  const toast = useToast()

  const [books, setBooks] = useState([])
  const [readers, setReaders] = useState([])
  const [tickets, setTickets] = useState([])
  const [period, setPeriod] = useState('ALL')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const today = todayIso()

  useEffect(() => {
    let active = true

    async function load() {
      setLoading(true)
      setError('')

      try {
        const [bookData, readerData, ticketData] =
          await Promise.all([
            api.get('/api/books'),
            api.get('/api/readers'),
            api.get('/api/tickets'),
          ])

        if (!active) return

        setBooks(Array.isArray(bookData) ? bookData : [])
        setReaders(Array.isArray(readerData) ? readerData : [])
        setTickets(Array.isArray(ticketData) ? ticketData : [])
      } catch (err) {
        if (!active) return

        const message = err.message || 'Không thể tải thống kê'
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
  }, [toast])

  const stats = useMemo(() => {
    const start = periodStart(period, today)

    const withinPeriod = (date) =>
      Boolean(date) && (!start || date >= start) && date <= today

    const ticketRows = tickets.map((ticket) => ({
      ...ticket,
      viewState: ticketState(ticket, today),
    }))

    const activeTickets = ticketRows.filter(
      (ticket) => ticket.status !== 'RETURNED'
    )

    const overdueTickets = activeTickets
      .filter((ticket) => ticket.viewState.key === 'OVERDUE')
      .sort(
        (a, b) =>
          b.viewState.lateDays - a.viewState.lateDays
      )

    const returnedTickets = ticketRows.filter(
      (ticket) => ticket.status === 'RETURNED'
    )

    const periodTickets = ticketRows.filter((ticket) =>
      withinPeriod(ticket.borrowDate)
    )

    const totalAvailable = books.reduce(
      (sum, book) =>
        sum + Number(book.availableQuantity || 0),
      0
    )

    const borrowedCopies = activeTickets.reduce(
      (sum, ticket) => sum + ticketCopies(ticket),
      0
    )

    const totalFine = returnedTickets
      .filter((ticket) => withinPeriod(ticket.returnDate))
      .reduce(
        (sum, ticket) =>
          sum + Number(ticket.fineAmount || 0),
        0
      )

    const monthMap = new Map()

    periodTickets.forEach((ticket) => {
      const month = ticket.borrowDate.slice(0, 7)
      monthMap.set(month, (monthMap.get(month) || 0) + 1)
    })

    const monthly = [...monthMap.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .slice(-12)
      .map(([month, count]) => ({
        month,
        count,
      }))

    const stateCounts = [
      {
        label: 'Trong hạn',
        value: activeTickets.filter(
          (ticket) =>
            ticket.viewState.key === 'ON_TIME' ||
            ticket.viewState.key === 'DUE_TODAY'
        ).length,
      },
      {
        label: 'Quá hạn',
        value: overdueTickets.length,
      },
      {
        label: 'Đã trả',
        value: returnedTickets.length,
      },
    ]

    const bookMap = new Map(
      books.map((book) => [book.id, book])
    )

    const borrowedMap = new Map()

    periodTickets.forEach((ticket) => {
      ;(ticket.details || []).forEach((detail) => {
        const id = detail.bookId

        if (!id) return

        const current = borrowedMap.get(id) || {
          bookId: id,
          title:
            bookMap.get(id)?.title ||
            detail.bookTitle ||
            detail.bookName ||
            id,
          count: 0,
        }

        current.count += Number(detail.quantity || 0)
        borrowedMap.set(id, current)
      })
    })

    const topBooks = [...borrowedMap.values()]
      .sort((a, b) => b.count - a.count)
      .slice(0, 5)

    const lowStock = books
      .filter(
        (book) => Number(book.availableQuantity || 0) <= 2
      )
      .sort(
        (a, b) =>
          Number(a.availableQuantity || 0) -
          Number(b.availableQuantity || 0)
      )

    return {
      totalBooks: books.length,
      totalAvailable,
      borrowedCopies,
      totalReaders: readers.length,
      overdueCount: overdueTickets.length,
      totalFine,
      monthly,
      stateCounts,
      topBooks,
      overdueTickets: overdueTickets.slice(0, 5),
      lowStock,
    }
  }, [books, readers, tickets, period, today])

  const summaryCards = [
    { label: 'Tổng đầu sách', value: stats.totalBooks },
    { label: 'Cuốn trong kho', value: stats.totalAvailable },
    { label: 'Cuốn đang mượn', value: stats.borrowedCopies },
    { label: 'Tổng bạn đọc', value: stats.totalReaders },
    { label: 'Phiếu quá hạn', value: stats.overdueCount },
    {
      label: 'Tiền phạt đã ghi nhận',
      value: formatMoney(stats.totalFine),
    },
  ]

  const maxMonth = Math.max(
    1,
    ...stats.monthly.map((item) => item.count)
  )

  const maxState = Math.max(
    1,
    ...stats.stateCounts.map((item) => item.value)
  )

  const maxTopBook = Math.max(
    1,
    ...stats.topBooks.map((item) => item.count)
  )

  if (loading) {
    return (
      <section className="page">
        <h1>Thống kê</h1>
        <p>Đang tải thống kê...</p>
      </section>
    )
  }

  return (
    <section className="page">
      <header className="page-head">
        <h1>Thống kê</h1>

        <select
          className="input"
          value={period}
          onChange={(event) => setPeriod(event.target.value)}
          aria-label="Khoảng thời gian thống kê"
        >
          <option value="ALL">Toàn bộ</option>
          <option value="THIS_MONTH">Tháng này</option>
          <option value="THREE_MONTHS">3 tháng gần đây</option>
          <option value="THIS_YEAR">Năm nay</option>
        </select>
      </header>

      {error && (
        <p style={{ color: 'var(--late, #b91c1c)' }}>
          {error}
        </p>
      )}

      <div className="stat-grid" style={{ marginTop: 20 }}>
        {summaryCards.map((item) => (
          <div className="stat-card" key={item.label}>
            <div className="stat-label">{item.label}</div>
            <div className="stat-value">{item.value}</div>
          </div>
        ))}
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns:
            'repeat(auto-fit, minmax(min(100%, 320px), 1fr))',
          gap: 20,
          marginTop: 24,
        }}
      >
        <div className="card">
          <h2>Phiếu mượn theo tháng</h2>

          {stats.monthly.length === 0 ? (
            <p>Chưa có dữ liệu trong khoảng thời gian này.</p>
          ) : (
            <div
              style={{
                display: 'flex',
                alignItems: 'end',
                gap: 12,
                height: 240,
                overflowX: 'auto',
                paddingTop: 20,
              }}
            >
              {stats.monthly.map((item) => (
                <div
                  key={item.month}
                  style={{
                    flex: '1 0 45px',
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'end',
                    alignItems: 'center',
                    gap: 6,
                  }}
                >
                  <strong>{item.count}</strong>

                  <div
                    style={{
                      height: `${
                        (item.count / maxMonth) * 100
                      }%`,
                      minHeight: 4,
                      width: '65%',
                      background: 'var(--accent)',
                      borderRadius: '5px 5px 0 0',
                    }}
                  />

                  <small>
                    {item.month.slice(5)}/
                    {item.month.slice(0, 4)}
                  </small>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="card">
          <h2>Trạng thái phiếu</h2>

          {stats.stateCounts.map((item) => (
            <BarRow
              key={item.label}
              label={item.label}
              value={item.value}
              max={maxState}
            />
          ))}
        </div>

        <div className="card">
          <h2>Top 5 sách được mượn</h2>

          {stats.topBooks.length === 0 ? (
            <p>Chưa có dữ liệu.</p>
          ) : (
            stats.topBooks.map((item) => (
              <BarRow
                key={item.bookId}
                label={item.title}
                value={item.count}
                max={maxTopBook}
              />
            ))
          )}
        </div>
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns:
            'repeat(auto-fit, minmax(min(100%, 320px), 1fr))',
          gap: 20,
          marginTop: 24,
        }}
      >
        <div className="card">
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              gap: 12,
            }}
          >
            <h2>Phiếu quá hạn</h2>

            <button
              type="button"
              className="link-btn"
              onClick={() =>
                navigate('/tickets?status=OVERDUE')
              }
            >
              Xem tất cả
            </button>
          </div>

          {stats.overdueTickets.length === 0 ? (
            <p>Không có phiếu quá hạn.</p>
          ) : (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Mã phiếu</th>
                    <th>Hạn trả</th>
                    <th>Trạng thái</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>

                <tbody>
                  {stats.overdueTickets.map((ticket) => (
                    <tr key={ticket.id}>
                      <td>
                        <button
                          type="button"
                          className="link-btn"
                          onClick={() =>
                            navigate(
                              `/tickets?id=${encodeURIComponent(
                                ticket.id
                              )}`
                            )
                          }
                        >
                          {ticket.id}
                        </button>
                      </td>

                      <td>{formatDate(ticket.dueDate)}</td>

                      <td>
                        <span className="badge badge-late">
                          {ticket.viewState.label}
                        </span>
                      </td>

                      <td>
                        <button
                          type="button"
                          className="btn btn-primary btn-sm"
                          onClick={() =>
                            navigate(
                              `/return?ticketId=${encodeURIComponent(
                                ticket.id
                              )}`
                            )
                          }
                        >
                          Trả sách
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div className="card">
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              gap: 12,
            }}
          >
            <h2>Sách sắp hết</h2>

            <button
              type="button"
              className="link-btn"
              onClick={() => navigate('/books')}
            >
              Mở trang Sách
            </button>
          </div>

          {stats.lowStock.length === 0 ? (
            <p>Không có sách sắp hết.</p>
          ) : (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Mã sách</th>
                    <th>Tên sách</th>
                    <th>Còn lại</th>
                  </tr>
                </thead>

                <tbody>
                  {stats.lowStock.map((book) => {
                    const quantity = Number(
                      book.availableQuantity || 0
                    )

                    return (
                      <tr key={book.id}>
                        <td>{book.id}</td>
                        <td>{book.title}</td>
                        <td>
                          <span
                            className={
                              quantity === 0
                                ? 'badge badge-late'
                                : 'badge badge-warn'
                            }
                          >
                            {quantity === 0
                              ? 'Hết sách'
                              : `Còn ${quantity}`}
                          </span>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </section>
  )
}
