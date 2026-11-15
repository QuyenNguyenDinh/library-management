import { useCallback, useEffect, useMemo, useState } from 'react'
import { borrowApi } from '../api/borrowApi.js'
import { formatDate, todayIso } from '../api/client.js'
import { useToast } from '../components/Toast.jsx'

/**
 * Trang Mượn sách (TV1 - Quyền).
 * Chọn bạn đọc -> thêm sách vào phiếu -> chỉnh số lượng, ngày -> lập phiếu.
 * Hạn mức và tồn kho do backend quyết định; giao diện chỉ cảnh báo trước cho tiện.
 */

const DEFAULT_BORROW_DAYS = 14

const READER_TYPE_LABELS = {
  STUDENT: 'Sinh viên',
  PRIORITY_STUDENT: 'Sinh viên ưu tiên',
  LECTURER: 'Giảng viên',
}

function addDays(isoDate, days) {
  const [y, m, d] = isoDate.split('-').map(Number)
  const date = new Date(y, m - 1, d + days)
  const pad = (x) => String(x).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

const layout = {
  grid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 20, marginBottom: 20 },
  card: { background: 'var(--card)', border: '1px solid var(--line)', borderRadius: 'var(--radius)', padding: 18 },
  cardTitle: { margin: '0 0 12px', fontSize: '1rem', fontWeight: 600 },
  row: { display: 'flex', gap: 8, marginBottom: 12 },
  dates: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 },
  footer: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, flexWrap: 'wrap', marginTop: 14 },
  scroll: { maxHeight: 360, overflowY: 'auto' },
}

