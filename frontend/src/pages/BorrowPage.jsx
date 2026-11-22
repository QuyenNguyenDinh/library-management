import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { borrowApi } from '../api/borrowApi.js'
import { formatDate, todayIso } from '../api/client.js'
import ConfirmModal from '../components/ConfirmModal.jsx'
import { useToast } from '../components/Toast.jsx'

/**
 * Trang Mượn sách (TV1 - Quyền).
 * Bên trái: tìm và chọn bạn đọc, tìm và thêm sách.
 * Bên phải: "Phiếu đang soạn" (dính khi cuộn) gồm bạn đọc, ngày mượn, hạn trả, danh sách sách và nút lập phiếu.
 * Giao diện kiểm tra trước hạn mức và tồn kho; backend vẫn là nơi quyết định cuối cùng.
 */

const QUICK_DURATIONS = [7, 14, 30]
const DEFAULT_DURATION = 14

const READER_TYPE_LABELS = {
  STUDENT: 'Sinh viên',
  PRIORITY_STUDENT: 'Sinh viên ưu tiên',
  LECTURER: 'Giảng viên',
}

const WEEKDAYS = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy']

// Màu "trong hạn" lấy biến --ok nếu có, không thì dùng màu nhấn
const OK_COLOR = 'var(--ok, var(--accent))'
const OK_BG = 'var(--ok-bg)'

function parseIso(iso) {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d)
}

function addDays(iso, days) {
  const date = parseIso(iso)
  date.setDate(date.getDate() + days)
  const pad = (x) => String(x).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function daysBetween(fromIso, toIso) {
  return Math.round((parseIso(toIso) - parseIso(fromIso)) / 86400000)
}

function longDate(iso) {
  return `${WEEKDAYS[parseIso(iso).getDay()]}, ${formatDate(iso)}`
}

function initials(name = '') {
  const words = name.trim().split(/\s+/)
  const last = words[words.length - 1] || ''
  const first = words.length > 1 ? words[0] : ''
  return (first.charAt(0) + last.charAt(0)).toUpperCase() || '?'
}

const ui = {
  layout: { display: 'flex', gap: 20, alignItems: 'flex-start', flexWrap: 'wrap' },
  left: { flex: '1 1 560px', minWidth: 0, display: 'grid', gap: 20 },
  right: { flex: '0 1 380px', minWidth: 300, position: 'sticky', top: 20 },
  card: { background: 'var(--card)', border: '1px solid var(--line)', borderRadius: 'var(--radius)', padding: 18 },
  sectionTitle: { margin: '0 0 12px', fontSize: '0.8rem', fontWeight: 600, letterSpacing: '0.06em', color: 'var(--muted)' },
  row: { display: 'flex', gap: 8 },
  between: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 },
  scroll: { maxHeight: 280, overflowY: 'auto' },
  notice: { padding: '10px 12px', borderRadius: 'var(--radius)', fontSize: '0.88rem' },
  avatar: {
    width: 44, height: 44, borderRadius: '50%', display: 'grid', placeItems: 'center', flexShrink: 0,
    background: 'var(--spine)', color: 'var(--spine-ink)', fontWeight: 600,
  },
  barTrack: { height: 8, borderRadius: 999, background: 'var(--line)', overflow: 'hidden', display: 'flex' },
  qtyBtn: { width: 30, padding: '4px 0' },
  divider: { border: 0, borderTop: '1px solid var(--line)', margin: '14px 0' },
}

function StateBlock({ loading, error, onRetry, loadingText }) {
  if (loading) return <p className="muted small">{loadingText}</p>
  if (error) {
    return (
      <div style={{ ...ui.notice, ...ui.between, background: 'var(--late-bg)', color: 'var(--late)' }}>
        <span>{error}</span>
        <button type="button" className="btn btn-ghost btn-sm" onClick={onRetry}>Thử lại</button>
      </div>
    )
  }
  return null
}

