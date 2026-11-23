import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, formatDate, formatMoney } from '../api/client.js'
import { readerApi } from '../api/readerApi.js'
import ConfirmModal from '../components/ConfirmModal.jsx'
import Pagination, { paginate } from '../components/Pagination.jsx'
import { useToast } from '../components/Toast.jsx'
import {
  matches,
  READER_TYPES,
  readerTypeLabel,
  ticketCopies,
  ticketState,
} from '../utils/libraryView.js'

const PAGE_SIZE = 10
const PHONE_PATTERN = /^0\d{9}$/

const BORROW_FILTERS = {
  ALL: 'Tất cả',
  NONE: 'Chưa mượn',
  BORROWING: 'Đang mượn',
  OVERDUE: 'Có phiếu quá hạn',
}

export default function ReadersPage() {
  const toast = useToast()
  const navigate = useNavigate()

  // Dữ liệu API
  const [readers, setReaders] = useState([])
  const [tickets, setTickets] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  // Bộ lọc
  const [keyword, setKeyword] = useState('')
  const [typeFilter, setTypeFilter] = useState('ALL')
  const [statusFilter, setStatusFilter] = useState('ALL') // 'ALL' | 'NONE' | 'BORROWING' | 'OVERDUE'
  const [page, setPage] = useState(1)

  // Hộp thoại
  const [detailReader, setDetailReader] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [modalMode, setModalMode] = useState('CREATE') // 'CREATE' | 'EDIT'
  const [currentReader, setCurrentReader] = useState({
    id: '',
    fullName: '',
    phone: '',
    type: 'STUDENT',
    currentBorrowCount: 0,
  })
  const [touched, setTouched] = useState({ fullName: false, phone: false })
  const [deletingReader, setDeletingReader] = useState(null)

  // Tải dữ liệu: Hàm load gọi song song readerApi.getAll() và api.get('/api/tickets')
  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [readersData, ticketsData] = await Promise.all([
        readerApi.getAll(),
        api.get('/api/tickets'),
      ])
      setReaders(Array.isArray(readersData) ? readersData : [])
      setTickets(Array.isArray(ticketsData) ? ticketsData : [])
    } catch (err) {
      const msg = err?.message || 'Không tải được danh sách bạn đọc.'
      setError(msg)
      toast.error(msg)
    } finally {
      setLoading(false)
    }
  }, [toast])

  useEffect(() => {
    load()
  }, [load])

  // Tính toán: ticketsByReader - Map gom phiếu theo readerId
  const ticketsByReader = useMemo(() => {
    const map = new Map()
    for (const t of tickets) {
      if (!t.readerId) continue
      if (!map.has(t.readerId)) {
        map.set(t.readerId, [])
      }
      map.get(t.readerId).push(t)
    }
    return map
  }, [tickets])

  // overdueOf(readerId): trả về các phiếu của bạn đọc đó có ticketState(t).key === 'OVERDUE'
  const overdueOf = useCallback(
    (readerId) => {
      const list = ticketsByReader.get(readerId) || []
      return list.filter((t) => ticketState(t).key === 'OVERDUE')
    },
    [ticketsByReader],
  )

  // counts: tổng bạn đọc, số bạn đọc có currentBorrowCount > 0, số bạn đọc có phiếu quá hạn
  const counts = useMemo(() => {
    const total = readers.length
    const borrowing = readers.filter((r) => (r.currentBorrowCount || 0) > 0).length
    const overdue = readers.filter((r) => overdueOf(r.id).length > 0).length
    return { total, borrowing, overdue }
  }, [readers, overdueOf])

  // filtered: lọc lần lượt theo loại bạn đọc, theo tình trạng mượn (NONE, BORROWING, OVERDUE), rồi theo từ khoá bằng matches(keyword, r.id, r.fullName, r.phone)
  const filtered = useMemo(() => {
    return readers.filter((r) => {
      // 1. Loại bạn đọc
      if (typeFilter !== 'ALL' && r.type !== typeFilter) {
        return false
      }
      // 2. Tình trạng mượn
      if (statusFilter === 'NONE' && (r.currentBorrowCount || 0) > 0) {
        return false
      }
      if (statusFilter === 'BORROWING' && (r.currentBorrowCount || 0) <= 0) {
        return false
      }
      if (statusFilter === 'OVERDUE' && overdueOf(r.id).length === 0) {
        return false
      }
      // 3. Từ khoá
      if (!matches(keyword, r.id, r.fullName, r.phone)) {
        return false
      }
      return true
    })
  }, [readers, typeFilter, statusFilter, keyword, overdueOf])

  // Cắt danh sách bằng paginate
  const paginated = useMemo(() => {
    return paginate(filtered, page, PAGE_SIZE)
  }, [filtered, page])

  // Mở modal Thêm bạn đọc
  const handleOpenCreateModal = () => {
    setModalMode('CREATE')
    setCurrentReader({
      id: '',
      fullName: '',
      phone: '',
      type: 'STUDENT',
      currentBorrowCount: 0,
    })
    setTouched({ fullName: false, phone: false })
    setShowModal(true)
  }

  // Mở modal Sửa bạn đọc
  const handleOpenEditModal = (r) => {
    setModalMode('EDIT')
    setCurrentReader({
      id: r.id,
      fullName: r.fullName,
      phone: r.phone,
      type: r.type,
      currentBorrowCount: r.currentBorrowCount || 0,
    })
    setTouched({ fullName: false, phone: false })
    setShowModal(true)
  }

  // Mở modal Chi tiết bạn đọc
  const handleOpenDetailModal = (r) => {
    setDetailReader(r)
  }

  // Xóa bạn đọc:
  // Nếu bạn đọc đang mượn sách thì không mở hộp xác nhận, chỉ báo toast lỗi "Không thể xóa tên: đang mượn n cuốn sách."
  // Nếu không đang mượn thì mở ConfirmModal với tiêu đề "Xóa bạn đọc?", hai nút "Xóa" và "Hủy".
  const handleDeleteClick = (r) => {
    if (r.currentBorrowCount > 0) {
      toast.error(
        `Không thể xóa ${r.fullName}: đang mượn ${r.currentBorrowCount} cuốn sách.`,
      )
      return
    }
    setDeletingReader(r)
  }

  const handleConfirmDelete = async () => {
    if (!deletingReader) return
    setBusy(true)
    try {
      await readerApi.delete(deletingReader.id)
      toast.success(`Đã xóa bạn đọc ${deletingReader.fullName} thành công.`)
      setDeletingReader(null)
      await load()
    } catch (err) {
      toast.error(err?.message || 'Không thể xóa bạn đọc này.')
    } finally {
      setBusy(false)
    }
  }

  // Kiểm tra lỗi form thêm / sửa:
  // Chỉ hiện sau khi đã bấm vào ô rồi rời ra (onBlur), hoặc sau khi bấm Lưu
  // Tên trống thì báo "Nhập họ và tên."
  // Số điện thoại trống thì báo "Nhập số điện thoại."
  // Số điện thoại sai mẫu thì báo "Số điện thoại gồm 10 chữ số và bắt đầu bằng 0."
  // Câu báo lỗi dùng class small fine
  const isNameEmpty = !currentReader.fullName.trim()
  const isPhoneEmpty = !currentReader.phone.trim()
  const isPhoneFormatValid = PHONE_PATTERN.test(currentReader.phone.trim())

  let nameError = ''
  if (touched.fullName && isNameEmpty) {
    nameError = 'Nhập họ và tên.'
  }

  let phoneError = ''
  if (touched.phone) {
    if (isPhoneEmpty) {
      phoneError = 'Nhập số điện thoại.'
    } else if (!isPhoneFormatValid) {
      phoneError = 'Số điện thoại gồm 10 chữ số và bắt đầu bằng 0.'
    }
  }

  const currentTypeConfig = READER_TYPES[currentReader.type] || READER_TYPES.STUDENT

  const handleSaveReader = async (e) => {
    e.preventDefault()
    setTouched({ fullName: true, phone: true })

    if (isNameEmpty || isPhoneEmpty || !isPhoneFormatValid) {
      return
    }

    setBusy(true)
    try {
      if (modalMode === 'CREATE') {
        await readerApi.create({
          fullName: currentReader.fullName.trim(),
          phone: currentReader.phone.trim(),
          type: currentReader.type,
        })
        toast.success('Thêm bạn đọc mới thành công!')
      } else {
        await readerApi.update(currentReader.id, {
          fullName: currentReader.fullName.trim(),
          phone: currentReader.phone.trim(),
          type: currentReader.type,
        })
        toast.success('Cập nhật thông tin bạn đọc thành công!')
      }
      setShowModal(false)
      await load()
    } catch (err) {
      toast.error(err?.message || 'Có lỗi xảy ra khi lưu thông tin bạn đọc.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page">
      {/* Đầu trang: Tiêu đề h1 là "Bạn đọc". Không có dòng mô tả, không có nút Tải lại. Bên phải có nút "+ Thêm bạn đọc". */}
      <header className="page-head">
        <h1>Bạn đọc</h1>
        <button className="btn btn-primary" onClick={handleOpenCreateModal}>
          + Thêm bạn đọc
        </button>
      </header>

      {/* Ba thẻ tổng quan, class stat-grid:
          Ba thẻ là "Tổng bạn đọc", "Đang mượn sách", "Có phiếu quá hạn".
          Mỗi thẻ là button class stat-card. Thẻ đang chọn thêm class active. Bấm vào thẻ thì lọc theo nhóm tương ứng.
          Số dùng class stat-value. Riêng thẻ quá hạn, nếu số lớn hơn 0 thì thêm class fine. Khi đang tải thì hiện dấu "…". */}
      <div className="stat-grid">
        <button
          type="button"
          className={`stat-card ${statusFilter === 'ALL' ? 'active' : ''}`}
          onClick={() => {
            setStatusFilter('ALL')
            setPage(1)
          }}
        >
          <div className="muted small">Tổng bạn đọc</div>
          <div className="stat-value">{loading ? '…' : counts.total}</div>
        </button>

        <button
          type="button"
          className={`stat-card ${statusFilter === 'BORROWING' ? 'active' : ''}`}
          onClick={() => {
            setStatusFilter('BORROWING')
            setPage(1)
          }}
        >
          <div className="muted small">Đang mượn sách</div>
          <div className="stat-value">{loading ? '…' : counts.borrowing}</div>
        </button>

        <button
          type="button"
          className={`stat-card ${statusFilter === 'OVERDUE' ? 'active' : ''}`}
          onClick={() => {
            setStatusFilter('OVERDUE')
            setPage(1)
          }}
        >
          <div className="muted small">Có phiếu quá hạn</div>
          <div className={`stat-value ${!loading && counts.overdue > 0 ? 'fine' : ''}`}>
            {loading ? '…' : counts.overdue}
          </div>
        </button>
      </div>

      {/* Thanh lọc, class toolbar:
          Ô tìm kiếm: placeholder "Tìm theo mã RD, họ tên hoặc số điện thoại…", style flex '1 1 260px'.
          Ô chọn loại: mục đầu "Mọi loại bạn đọc", các mục sau lấy từ READER_TYPES.
          Ô chọn tình trạng mượn: lấy từ BORROW_FILTERS, riêng mục "Tất cả" hiện chữ "Mọi tình trạng mượn".
          Đổi bất kỳ bộ lọc nào thì đưa về trang 1. */}
      <div className="toolbar">
        <input
          type="text"
          className="input"
          placeholder="Tìm theo mã RD, họ tên hoặc số điện thoại…"
          style={{ flex: '1 1 260px' }}
          value={keyword}
          onChange={(e) => {
            setKeyword(e.target.value)
            setPage(1)
          }}
        />

        <select
          className="input"
          value={typeFilter}
          onChange={(e) => {
            setTypeFilter(e.target.value)
            setPage(1)
          }}
        >
          <option value="ALL">Mọi loại bạn đọc</option>
          {Object.entries(READER_TYPES).map(([key, meta]) => (
            <option key={key} value={key}>
              {meta.label}
            </option>
          ))}
        </select>

        <select
          className="input"
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value)
            setPage(1)
          }}
        >
          {Object.entries(BORROW_FILTERS).map(([key, label]) => (
            <option key={key} value={key}>
              {key === 'ALL' ? 'Mọi tình trạng mượn' : label}
            </option>
          ))}
        </select>
      </div>

      {/* Khi có lỗi: hiện ô class card, chữ màu var(--late), bên trái là câu lỗi, bên phải là nút "Thử lại" (btn btn-ghost btn-sm) gọi lại load. */}
      {error && (
        <div
          className="card"
          style={{
            color: 'var(--late)',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: 16,
          }}
        >
          <span>{error}</span>
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            onClick={load}
          >
            Thử lại
          </button>
        </div>
      )}

      {/* Bảng có các cột: Mã, Họ và tên, Điện thoại, Loại bạn đọc, Đang mượn (căn phải), Thao tác (căn phải). */}
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Mã</th>
              <th>Họ và tên</th>
              <th>Điện thoại</th>
              <th>Loại bạn đọc</th>
              <th className="right">Đang mượn</th>
              <th className="right">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td colSpan="6" className="empty">
                  Đang tải danh sách…
                </td>
              </tr>
            )}
            {!loading && paginated.length === 0 && (
              <tr>
                <td colSpan="6" className="empty">
                  Không tìm thấy bạn đọc nào phù hợp.
                </td>
              </tr>
            )}
            {!loading &&
              paginated.map((r) => {
                const overdues = overdueOf(r.id)
                const limit = r.maxBorrowLimit || READER_TYPES[r.type]?.limit || 3
                const count = r.currentBorrowCount || 0
                const isFull = count >= limit

                return (
                  <tr key={r.id}>
                    <td className="mono" style={{ fontWeight: 600 }}>
                      {r.id}
                    </td>
                    <td>
                      <div>
                        {/* Họ và tên: Là nút class link-btn, chữ màu var(--ink), đậm 500. Bấm vào thì mở hộp chi tiết. */}
                        <button
                          type="button"
                          className="link-btn"
                          style={{ color: 'var(--ink)', fontWeight: 500 }}
                          onClick={() => handleOpenDetailModal(r)}
                        >
                          {r.fullName}
                        </button>
                      </div>
                      {/* Nếu bạn đọc có phiếu quá hạn thì bên dưới thêm badge badge-late "n phiếu quá hạn" */}
                      {overdues.length > 0 && (
                        <div style={{ marginTop: 4 }}>
                          <span className="badge badge-late">
                            {overdues.length} phiếu quá hạn
                          </span>
                        </div>
                      )}
                    </td>
                    <td className="mono muted">{r.phone}</td>
                    {/* Loại bạn đọc: badge badge-neutral, chữ lấy từ readerTypeLabel */}
                    <td>
                      <span className="badge badge-neutral">
                        {readerTypeLabel(r.type)}
                      </span>
                    </td>
                    {/* Đang mượn: badge ghi "x/y cuốn". Đã mượn đủ hạn mức thì dùng badge-warn, chưa đủ thì dùng badge-neutral. */}
                    <td className="right">
                      <span
                        className={isFull ? 'badge badge-warn' : 'badge badge-neutral'}
                      >
                        {count}/{limit} cuốn
                      </span>
                    </td>
                    {/* Thao tác: ba nút "Chi tiết", "Sửa", "Xóa" (nút Xóa chữ đỏ). Không có nút Mượn sách. */}
                    <td className="right">
                      <div style={{ display: 'inline-flex', gap: 6 }}>
                        <button
                          type="button"
                          className="btn btn-ghost btn-sm"
                          onClick={() => handleOpenDetailModal(r)}
                        >
                          Chi tiết
                        </button>
                        <button
                          type="button"
                          className="btn btn-ghost btn-sm"
                          onClick={() => handleOpenEditModal(r)}
                        >
                          Sửa
                        </button>
                        <button
                          type="button"
                          className="btn btn-ghost btn-sm"
                          style={{ color: 'var(--late)' }}
                          onClick={() => handleDeleteClick(r)}
                        >
                          Xóa
                        </button>
                      </div>
                    </td>
                  </tr>
                )
              })}
          </tbody>
        </table>
      </div>

      {/* Bên dưới bảng đặt Pagination với unit="bạn đọc" */}
      {!loading && (
        <Pagination
          page={page}
          pageSize={PAGE_SIZE}
          total={filtered.length}
          onChange={setPage}
          unit="bạn đọc"
        />
      )}

      {/* Hộp chi tiết bạn đọc, class modal modal-wide */}
      {detailReader && (
        <div
          className="modal-backdrop"
          onClick={() => setDetailReader(null)}
        >
          <div
            className="modal modal-wide"
            role="dialog"
            aria-modal="true"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Phần đầu: tên bạn đọc (h3), dòng nhỏ "mã · số điện thoại", nút Đóng bên phải */}
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'flex-start',
                marginBottom: 16,
              }}
            >
              <div>
                <h3>{detailReader.fullName}</h3>
                <div className="muted small mono" style={{ marginTop: 2 }}>
                  {detailReader.id} · {detailReader.phone}
                </div>
              </div>
              <button
                type="button"
                className="btn btn-ghost btn-sm"
                onClick={() => setDetailReader(null)}
              >
                Đóng
              </button>
            </div>

            {/* Bốn thẻ trong stat-grid (các thẻ là div, không phải button):
                "Nhóm bạn đọc".
                "Đang mượn / hạn mức": ghi "x/y cuốn", đủ hạn mức thì chữ đỏ.
                "Chính sách phạt": ghi "x đ/ngày trễ".
                "Tiền phạt đã ghi nhận": tổng fineAmount của các phiếu đã trả. */}
            {(() => {
              const detailLimit =
                detailReader.maxBorrowLimit ||
                READER_TYPES[detailReader.type]?.limit ||
                3
              const isDetailFull = (detailReader.currentBorrowCount || 0) >= detailLimit
              const detailRate =
                detailReader.finePolicy?.dailyRate ||
                READER_TYPES[detailReader.type]?.dailyRate ||
                5000
              const readerTickets = ticketsByReader.get(detailReader.id) || []
              const returnedTickets = readerTickets.filter((t) => t.status === 'RETURNED')
              const totalFine = returnedTickets.reduce(
                (sum, t) => sum + (Number(t.fineAmount) || 0),
                0,
              )
              const borrowingTickets = readerTickets.filter((t) => t.status !== 'RETURNED')
              const historyTickets = [...returnedTickets].sort((a, b) =>
                (b.returnDate || b.borrowDate || '').localeCompare(
                  a.returnDate || a.borrowDate || '',
                ),
              )

              return (
                <>
                  <div className="stat-grid">
                    <div className="stat-card">
                      <div className="muted small">Nhóm bạn đọc</div>
                      <div style={{ fontSize: '1.2rem', fontWeight: 600, marginTop: 4 }}>
                        {readerTypeLabel(detailReader.type)}
                      </div>
                    </div>
                    <div className="stat-card">
                      <div className="muted small">Đang mượn / hạn mức</div>
                      <div
                        className="mono"
                        style={{
                          fontSize: '1.2rem',
                          fontWeight: 600,
                          marginTop: 4,
                          color: isDetailFull ? 'var(--late)' : 'inherit',
                        }}
                      >
                        {detailReader.currentBorrowCount || 0}/{detailLimit} cuốn
                      </div>
                    </div>
                    <div className="stat-card">
                      <div className="muted small">Chính sách phạt</div>
                      <div style={{ fontSize: '1.2rem', fontWeight: 600, marginTop: 4 }}>
                        {Number(detailRate).toLocaleString('vi-VN')} đ/ngày trễ
                      </div>
                    </div>
                    <div className="stat-card">
                      <div className="muted small">Tiền phạt đã ghi nhận</div>
                      <div
                        className="mono"
                        style={{
                          fontSize: '1.2rem',
                          fontWeight: 600,
                          marginTop: 4,
                          color: totalFine > 0 ? 'var(--late)' : 'inherit',
                        }}
                      >
                        {formatMoney(totalFine)}
                      </div>
                    </div>
                  </div>

                  {/* Bảng "Phiếu đang mượn (n)":
                      Các cột: mã phiếu (link-btn, bấm thì chuyển sang /tickets?id=mã), "n cuốn", "Hạn dd/mm/yyyy", badge trạng thái, nút "Trả sách" (chuyển sang /return?ticketId=mã).
                      Không có phiếu thì ghi "Không có phiếu đang mượn." */}
                  <h4 style={{ margin: '18px 0 8px' }}>
                    Phiếu đang mượn ({borrowingTickets.length})
                  </h4>
                  <div className="table-wrap" style={{ marginBottom: 20 }}>
                    <table className="table">
                      <thead>
                        <tr>
                          <th>Mã phiếu</th>
                          <th>Số cuốn</th>
                          <th>Hạn trả</th>
                          <th>Trạng thái</th>
                          <th className="right">Thao tác</th>
                        </tr>
                      </thead>
                      <tbody>
                        {borrowingTickets.length === 0 ? (
                          <tr>
                            <td colSpan="5" className="empty">
                              Không có phiếu đang mượn.
                            </td>
                          </tr>
                        ) : (
                          borrowingTickets.map((t) => {
                            const count = ticketCopies(t) || 1
                            const stateInfo = ticketState(t)
                            return (
                              <tr key={t.id}>
                                <td>
                                  <button
                                    type="button"
                                    className="link-btn mono"
                                    onClick={() => {
                                      setDetailReader(null)
                                      navigate(`/tickets?id=${t.id}`)
                                    }}
                                  >
                                    {t.id}
                                  </button>
                                </td>
                                <td>{count} cuốn</td>
                                <td>Hạn {formatDate(t.dueDate)}</td>
                                <td>
                                  <span className={stateInfo.badge}>
                                    {stateInfo.key === 'OVERDUE' ? 'Quá hạn' : 'Trong hạn'}
                                  </span>
                                </td>
                                <td className="right">
                                  <button
                                    type="button"
                                    className="btn btn-primary btn-sm"
                                    onClick={() => {
                                      setDetailReader(null)
                                      navigate(`/return?ticketId=${t.id}`)
                                    }}
                                  >
                                    Trả sách
                                  </button>
                                </td>
                              </tr>
                            )
                          })
                        )}
                      </tbody>
                    </table>
                  </div>

                  {/* Bảng "Lịch sử mượn/trả (n)":
                      Các cột: Phiếu, Ngày mượn, Ngày trả, Tình trạng, Tiền phạt (căn phải, có tiền thì chữ đỏ).
                      Không có thì ghi "Chưa có lịch sử trả." */}
                  <h4 style={{ margin: '18px 0 8px' }}>
                    Lịch sử mượn/trả ({historyTickets.length})
                  </h4>
                  <div className="table-wrap">
                    <table className="table">
                      <thead>
                        <tr>
                          <th>Phiếu</th>
                          <th>Ngày mượn</th>
                          <th>Ngày trả</th>
                          <th>Tình trạng</th>
                          <th className="right">Tiền phạt</th>
                        </tr>
                      </thead>
                      <tbody>
                        {historyTickets.length === 0 ? (
                          <tr>
                            <td colSpan="5" className="empty">
                              Chưa có lịch sử trả.
                            </td>
                          </tr>
                        ) : (
                          historyTickets.map((t) => {
                            const hasFine = Number(t.fineAmount || 0) > 0
                            return (
                              <tr key={t.id}>
                                <td>
                                  <button
                                    type="button"
                                    className="link-btn mono"
                                    onClick={() => {
                                      setDetailReader(null)
                                      navigate(`/tickets?id=${t.id}`)
                                    }}
                                  >
                                    {t.id}
                                  </button>
                                </td>
                                <td>{formatDate(t.borrowDate)}</td>
                                <td>{formatDate(t.returnDate)}</td>
                                <td>
                                  <span className="badge">Đã trả</span>
                                </td>
                                <td
                                  className="right mono"
                                  style={{
                                    color: hasFine ? 'var(--late)' : 'inherit',
                                    fontWeight: hasFine ? 600 : 400,
                                  }}
                                >
                                  {formatMoney(t.fineAmount)}
                                </td>
                              </tr>
                            )
                          })
                        )}
                      </tbody>
                    </table>
                  </div>
                </>
              )
            })()}
          </div>
        </div>
      )}

      {/* Hộp thêm và sửa:
          Tiêu đề là "Thêm bạn đọc" hoặc "Sửa bạn đọc RDxxx".
          Các ô:
          "Họ và tên *", placeholder "Ví dụ: Nguyễn Văn A".
          "Số điện thoại *", placeholder "0901234567".
          "Loại bạn đọc *".
          Kiểm tra lỗi, chỉ hiện sau khi đã bấm vào ô rồi rời ra, hoặc sau khi bấm Lưu:
          Tên trống thì báo "Nhập họ và tên.".
          Số điện thoại trống thì báo "Nhập số điện thoại.".
          Số điện thoại sai mẫu thì báo "Số điện thoại gồm 10 chữ số và bắt đầu bằng 0.".
          Câu báo lỗi dùng class small fine.
          Dưới ô loại có dòng nhỏ mờ "Hạn mức x cuốn · phạt trễ x đ/ngày".
          Khi sửa, nếu số cuốn bạn đọc đang mượn lớn hơn hạn mức của loại mới thì hiện dòng chữ màu var(--warn):
          "Bạn đọc đang mượn x cuốn, nhiều hơn hạn mức mới y cuốn. Sẽ không mượn thêm được cho tới khi trả bớt."
          Nút "Hủy". Nút lưu ghi "Thêm bạn đọc" hoặc "Lưu thay đổi", khi đang lưu thì ghi "Đang lưu…". */}
      {showModal && (
        <div
          className="modal-backdrop"
          onClick={() => !busy && setShowModal(false)}
        >
          <div
            className="modal"
            role="dialog"
            aria-modal="true"
            onClick={(e) => e.stopPropagation()}
          >
            <h3>
              {modalMode === 'CREATE'
                ? 'Thêm bạn đọc'
                : `Sửa bạn đọc ${currentReader.id}`}
            </h3>
            <form noValidate onSubmit={handleSaveReader}>
              <div className="modal-body">
                <label className="field">
                  <span>Họ và tên *</span>
                  <input
                    type="text"
                    className="input"
                    placeholder="Ví dụ: Nguyễn Văn A"
                    value={currentReader.fullName}
                    onChange={(e) =>
                      setCurrentReader({
                        ...currentReader,
                        fullName: e.target.value,
                      })
                    }
                    onBlur={() =>
                      setTouched((prev) => ({ ...prev, fullName: true }))
                    }
                  />
                  {nameError && <div className="small fine">{nameError}</div>}
                </label>

                <label className="field">
                  <span>Số điện thoại *</span>
                  <input
                    type="text"
                    className="input"
                    placeholder="0901234567"
                    value={currentReader.phone}
                    onChange={(e) =>
                      setCurrentReader({
                        ...currentReader,
                        phone: e.target.value,
                      })
                    }
                    onBlur={() =>
                      setTouched((prev) => ({ ...prev, phone: true }))
                    }
                  />
                  {phoneError && <div className="small fine">{phoneError}</div>}
                </label>

                <label className="field">
                  <span>Loại bạn đọc *</span>
                  <select
                    className="input"
                    value={currentReader.type}
                    onChange={(e) =>
                      setCurrentReader({
                        ...currentReader,
                        type: e.target.value,
                      })
                    }
                  >
                    {Object.entries(READER_TYPES).map(([code, meta]) => (
                      <option key={code} value={code}>
                        {meta.label}
                      </option>
                    ))}
                  </select>
                  <div className="muted small" style={{ marginTop: 4 }}>
                    Hạn mức {currentTypeConfig.limit} cuốn · phạt trễ{' '}
                    {Number(currentTypeConfig.dailyRate).toLocaleString('vi-VN')}{' '}
                    đ/ngày
                  </div>
                  {modalMode === 'EDIT' &&
                    (currentReader.currentBorrowCount || 0) >
                      currentTypeConfig.limit && (
                      <div
                        style={{
                          color: 'var(--warn)',
                          marginTop: 6,
                          fontSize: '0.88rem',
                          lineHeight: 1.4,
                        }}
                      >
                        Bạn đọc đang mượn {currentReader.currentBorrowCount} cuốn,
                        nhiều hơn hạn mức mới {currentTypeConfig.limit} cuốn. Sẽ
                        không mượn thêm được cho tới khi trả bớt.
                      </div>
                    )}
                </label>
              </div>

              <div className="modal-actions">
                <button
                  type="button"
                  className="btn btn-ghost"
                  onClick={() => setShowModal(false)}
                  disabled={busy}
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={busy}
                >
                  {busy
                    ? 'Đang lưu…'
                    : modalMode === 'CREATE'
                    ? 'Thêm bạn đọc'
                    : 'Lưu thay đổi'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal xác nhận xoá */}
      <ConfirmModal
        open={!!deletingReader}
        title="Xóa bạn đọc?"
        confirmText="Xóa"
        cancelText="Hủy"
        busy={busy}
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeletingReader(null)}
      >
        <p>
          Bạn có chắc chắn muốn xóa bạn đọc{' '}
          <strong>{deletingReader?.fullName}</strong> (
          <span className="mono">{deletingReader?.id}</span>)?
        </p>
      </ConfirmModal>
    </div>
  )
}