export default function BorrowPage() {
  const toast = useToast()
  const [readers, setReaders] = useState([])
  const [books, setBooks] = useState([])
  const [readerKeyword, setReaderKeyword] = useState('')
  const [bookKeyword, setBookKeyword] = useState('')
  const [readerId, setReaderId] = useState('')
  const [cart, setCart] = useState([]) // [{ bookId, title, availableQuantity, quantity }]
  const [borrowDate, setBorrowDate] = useState(todayIso())
  const [dueDate, setDueDate] = useState(addDays(todayIso(), DEFAULT_BORROW_DAYS))
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [lastTicket, setLastTicket] = useState(null)

  const loadReaders = useCallback(
    async (keyword = '') => {
      try {
        setReaders((await borrowApi.getReaders(keyword)) || [])
      } catch (e) {
        toast.error(e.message)
      }
    },
    [toast],
  )

  const loadBooks = useCallback(
    async (keyword = '') => {
      try {
        setBooks((await borrowApi.getBooks(keyword)) || [])
      } catch (e) {
        toast.error(e.message)
      }
    },
    [toast],
  )

  useEffect(() => {
    Promise.all([loadReaders(), loadBooks()]).finally(() => setLoading(false))
  }, [loadReaders, loadBooks])

  const reader = useMemo(() => readers.find((r) => r.id === readerId) || null, [readers, readerId])

  const cartTotal = cart.reduce((sum, item) => sum + (Number(item.quantity) || 0), 0)
  const remaining = reader ? reader.maxBorrowLimit - (reader.currentBorrowCount ?? 0) : null
  const overLimit = remaining !== null && cartTotal > remaining
  const overStock = cart.some((item) => Number(item.quantity) > item.availableQuantity)
  const badQuantity = cart.some((item) => !(Number(item.quantity) > 0))
  const badDates = !borrowDate || !dueDate || dueDate < borrowDate
  const canSubmit = reader && cart.length > 0 && !badQuantity && !badDates && !busy

  const addToCart = (book) => {
    setCart((prev) =>
      prev.some((item) => item.bookId === book.id)
        ? prev.map((item) => (item.bookId === book.id ? { ...item, quantity: Number(item.quantity) + 1 } : item))
        : [...prev, { bookId: book.id, title: book.title, availableQuantity: book.availableQuantity, quantity: 1 }],
    )
  }

  const setQuantity = (bookId, value) =>
    setCart((prev) => prev.map((item) => (item.bookId === bookId ? { ...item, quantity: value } : item)))

  const removeFromCart = (bookId) => setCart((prev) => prev.filter((item) => item.bookId !== bookId))

  const changeBorrowDate = (value) => {
    setBorrowDate(value)
    if (value) setDueDate(addDays(value, DEFAULT_BORROW_DAYS))
  }

  const submit = async () => {
    setBusy(true)
    try {
      const ticket = await borrowApi.borrow({
        readerId,
        items: cart.map((item) => ({ bookId: item.bookId, quantity: Number(item.quantity) })),
        borrowDate,
        dueDate,
      })
      toast.success(`Đã lập phiếu ${ticket.id} cho ${ticket.readerId}, hạn trả ${formatDate(ticket.dueDate)}.`)
      setLastTicket(ticket)
      setCart([])
      await Promise.all([loadReaders(readerKeyword), loadBooks(bookKeyword)]) // cập nhật tồn kho và số đang mượn
    } catch (e) {
      toast.error(e.message) // 400 dữ liệu sai, 404 không tồn tại, 409 hết sách / vượt hạn mức
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <h1>Mượn sách</h1>
          <p className="muted">Chọn bạn đọc, thêm sách vào phiếu rồi bấm “Lập phiếu mượn”.</p>
        </div>
      </header>

      <div style={layout.grid}>
        {/* 1. Bạn đọc và ngày mượn */}
        <section style={layout.card}>
          <h2 style={layout.cardTitle}>1. Bạn đọc</h2>
          <form
            style={layout.row}
            onSubmit={(e) => {
              e.preventDefault()
              loadReaders(readerKeyword)
            }}
          >
            <input
              className="input"
              style={{ flex: 1 }}
              placeholder="Tìm theo mã RD, họ tên hoặc SĐT…"
              value={readerKeyword}
              onChange={(e) => setReaderKeyword(e.target.value)}
            />
            <button type="submit" className="btn btn-ghost">Tìm</button>
          </form>

          <label className="field" style={{ marginTop: 0 }}>
            <span>Bạn đọc</span>
            <select className="input" value={readerId} onChange={(e) => setReaderId(e.target.value)}>
              <option value="">— Chọn bạn đọc —</option>
              {readers.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.id} — {r.fullName} ({READER_TYPE_LABELS[r.type] || r.type})
                </option>
              ))}
            </select>
          </label>

          {reader && (
            <p className="small" style={{ margin: '10px 0 0' }}>
              {READER_TYPE_LABELS[reader.type] || reader.type} · đang mượn{' '}
              <strong className="mono">
                {reader.currentBorrowCount ?? 0}/{reader.maxBorrowLimit}
              </strong>{' '}
              cuốn{' '}
              <span className={remaining > 0 ? 'badge badge-ok' : 'badge badge-late'}>
                {remaining > 0 ? `còn được mượn ${remaining}` : 'đã hết hạn mức'}
              </span>
            </p>
          )}

          <div style={layout.dates}>
            <label className="field">
              <span>Ngày mượn</span>
              <input type="date" className="input" value={borrowDate} onChange={(e) => changeBorrowDate(e.target.value)} />
            </label>
            <label className="field">
              <span>Hạn trả</span>
              <input type="date" className="input" value={dueDate} min={borrowDate} onChange={(e) => setDueDate(e.target.value)} />
            </label>
          </div>
          {badDates && <p className="small fine">Hạn trả không được trước ngày mượn.</p>}
        </section>

        {/* 2. Danh sách sách */}
        <section style={layout.card}>
          <h2 style={layout.cardTitle}>2. Sách</h2>
          <form
            style={layout.row}
            onSubmit={(e) => {
              e.preventDefault()
              loadBooks(bookKeyword)
            }}
          >
            <input
              className="input"
              style={{ flex: 1 }}
              placeholder="Tìm theo mã BK hoặc tên sách…"
              value={bookKeyword}
              onChange={(e) => setBookKeyword(e.target.value)}
            />
            <button type="submit" className="btn btn-ghost">Tìm</button>
          </form>

          <div className="table-wrap" style={layout.scroll}>
            <table className="table">
              <thead>
                <tr>
                  <th>Mã</th>
                  <th>Tên sách</th>
                  <th className="right">Còn</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {loading && (
                  <tr>
                    <td colSpan="4" className="empty">Đang tải…</td>
                  </tr>
                )}
                {!loading && books.length === 0 && (
                  <tr>
                    <td colSpan="4" className="empty">Không có sách phù hợp.</td>
                  </tr>
                )}
                {books.map((book) => {
                  const outOfStock = book.availableQuantity <= 0
                  return (
                    <tr key={book.id}>
                      <td className="mono">{book.id}</td>
                      <td>{book.title}</td>
                      <td className={outOfStock ? 'right fine mono' : 'right mono'}>{book.availableQuantity}</td>
                      <td className="right">
                        <button type="button" className="btn btn-primary btn-sm" disabled={outOfStock} onClick={() => addToCart(book)}>
                          {outOfStock ? 'Hết' : 'Thêm'}
                        </button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </section>
      </div>

      {/* 3. Phiếu mượn */}
      <section style={layout.card}>
        <h2 style={layout.cardTitle}>3. Phiếu mượn</h2>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Mã</th>
                <th>Tên sách</th>
                <th className="right">Còn trong kho</th>
                <th>Số lượng mượn</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {cart.length === 0 && (
                <tr>
                  <td colSpan="5" className="empty">Chưa có sách nào. Bấm “Thêm” ở danh sách sách.</td>
                </tr>
              )}
              {cart.map((item) => (
                <tr key={item.bookId}>
                  <td className="mono">{item.bookId}</td>
                  <td>{item.title}</td>
                  <td className="right mono">{item.availableQuantity}</td>
                  <td>
                    <input
                      type="number"
                      className="input"
                      style={{ width: 90 }}
                      min="1"
                      max={item.availableQuantity}
                      value={item.quantity}
                      onChange={(e) => setQuantity(item.bookId, e.target.value)}
                      aria-label={`Số lượng mượn ${item.bookId}`}
                    />
                  </td>
                  <td className="right">
                    <button type="button" className="btn btn-ghost btn-sm" onClick={() => removeFromCart(item.bookId)}>
                      Bỏ
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {badQuantity && <p className="small fine">Số lượng mượn mỗi đầu sách phải lớn hơn 0.</p>}
        {overStock && <p className="small fine">Có đầu sách vượt quá số lượng còn trong kho.</p>}
        {overLimit && (
          <p className="small fine">
            Tổng {cartTotal} cuốn vượt hạn mức: bạn đọc chỉ còn được mượn {Math.max(remaining, 0)} cuốn.
          </p>
        )}

        <div style={layout.footer}>
          <span className="muted">
            Tổng <strong className="mono">{cartTotal}</strong> cuốn
            {!badDates && ` · mượn ${formatDate(borrowDate)} → hạn trả ${formatDate(dueDate)}`}
          </span>
          <button type="button" className="btn btn-primary" disabled={!canSubmit} onClick={submit}>
            {busy ? 'Đang lập phiếu…' : 'Lập phiếu mượn'}
          </button>
        </div>

        {lastTicket && (
          <p className="small muted" style={{ marginBottom: 0 }}>
            Phiếu vừa lập: <strong className="mono">{lastTicket.id}</strong> —{' '}
            {lastTicket.details.map((d) => `${d.bookTitle || d.bookId} × ${d.quantity}`).join(', ')}
          </p>
        )}
      </section>
    </div>
  )
}