export default function BorrowPage() {
  const toast = useToast()
  const navigate = useNavigate()

  // Bạn đọc
  const [readers, setReaders] = useState([])
  const [readersLoading, setReadersLoading] = useState(true)
  const [readersError, setReadersError] = useState('')
  const [readerKeyword, setReaderKeyword] = useState('')
  const [readerQuery, setReaderQuery] = useState('')
  const [reader, setReader] = useState(null) // lưu riêng đối tượng, không phụ thuộc kết quả tìm kiếm
  const [pendingReader, setPendingReader] = useState(null) // { next: reader | null } khi cần xác nhận đổi

  // Sách
  const [books, setBooks] = useState([])
  const [booksLoading, setBooksLoading] = useState(true)
  const [booksError, setBooksError] = useState('')
  const [bookKeyword, setBookKeyword] = useState('')
  const [bookQuery, setBookQuery] = useState('')

  // Phiếu đang soạn
  const [cart, setCart] = useState([]) // [{ bookId, title, availableQuantity, quantity }]
  const [borrowDate, setBorrowDate] = useState(todayIso())
  const [duration, setDuration] = useState(DEFAULT_DURATION) // null khi hạn trả được chọn tay
  const [dueDate, setDueDate] = useState(addDays(todayIso(), DEFAULT_DURATION))
  const [busy, setBusy] = useState(false)
  const [serverIssue, setServerIssue] = useState(null) // { bookId?, message } từ lỗi 409
  const [lastTicket, setLastTicket] = useState(null)

  const loadReaders = useCallback(
    async (keyword) => {
      setReadersLoading(true)
      setReadersError('')
      try {
        setReaders((await borrowApi.getReaders(keyword)) || [])
        setReaderQuery(keyword)
      } catch (e) {
        setReadersError(e.message || 'Không tải được danh sách bạn đọc.')
      } finally {
        setReadersLoading(false)
      }
    },
    [],
  )

  const loadBooks = useCallback(
    async (keyword) => {
      setBooksLoading(true)
      setBooksError('')
      try {
        setBooks((await borrowApi.getBooks(keyword)) || [])
        setBookQuery(keyword)
      } catch (e) {
        setBooksError(e.message || 'Không tải được danh sách sách.')
      } finally {
        setBooksLoading(false)
      }
    },
    [],
  )

  useEffect(() => {
    loadReaders('')
    loadBooks('')
  }, [loadReaders, loadBooks])

  // Cập nhật lại tồn kho của các dòng trong phiếu mỗi khi danh sách sách được tải lại
  useEffect(() => {
    const stock = new Map(books.map((b) => [b.id, b.availableQuantity]))
    setCart((prev) =>
      prev.some((item) => stock.has(item.bookId) && stock.get(item.bookId) !== item.availableQuantity)
        ? prev.map((item) => (stock.has(item.bookId) ? { ...item, availableQuantity: stock.get(item.bookId) } : item))
        : prev,
    )
  }, [books])

  /** Tải lại bạn đọc đang chọn và tồn kho sau khi lập phiếu hoặc khi backend báo lỗi. */
  const refreshData = useCallback(async () => {
    const tasks = [loadBooks(bookQuery), loadReaders(readerQuery)]
    if (reader) {
      tasks.push(
        borrowApi
          .getReader(reader.id)
          .then(setReader)
          .catch(() => {}),
      )
    }
    await Promise.all(tasks)
  }, [loadBooks, loadReaders, bookQuery, readerQuery, reader])

  // ===================== Bạn đọc =====================

  const requestReader = (next) => {
    // Chỉ hỏi khi đang có bạn đọc khác và phiếu đã có sách
    if (reader && cart.length > 0 && (next === null || next.id !== reader.id)) {
      setPendingReader({ next })
      return
    }
    setReader(next)
    setServerIssue(null)
  }

  const confirmReaderChange = () => {
    setCart([])
    setReader(pendingReader.next)
    setServerIssue(null)
    setPendingReader(null)
  }

  // ===================== Phiếu đang soạn =====================

  const cartQty = (bookId) => cart.find((item) => item.bookId === bookId)?.quantity || 0

  const addToCart = (book) => {
    setLastTicket(null)
    setServerIssue(null)
    const chosen = cartQty(book.id)
    if (chosen >= book.availableQuantity) {
      toast.error(`"${book.title}" chỉ còn ${book.availableQuantity} cuốn, đã chọn đủ.`)
      return
    }
    setCart((prev) =>
      chosen > 0
        ? prev.map((item) => (item.bookId === book.id ? { ...item, quantity: item.quantity + 1 } : item))
        : [...prev, { bookId: book.id, title: book.title, availableQuantity: book.availableQuantity, quantity: 1 }],
    )
  }

  const setQuantity = (bookId, quantity) => {
    setServerIssue((issue) => (issue?.bookId === bookId ? null : issue))
    setCart((prev) => prev.map((item) => (item.bookId === bookId ? { ...item, quantity } : item)))
  }

  const stepQuantity = (item, delta) => {
    const current = Number(item.quantity) || 0
    const next = Math.min(Math.max(current + delta, 1), Math.max(item.availableQuantity, 1))
    setQuantity(item.bookId, next)
  }

  const removeFromCart = (bookId) => {
    setServerIssue((issue) => (issue?.bookId === bookId ? null : issue))
    setCart((prev) => prev.filter((item) => item.bookId !== bookId))
  }

  const changeBorrowDate = (value) => {
    setBorrowDate(value)
    // Chỉ dời hạn trả theo khi đang dùng thời hạn nhanh; hạn trả chọn tay thì giữ nguyên
    if (value && duration !== null) setDueDate(addDays(value, duration))
  }

  const chooseDuration = (days) => {
    setDuration(days)
    if (borrowDate) setDueDate(addDays(borrowDate, days))
  }

  const changeDueDate = (value) => {
    setDueDate(value)
    setDuration(null)
  }

  // ===================== Kiểm tra trước khi gửi =====================

  const totalTitles = cart.length
  const totalCopies = cart.reduce((sum, item) => sum + (Number(item.quantity) || 0), 0)
  const used = reader?.currentBorrowCount ?? 0
  const limit = reader?.maxBorrowLimit ?? 0
  const remaining = Math.max(limit - used, 0)
  const overLimit = !!reader && totalCopies > remaining

  const lineErrors = useMemo(() => {
    const errors = {}
    for (const item of cart) {
      const qty = Number(item.quantity)
      if (!Number.isInteger(qty) || qty <= 0) errors[item.bookId] = 'Số lượng phải là số nguyên lớn hơn 0.'
      else if (qty > item.availableQuantity) {
        errors[item.bookId] =
          item.availableQuantity > 0 ? `Chỉ còn ${item.availableQuantity} cuốn trong kho.` : 'Sách đã hết trong kho.'
      } else if (serverIssue?.bookId === item.bookId) errors[item.bookId] = serverIssue.message
    }
    return errors
  }, [cart, serverIssue])

  const hasLineError = Object.keys(lineErrors).length > 0
  const badDates = !borrowDate || !dueDate || dueDate < borrowDate
  const durationDays = !badDates ? daysBetween(borrowDate, dueDate) : null

  const blockReason = !reader
    ? 'Chọn bạn đọc để lập phiếu.'
    : cart.length === 0
      ? 'Thêm ít nhất một cuốn sách.'
      : badDates
        ? 'Hạn trả không được trước ngày mượn.'
        : overLimit
          ? `Vượt hạn mức: bạn đọc chỉ còn được mượn ${remaining} cuốn.`
          : hasLineError
            ? 'Sửa các dòng sách đang báo lỗi.'
            : ''
  const canSubmit = !blockReason && !busy

  const submit = async () => {
    if (!canSubmit) return
    setBusy(true)
    setServerIssue(null)
    try {
      const ticket = await borrowApi.borrow({
        readerId: reader.id,
        items: cart.map((item) => ({ bookId: item.bookId, quantity: Number(item.quantity) })),
        borrowDate,
        dueDate,
      })
      toast.success(`Đã lập phiếu ${ticket.id}.`)
      setLastTicket({ ...ticket, readerName: reader.fullName })
      setCart([])
      await refreshData()
    } catch (e) {
      if (e.status === 409) {
        const match = /Sách '([^']+)'/.exec(e.message || '')
        setServerIssue(match ? { bookId: match[1], message: e.message } : { message: e.message })
        await refreshData() // tồn kho hoặc số đang mượn đã đổi, tải lại để hiển thị đúng
      } else if (e.status === 404) {
        await refreshData()
      }
      toast.error(e.message)
    } finally {
      setBusy(false)
    }
  }

  const startNewTicket = () => {
    setLastTicket(null)
    setReader(null)
    setBorrowDate(todayIso())
    setDuration(DEFAULT_DURATION)
    setDueDate(addDays(todayIso(), DEFAULT_DURATION))
  }

  const reloadAll = () => {
    setServerIssue(null)
    refreshData()
  }

  // ===================== Giao diện =====================

  const renderReaderPicker = () => (
    <>
      <form
        style={{ ...ui.row, marginBottom: 12 }}
        onSubmit={(e) => {
          e.preventDefault()
          loadReaders(readerKeyword.trim())
        }}
      >
        <input
          className="input"
          style={{ flex: 1 }}
          placeholder="Tìm theo mã RD, họ tên hoặc số điện thoại…"
          value={readerKeyword}
          onChange={(e) => setReaderKeyword(e.target.value)}
        />
        <button type="submit" className="btn btn-ghost">Tìm</button>
      </form>

      <StateBlock
        loading={readersLoading}
        error={readersError}
        onRetry={() => loadReaders(readerQuery)}
        loadingText="Đang tải bạn đọc…"
      />

      {!readersLoading && !readersError && readers.length === 0 && (
        <p className="muted small">
          Không có bạn đọc nào khớp “{readerQuery}”. Thử tìm theo mã (RD001) hoặc một phần họ tên.
        </p>
      )}

      {!readersLoading && !readersError && readers.length > 0 && (
        <div className="table-wrap" style={ui.scroll}>
          <table className="table">
            <tbody>
              {readers.map((r) => {
                const full = (r.currentBorrowCount ?? 0) >= r.maxBorrowLimit
                return (
                  <tr key={r.id}>
                    <td className="mono">{r.id}</td>
                    <td>
                      <div style={{ fontWeight: 500 }}>{r.fullName}</div>
                      <div className="muted small">
                        {READER_TYPE_LABELS[r.type] || r.type} · {r.phone}
                      </div>
                    </td>
                    <td className="right">
                      <span className={full ? 'badge badge-late' : 'badge'}>
                        {r.currentBorrowCount ?? 0}/{r.maxBorrowLimit}
                      </span>
                    </td>
                    <td className="right">
                      <button type="button" className="btn btn-primary btn-sm" onClick={() => requestReader(r)}>
                        Chọn
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </>
  )

  const renderReaderCard = () => {
    const projected = Math.min(used + totalCopies, Math.max(limit, 1))
    const pct = (n) => `${Math.min((n / Math.max(limit, 1)) * 100, 100)}%`
    return (
      <div>
        <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
          <div style={ui.avatar}>{initials(reader.fullName)}</div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontWeight: 600, fontSize: '1.05rem' }}>{reader.fullName}</div>
            <div className="muted small">
              <span className="mono">{reader.id}</span> · {reader.phone}
            </div>
          </div>
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => requestReader(null)}>
            Đổi bạn đọc
          </button>
        </div>

        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', margin: '12px 0' }}>
          <span className="badge">{READER_TYPE_LABELS[reader.type] || reader.type}</span>
          {reader.finePolicy?.dailyRate != null && (
            <span className="badge">
              Phạt trễ {Number(reader.finePolicy.dailyRate).toLocaleString('vi-VN')}đ/ngày
            </span>
          )}
        </div>

        <div style={ui.barTrack} aria-hidden="true">
          <div style={{ width: pct(used), background: 'var(--muted)' }} />
          <div
            style={{
              width: `calc(${pct(projected)} - ${pct(used)})`,
              background: overLimit ? 'var(--late)' : 'var(--accent)',
            }}
          />
        </div>
        <div className="small" style={{ ...ui.between, marginTop: 6 }}>
          <span>
            Đang mượn <strong>{used}</strong>/{limit} cuốn
          </span>
          <span className={remaining === 0 ? 'fine' : 'muted'}>Còn được mượn {remaining} cuốn</span>
        </div>
      </div>
    )
  }

  const renderBooks = () => (
    <>
      <form
        style={{ ...ui.row, marginBottom: 12 }}
        onSubmit={(e) => {
          e.preventDefault()
          loadBooks(bookKeyword.trim())
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

      <StateBlock
        loading={booksLoading}
        error={booksError}
        onRetry={() => loadBooks(bookQuery)}
        loadingText="Đang tải sách…"
      />

      {!booksLoading && !booksError && books.length === 0 && (
        <p className="muted small">
          Không tìm thấy sách nào khớp “{bookQuery}”. Thử từ khóa ngắn hơn, bỏ dấu, hoặc tìm theo mã (BK001).
        </p>
      )}

      {!booksLoading && !booksError && books.length > 0 && (
        <div className="table-wrap" style={{ maxHeight: 420, overflowY: 'auto' }}>
          <table className="table">
            <thead>
              <tr>
                <th>Mã</th>
                <th>Tên sách</th>
                <th>Tác giả</th>
                <th>Thể loại</th>
                <th className="right">Còn</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {books.map((b) => {
                const out = b.availableQuantity <= 0
                const chosen = cartQty(b.id)
                const full = chosen >= b.availableQuantity
                return (
                  <tr key={b.id} style={serverIssue?.bookId === b.id ? { background: 'var(--late-bg)' } : undefined}>
                    <td className="mono">{b.id}</td>
                    <td style={{ fontWeight: 500 }}>{b.title}</td>
                    <td className="muted">{b.author}</td>
                    <td className="muted small">{b.category}</td>
                    <td className="right">
                      {out ? <span className="badge badge-late">Hết sách</span> : <span className="mono">{b.availableQuantity}</span>}
                    </td>
                    <td className="right" style={{ whiteSpace: 'nowrap' }}>
                      {chosen > 0 ? (
                        <span className="badge" style={{ background: OK_BG, color: OK_COLOR }}>
                          Đã chọn × {chosen}
                        </span>
                      ) : (
                        <button
                          type="button"
                          className="btn btn-primary btn-sm"
                          disabled={out || full}
                          onClick={() => addToCart(b)}
                        >
                          {out ? 'Hết' : 'Thêm'}
                        </button>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </>
  )

  const renderDraft = () => (
    <div style={ui.card}>
      <h2 style={ui.sectionTitle}>PHIẾU ĐANG SOẠN</h2>

      {reader ? (
        <div style={ui.between}>
          <div>
            <div style={{ fontWeight: 600 }}>{reader.fullName}</div>
            <div className="muted small">
              <span className="mono">{reader.id}</span> · {READER_TYPE_LABELS[reader.type] || reader.type}
            </div>
          </div>
          <span className={overLimit ? 'badge badge-late' : 'badge'}>
            Còn {Math.max(remaining - totalCopies, 0)}/{limit}
          </span>
        </div>
      ) : (
        <p className="muted small" style={{ margin: 0 }}>Chưa chọn bạn đọc.</p>
      )}

      <hr style={ui.divider} />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
        <label className="field" style={{ marginTop: 0 }}>
          <span>Ngày mượn</span>
          <input type="date" className="input" value={borrowDate} onChange={(e) => changeBorrowDate(e.target.value)} />
        </label>
        <label className="field" style={{ marginTop: 0 }}>
          <span>Hạn trả</span>
          <input type="date" className="input" value={dueDate} min={borrowDate} onChange={(e) => changeDueDate(e.target.value)} />
        </label>
      </div>
      <div style={{ ...ui.row, marginTop: 8 }}>
        {QUICK_DURATIONS.map((days) => (
          <button
            key={days}
            type="button"
            className={duration === days ? 'btn btn-primary btn-sm' : 'btn btn-ghost btn-sm'}
            onClick={() => chooseDuration(days)}
          >
            {days} ngày
          </button>
        ))}
      </div>
      <p className="small" style={{ margin: '8px 0 0' }}>
        {badDates ? (
          <span className="fine">Hạn trả không được trước ngày mượn.</span>
        ) : (
          <>
            Hạn trả: <strong>{longDate(dueDate)}</strong> <span className="muted">({durationDays} ngày)</span>
          </>
        )}
      </p>

      <hr style={ui.divider} />

      {cart.length === 0 ? (
        <p className="muted small" style={{ margin: 0 }}>Chưa có sách. Bấm “Thêm” ở danh sách sách bên trái.</p>
      ) : (
        <div style={{ display: 'grid', gap: 10 }}>
          {cart.map((item) => {
            const error = lineErrors[item.bookId]
            return (
              <div
                key={item.bookId}
                style={{
                  padding: 10,
                  borderRadius: 'var(--radius)',
                  border: `1px solid ${error ? 'var(--late)' : 'var(--line)'}`,
                  background: error ? 'var(--late-bg)' : 'transparent',
                }}
              >
                <div style={{ fontWeight: 500 }}>{item.title}</div>
                <div style={{ ...ui.between, marginTop: 6 }}>
                  <span className="muted small">
                    <span className="mono">{item.bookId}</span> · còn {item.availableQuantity}
                  </span>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                    <button
                      type="button"
                      className="btn btn-ghost btn-sm"
                      style={ui.qtyBtn}
                      aria-label={`Giảm số lượng ${item.bookId}`}
                      disabled={Number(item.quantity) <= 1}
                      onClick={() => stepQuantity(item, -1)}
                    >
                      −
                    </button>
                    <input
                      type="number"
                      className="input mono"
                      style={{ width: 52, padding: '4px 6px', textAlign: 'center' }}
                      min="1"
                      max={item.availableQuantity}
                      value={item.quantity}
                      aria-label={`Số lượng ${item.bookId}`}
                      onChange={(e) => setQuantity(item.bookId, e.target.value === '' ? '' : Number(e.target.value))}
                    />
                    <button
                      type="button"
                      className="btn btn-ghost btn-sm"
                      style={ui.qtyBtn}
                      aria-label={`Tăng số lượng ${item.bookId}`}
                      disabled={Number(item.quantity) >= item.availableQuantity}
                      onClick={() => stepQuantity(item, 1)}
                    >
                      +
                    </button>
                    <button
                      type="button"
                      className="btn btn-ghost btn-sm"
                      style={{ color: 'var(--late)', marginLeft: 4 }}
                      onClick={() => removeFromCart(item.bookId)}
                    >
                      Xóa
                    </button>
                  </div>
                </div>
                {error && <p className="small fine" style={{ margin: '6px 0 0' }}>{error}</p>}
              </div>
            )
          })}
        </div>
      )}

      <hr style={ui.divider} />

      <div className="small" style={ui.between}>
        <span>
          <strong>{totalTitles}</strong> đầu sách · <strong>{totalCopies}</strong> cuốn
        </span>
        {reader && (
          <span className={overLimit ? 'fine' : 'muted'}>
            {overLimit ? `Vượt hạn mức ${totalCopies - remaining} cuốn` : `Hạn mức còn lại ${remaining - totalCopies} cuốn`}
          </span>
        )}
      </div>

      {serverIssue && !serverIssue.bookId && (
        <div style={{ ...ui.notice, marginTop: 10, background: 'var(--late-bg)', color: 'var(--late)' }}>
          {serverIssue.message}
        </div>
      )}

      <button
        type="button"
        className="btn btn-primary"
        style={{ width: '100%', marginTop: 14, padding: '11px 16px', fontWeight: 600 }}
        disabled={!canSubmit}
        onClick={submit}
      >
        {busy ? 'Đang lập phiếu…' : 'Lập phiếu mượn'}
      </button>
      {blockReason && <p className="muted small" style={{ margin: '8px 0 0', textAlign: 'center' }}>{blockReason}</p>}
    </div>
  )

  const renderResult = () => (
    <div style={{ ...ui.card, borderColor: OK_COLOR }}>
      <h2 style={{ ...ui.sectionTitle, color: OK_COLOR }}>ĐÃ LẬP PHIẾU</h2>
      <div className="mono" style={{ fontSize: '1.6rem', fontWeight: 700 }}>{lastTicket.id}</div>
      <p style={{ margin: '6px 0 12px' }}>
        {lastTicket.readerName} <span className="muted mono small">({lastTicket.readerId})</span>
      </p>
      <div style={{ display: 'grid', gap: 4 }}>
        {lastTicket.details.map((d) => (
          <div key={d.bookId} className="small" style={ui.between}>
            <span>{d.bookTitle || d.bookId}</span>
            <span className="mono">× {d.quantity}</span>
          </div>
        ))}
      </div>
      <hr style={ui.divider} />
      <p className="small" style={{ margin: 0 }}>
        Mượn {formatDate(lastTicket.borrowDate)} · Hạn trả <strong>{longDate(lastTicket.dueDate)}</strong>
      </p>
      <div style={{ display: 'grid', gap: 8, marginTop: 14 }}>
        <button type="button" className="btn btn-primary" onClick={startNewTicket}>Lập phiếu mới</button>
        <button type="button" className="btn btn-ghost" onClick={() => navigate('/tickets')}>Xem danh sách phiếu</button>
      </div>
    </div>
  )

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <h1>Mượn sách</h1>
          <p className="muted">Chọn bạn đọc, thêm sách vào phiếu rồi bấm “Lập phiếu mượn”.</p>
        </div>
        <button type="button" className="btn btn-ghost" onClick={reloadAll} disabled={readersLoading || booksLoading}>
          Làm mới
        </button>
      </header>

      <div style={ui.layout}>
        <div style={ui.left}>
          <section style={ui.card}>
            <h2 style={ui.sectionTitle}>BẠN ĐỌC</h2>
            {reader ? renderReaderCard() : renderReaderPicker()}
          </section>

          <section style={ui.card}>
            <h2 style={ui.sectionTitle}>SÁCH</h2>
            {renderBooks()}
          </section>
        </div>

        <aside style={ui.right}>{lastTicket ? renderResult() : renderDraft()}</aside>
      </div>

      <ConfirmModal
        open={!!pendingReader}
        title="Đổi bạn đọc?"
        confirmText="Đổi và xóa giỏ"
        cancelText="Giữ nguyên"
        onConfirm={confirmReaderChange}
        onCancel={() => setPendingReader(null)}
      >
        <p>
          Phiếu đang soạn có {totalCopies} cuốn cho <strong>{reader?.fullName}</strong>. Đổi bạn đọc sẽ xóa các sách đã
          chọn để tránh lập phiếu nhầm người.
        </p>
      </ConfirmModal>
    </div>
  )
}
