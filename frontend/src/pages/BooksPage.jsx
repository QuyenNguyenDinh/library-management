import { useCallback, useEffect, useMemo, useState } from 'react'
import { bookApi } from '../api/bookApi.js'
import ConfirmModal from '../components/ConfirmModal.jsx'
import { useToast } from '../components/Toast.jsx'

const EMPTY_BOOK = {
  id: '',
  title: '',
  author: '',
  category: '',
  availableQuantity: 1,
  price: 0,
}

const PAGE_SIZE = 8

export default function BooksPage() {
  const toast = useToast()
  const [books, setBooks] = useState([])
  const [loading, setLoading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [keyword, setKeyword] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('ALL')
  const [stockFilter, setStockFilter] = useState('ALL')
  const [sortBy, setSortBy] = useState('ID_ASC')
  const [currentPage, setCurrentPage] = useState(1)

  // Modal Thêm / Sửa sách
  const [showModal, setShowModal] = useState(false)
  const [modalMode, setModalMode] = useState('CREATE')
  const [currentBook, setCurrentBook] = useState(EMPTY_BOOK)

  // Modal Chi tiết & Kiểm tra tồn kho
  const [detailBook, setDetailBook] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [checkQty, setCheckQty] = useState(1)
  const [availabilityResult, setAvailabilityResult] = useState(null)
  const [checkingStock, setCheckingStock] = useState(false)


  // Modal xác nhận xóa sách
  const [deletingBook, setDeletingBook] = useState(null)

  const loadBooks = useCallback(
    async (searchKw = '') => {
      setLoading(true)
      try {
        const data = await bookApi.getAll(searchKw)
        setBooks(data || [])
        setCurrentPage(1)
      } catch (err) {
        toast.error(err.message || 'Lỗi khi tải danh sách sách.')
      } finally {
        setLoading(false)
      }
    },
    [toast],
  )

  useEffect(() => {
    loadBooks()
  }, [loadBooks])

  const handleSearch = (e) => {
    e.preventDefault()
    loadBooks(keyword)
  }

  const handleResetFilters = () => {
    setKeyword('')
    setSelectedCategory('ALL')
    setStockFilter('ALL')
    setSortBy('ID_ASC')
    setCurrentPage(1)
    loadBooks('')
  }

  const handleOpenDetailModal = async (book) => {
    setDetailBook(book)
    setCheckQty(1)
    setAvailabilityResult(null)
    setDetailLoading(true)
    try {
      const fresh = await bookApi.getById(book.id)
      if (fresh) setDetailBook(fresh)
    } catch (err) {
      toast.error(err.message || 'Không thể tải chi tiết sách.')
    } finally {
      setDetailLoading(false)
    }
  }

  const handleCheckAvailability = async (e) => {
    e.preventDefault()
    if (!detailBook) return
    const qty = Math.floor(Number(checkQty))
    if (Number.isNaN(qty) || qty <= 0) {
      toast.error('Số lượng cần kiểm tra phải lớn hơn 0.')
      return
    }
    setCheckingStock(true)
    try {
      const res = await bookApi.checkAvailability(detailBook.id, qty)
      setAvailabilityResult({ ...res, requestedQuantity: qty })
    } catch (err) {
      toast.error(err.message || 'Lỗi khi kiểm tra tồn kho.')
    } finally {
      setCheckingStock(false)
    }
  }

  const handleOpenCreateModal = () => {
    setModalMode('CREATE')
    setCurrentBook(EMPTY_BOOK)
    setShowModal(true)
  }

  const handleOpenEditModal = (book) => {
    setModalMode('EDIT')
    setCurrentBook({
      id: book.id,
      title: book.title || '',
      author: book.author || '',
      category: book.category || '',
      availableQuantity: book.availableQuantity ?? 0,
      price: book.price ?? 0,
    })
    setShowModal(true)
  }

  const handleSaveBook = async (e) => {
    e.preventDefault()

    const title = currentBook.title.trim()
    const author = currentBook.author.trim()
    const category = currentBook.category.trim()
    const availableQuantity = Number(currentBook.availableQuantity)
    const price = Number(currentBook.price)

    if (!title || !author || !category) {
      toast.error('Vui lòng nhập đầy đủ tên sách, tác giả và thể loại.')
      return
    }
    if (Number.isNaN(availableQuantity) || availableQuantity < 0) {
      toast.error('Số lượng tồn kho phải lớn hơn hoặc bằng 0.')
      return
    }
    if (Number.isNaN(price) || price < 0) {
      toast.error('Giá sách phải lớn hơn hoặc bằng 0.')
      return
    }

    const payload = {
      ...(modalMode === 'EDIT' ? { id: currentBook.id } : {}),
      title,
      author,
      category,
      availableQuantity: Math.floor(availableQuantity),
      price,
    }

    setBusy(true)
    try {
      if (modalMode === 'CREATE') {
        await bookApi.create(payload)
        toast.success('Thêm sách mới thành công!')
      } else {
        await bookApi.update(currentBook.id, payload)
        toast.success('Cập nhật thông tin sách thành công!')
      }
      setShowModal(false)
      await loadBooks(keyword)
    } catch (err) {
      toast.error(err.message || 'Có lỗi xảy ra khi lưu thông tin sách.')
    } finally {
      setBusy(false)
    }
  }


  const handleConfirmDelete = async () => {
    if (!deletingBook) return
    setBusy(true)
    try {
      await bookApi.delete(deletingBook.id)
      toast.success(`Đã xóa sách "${deletingBook.title}" thành công.`)
      setDeletingBook(null)
      await loadBooks(keyword)
    } catch (err) {
      toast.error(err.message || 'Không thể xóa sách này.')
      setDeletingBook(null)
    } finally {
      setBusy(false)
    }
  }

  // Thống kê riêng của danh mục sách (không trùng cảnh báo sắp hết bên trang Thống kê)
  const summary = useMemo(() => {
    const totalTitles = books.length
    const totalCopies = books.reduce((sum, b) => sum + (Number(b.availableQuantity) || 0), 0)
    const totalValue = books.reduce(
      (sum, b) => sum + (Number(b.availableQuantity) || 0) * (Number(b.price) || 0),
      0,
    )
    return { totalTitles, totalCopies, totalValue }
  }, [books])

  const categoryStats = useMemo(() => {
    const map = new Map()
    books.forEach((b) => {
      if (!b.category) return
      const prev = map.get(b.category) || { name: b.category, titles: 0, copies: 0 }
      prev.titles += 1
      prev.copies += Number(b.availableQuantity) || 0
      map.set(b.category, prev)
    })
    return Array.from(map.values())
  }, [books])

  const filteredBooks = useMemo(() => {
    const list = books.filter((b) => {
      if (selectedCategory !== 'ALL' && b.category !== selectedCategory) return false
      const qty = Number(b.availableQuantity) || 0
      if (stockFilter === 'IN_STOCK' && qty <= 0) return false
      if (stockFilter === 'OUT_OF_STOCK' && qty !== 0) return false
      return true
    })

    return [...list].sort((a, b) => {
      if (sortBy === 'TITLE_ASC') return (a.title || '').localeCompare(b.title || '', 'vi')
      if (sortBy === 'QTY_ASC') return (a.availableQuantity ?? 0) - (b.availableQuantity ?? 0)
      if (sortBy === 'QTY_DESC') return (b.availableQuantity ?? 0) - (a.availableQuantity ?? 0)
      if (sortBy === 'PRICE_DESC') return (b.price ?? 0) - (a.price ?? 0)
      return (a.id || '').localeCompare(b.id || '')
    })
  }, [books, selectedCategory, stockFilter, sortBy])

  const totalPages = Math.max(1, Math.ceil(filteredBooks.length / PAGE_SIZE))
  const safePage = Math.min(currentPage, totalPages)
  const paginatedBooks = useMemo(() => {
    const start = (safePage - 1) * PAGE_SIZE
    return filteredBooks.slice(start, start + PAGE_SIZE)
  }, [filteredBooks, safePage])

  const handleExportCsv = () => {
    if (filteredBooks.length === 0) {
      toast.error('Không có dữ liệu sách để xuất file.')
      return
    }
    const headers = ['Mã sách', 'Tên sách', 'Tác giả', 'Thể loại', 'Tồn kho', 'Giá bìa (VNĐ)']
    const rows = filteredBooks.map((b) => [
      b.id,
      `"${String(b.title || '').replaceAll('"', '""')}"`,
      `"${String(b.author || '').replaceAll('"', '""')}"`,
      `"${String(b.category || '').replaceAll('"', '""')}"`,
      b.availableQuantity ?? 0,
      b.price ?? 0,
    ])
    const csvContent = '\uFEFF' + [headers.join(','), ...rows.map((r) => r.join(','))].join('\n')
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'danh-muc-sach.csv'
    link.click()
    URL.revokeObjectURL(url)
    toast.success(`Đã xuất ${filteredBooks.length} đầu sách ra file CSV.`)
  }

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <h1>Quản lý Sách Thư viện</h1>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn btn-ghost" onClick={handleExportCsv}>
            Xuất CSV
          </button>
          <button className="btn btn-primary" onClick={handleOpenCreateModal}>
            + Thêm sách mới
          </button>
        </div>
      </header>

      {/* Thẻ thống kê danh mục sách */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: 12,
          marginBottom: 14,
        }}
      >
        <div className="card">
          <div className="muted">Tổng đầu sách</div>
          <div style={{ fontSize: 22, fontWeight: 700, marginTop: 4 }}>{summary.totalTitles} đầu sách</div>
        </div>
        <div className="card">
          <div className="muted">Tổng bản sách trong kho</div>
          <div style={{ fontSize: 22, fontWeight: 700, marginTop: 4 }}>{summary.totalCopies} cuốn</div>
        </div>
        <div className="card">
          <div className="muted">Tổng giá trị tủ sách</div>
          <div className="mono" style={{ fontSize: 20, fontWeight: 700, marginTop: 4 }}>
            {summary.totalValue.toLocaleString('vi-VN')}đ
          </div>
        </div>
      </div>

      {/* Thanh phân bổ nhanh theo thể loại */}
      {categoryStats.length > 0 && (
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 14, alignItems: 'center' }}>
          <span className="muted">Thể loại:</span>
          <button
            type="button"
            className={selectedCategory === 'ALL' ? 'btn btn-primary btn-sm' : 'btn btn-ghost btn-sm'}
            onClick={() => {
              setSelectedCategory('ALL')
              setCurrentPage(1)
            }}
          >
            Tất cả
          </button>
          {categoryStats.map((cat) => (
            <button
              key={cat.name}
              type="button"
              className={selectedCategory === cat.name ? 'btn btn-primary btn-sm' : 'btn btn-ghost btn-sm'}
              onClick={() => {
                setSelectedCategory(cat.name)
                setCurrentPage(1)
              }}
            >
              {cat.name}
            </button>
          ))}
        </div>
      )}

      {/* Thanh tìm kiếm, lọc & sắp xếp */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap', alignItems: 'center' }}>
        <form onSubmit={handleSearch} style={{ display: 'flex', gap: 8, flex: 1, minWidth: 240 }}>
          <input
            type="text"
            className="input"
            placeholder="Tìm theo mã BK hoặc tên sách…"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            style={{ flex: 1 }}
          />
          <button type="submit" className="btn btn-ghost">
            Tìm kiếm
          </button>
        </form>

        <select
          className="input"
          value={stockFilter}
          onChange={(e) => {
            setStockFilter(e.target.value)
            setCurrentPage(1)
          }}
        >
          <option value="ALL">Mọi trạng thái kho</option>
          <option value="IN_STOCK">Còn sách trong kho</option>
          <option value="OUT_OF_STOCK">Đã hết sách (0 cuốn)</option>
        </select>

        <select
          className="input"
          value={sortBy}
          onChange={(e) => setSortBy(e.target.value)}
        >
          <option value="ID_ASC">Sắp xếp: Mã sách</option>
          <option value="TITLE_ASC">Tên sách (A → Z)</option>
          <option value="QTY_ASC">Tồn kho (Thấp → Cao)</option>
          <option value="QTY_DESC">Tồn kho (Cao → Thấp)</option>
          <option value="PRICE_DESC">Giá bìa (Cao → Thấp)</option>
        </select>

        <button type="button" className="btn btn-ghost" onClick={handleResetFilters}>
          Đặt lại
        </button>
      </div>

      {/* Bảng danh sách sách */}
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Mã sách</th>
              <th>Tên sách</th>
              <th>Tác giả</th>
              <th>Thể loại</th>
              <th className="right">Tồn kho</th>
              <th className="right">Giá bìa</th>
              <th className="right">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td colSpan="7" className="empty">
                  Đang tải danh sách sách…
                </td>
              </tr>
            )}
            {!loading && paginatedBooks.length === 0 && (
              <tr>
                <td colSpan="7" className="empty">
                  Không tìm thấy cuốn sách nào phù hợp.
                </td>
              </tr>
            )}
            {!loading &&
              paginatedBooks.map((b) => (
                <tr key={b.id}>
                  <td className="mono">{b.id}</td>
                  <td>
                    <button
                      type="button"
                      title="Xem chi tiết sách"
                      style={{
                        background: 'none',
                        border: 0,
                        padding: 0,
                        font: 'inherit',
                        fontWeight: 500,
                        color: 'var(--ink)',
                        textAlign: 'left',
                        cursor: 'pointer',
                      }}
                      onMouseEnter={(e) => {
                        e.currentTarget.style.textDecoration = 'underline'
                      }}
                      onMouseLeave={(e) => {
                        e.currentTarget.style.textDecoration = 'none'
                      }}
                      onClick={() => handleOpenDetailModal(b)}
                    >
                      {b.title}
                    </button>
                  </td>
                  <td className="muted">{b.author}</td>
                  <td>
                    <span className="badge">{b.category}</span>
                  </td>
                  <td className="right">
                    <span className={b.availableQuantity > 0 ? 'badge badge-ok' : 'badge badge-late'}>
                      {b.availableQuantity > 0 ? `${b.availableQuantity} cuốn` : 'Hết sách'}
                    </span>
                  </td>
                  <td className="right mono">
                    {Number(b.price || 0).toLocaleString('vi-VN')}đ
                  </td>
                  <td className="right">
                    <div style={{ display: 'inline-flex', gap: 6 }}>
                      <button
                        className="btn btn-ghost btn-sm"
                        onClick={() => handleOpenDetailModal(b)}
                      >
                        Chi tiết
                      </button>
                      <button
                        className="btn btn-ghost btn-sm"
                        onClick={() => handleOpenEditModal(b)}
                      >
                        Sửa
                      </button>
                      <button
                        className="btn btn-ghost btn-sm"
                        onClick={() => setDeletingBook(b)}
                      >
                        Xóa
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {/* Thanh phân trang */}
      {!loading && filteredBooks.length > 0 && (
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginTop: 12,
            flexWrap: 'wrap',
            gap: 8,
          }}
        >
          <span className="muted">
            Hiển thị {(safePage - 1) * PAGE_SIZE + 1}–
            {Math.min(safePage * PAGE_SIZE, filteredBooks.length)} trên tổng số {filteredBooks.length} đầu sách
          </span>
          <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
            <button
              type="button"
              className="btn btn-ghost btn-sm"
              disabled={safePage <= 1}
              onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
            >
              ← Trước
            </button>
            <span>
              Trang {safePage} / {totalPages}
            </span>
            <button
              type="button"
              className="btn btn-ghost btn-sm"
              disabled={safePage >= totalPages}
              onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
            >
              Sau →
            </button>
          </div>
        </div>
      )}

      {/* Modal Chi tiết sách & Kiểm tra tồn kho */}
      {detailBook && (
        <div className="modal-backdrop" onClick={() => setDetailBook(null)}>
          <div className="modal" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
            <h3>
              Chi tiết sách (<span className="mono">{detailBook.id}</span>)
            </h3>
            <div className="modal-body">
              {detailLoading ? (
                <p className="muted">Đang tải dữ liệu chi tiết từ máy chủ…</p>
              ) : (
                <div style={{ display: 'grid', gap: 8 }}>
                  <div>
                    <span className="muted">Tên sách:</span> <strong>{detailBook.title}</strong>
                  </div>
                  <div>
                    <span className="muted">Tác giả:</span> <strong>{detailBook.author}</strong>
                  </div>
                  <div>
                    <span className="muted">Thể loại:</span> <strong>{detailBook.category}</strong>
                  </div>
                  <div>
                    <span className="muted">Số lượng hiện có:</span>{' '}
                    <strong>{detailBook.availableQuantity} cuốn</strong>
                  </div>
                  <div>
                    <span className="muted">Giá bìa / Tổng giá trị tồn:</span>{' '}
                    <strong className="mono">
                      {Number(detailBook.price || 0).toLocaleString('vi-VN')}đ /{' '}
                      {(
                        Number(detailBook.price || 0) * Number(detailBook.availableQuantity || 0)
                      ).toLocaleString('vi-VN')}
                      đ
                    </strong>
                  </div>
                </div>
              )}

              <hr style={{ margin: '14px 0', border: 0, borderTop: '1px solid var(--line)' }} />

              <form onSubmit={handleCheckAvailability}>
                <label className="field">
                  <span>Kiểm tra nhanh khả năng đáp ứng mượn (số cuốn)</span>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <input
                      type="number"
                      className="input"
                      min="1"
                      step="1"
                      required
                      value={checkQty}
                      onChange={(e) => setCheckQty(e.target.value)}
                      style={{ flex: 1 }}
                    />
                    <button type="submit" className="btn btn-primary" disabled={checkingStock}>
                      {checkingStock ? 'Đang kiểm tra…' : 'Kiểm tra kho'}
                    </button>
                  </div>
                </label>
              </form>

              {availabilityResult && (
                <div style={{ marginTop: 10 }}>
                  {availabilityResult.available ? (
                    <span className="badge badge-ok">
                      Đủ sách cho mượn {availabilityResult.requestedQuantity} cuốn (Kho còn{' '}
                      {availabilityResult.availableQuantity} cuốn)
                    </span>
                  ) : (
                    <span className="badge badge-late">
                      Không đủ {availabilityResult.requestedQuantity} cuốn (Kho chỉ còn{' '}
                      {availabilityResult.availableQuantity} cuốn)
                    </span>
                  )}
                </div>
              )}
            </div>
            <div className="modal-actions">
              <button type="button" className="btn btn-ghost" onClick={() => setDetailBook(null)}>
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Thêm / Sửa sách */}
      {showModal && (
        <div className="modal-backdrop" onClick={() => !busy && setShowModal(false)}>
          <div className="modal" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
            <h3>{modalMode === 'CREATE' ? 'Thêm sách mới' : `Chỉnh sửa sách (${currentBook.id})`}</h3>
            <form onSubmit={handleSaveBook}>
              <div className="modal-body">
                <label className="field">
                  <span>Tên sách *</span>
                  <input
                    type="text"
                    className="input"
                    required
                    placeholder="Ví dụ: Lập trình hướng đối tượng với Java"
                    value={currentBook.title}
                    onChange={(e) => setCurrentBook({ ...currentBook, title: e.target.value })}
                  />
                </label>

                <label className="field">
                  <span>Tác giả *</span>
                  <input
                    type="text"
                    className="input"
                    required
                    placeholder="Ví dụ: Nguyễn Văn A"
                    value={currentBook.author}
                    onChange={(e) => setCurrentBook({ ...currentBook, author: e.target.value })}
                  />
                </label>

                <label className="field">
                  <span>Thể loại *</span>
                  <input
                    type="text"
                    className="input"
                    required
                    placeholder="Ví dụ: Công nghệ thông tin"
                    value={currentBook.category}
                    onChange={(e) => setCurrentBook({ ...currentBook, category: e.target.value })}
                  />
                </label>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <label className="field">
                    <span>Số lượng tồn kho *</span>
                    <input
                      type="number"
                      className="input"
                      min="0"
                      step="1"
                      required
                      value={currentBook.availableQuantity}
                      onChange={(e) =>
                        setCurrentBook({ ...currentBook, availableQuantity: e.target.value })
                      }
                    />
                  </label>

                  <label className="field">
                    <span>Giá bìa (VNĐ) *</span>
                    <input
                      type="number"
                      className="input"
                      min="0"
                      required
                      value={currentBook.price}
                      onChange={(e) => setCurrentBook({ ...currentBook, price: e.target.value })}
                    />
                  </label>
                </div>
              </div>

              <div className="modal-actions">
                <button
                  type="button"
                  className="btn btn-ghost"
                  onClick={() => setShowModal(false)}
                  disabled={busy}
                >
                  Huỷ
                </button>
                <button type="submit" className="btn btn-primary" disabled={busy}>
                  {busy ? 'Đang lưu…' : modalMode === 'CREATE' ? 'Thêm sách' : 'Lưu thay đổi'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}


      {/* Modal xác nhận xóa sách */}
      <ConfirmModal
        open={!!deletingBook}
        title="Xác nhận xóa sách"
        confirmText="Xóa"
        cancelText="Huỷ"
        busy={busy}
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeletingBook(null)}
      >
        <p>
          Bạn có chắc chắn muốn xóa cuốn sách <strong>{deletingBook?.title}</strong> (
          <span className="mono">{deletingBook?.id}</span>)?
        </p>
      </ConfirmModal>
    </div>
  )
}