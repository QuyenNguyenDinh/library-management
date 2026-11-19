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
  const [stockFilter, setStockFilter] = useState('ALL') // 'ALL' | 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK'
  const [sortBy, setSortBy] = useState('ID_ASC') // 'ID_ASC' | 'TITLE_ASC' | 'QTY_ASC' | 'QTY_DESC' | 'PRICE_DESC'
  const [currentPage, setCurrentPage] = useState(1)

  // Modal Thêm / Sửa sách
  const [showModal, setShowModal] = useState(false)
  const [modalMode, setModalMode] = useState('CREATE') // 'CREATE' | 'EDIT'
  const [currentBook, setCurrentBook] = useState(EMPTY_BOOK)

  // Modal Chi tiết & Kiểm tra tồn kho (gọi GET /api/books/{id} và /availability)
  const [detailBook, setDetailBook] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [checkQty, setCheckQty] = useState(1)
  const [availabilityResult, setAvailabilityResult] = useState(null)
  const [checkingStock, setCheckingStock] = useState(false)

  // Modal Nhập kho nhanh
  const [restockBook, setRestockBook] = useState(null)
  const [restockAmount, setRestockAmount] = useState(5)

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

  const handleOpenRestock = (book) => {
    setRestockBook(book)
    setRestockAmount(5)
  }

  const handleConfirmRestock = async (e) => {
    e.preventDefault()
    if (!restockBook) return
    const added = Math.floor(Number(restockAmount))
    if (Number.isNaN(added) || added <= 0) {
      toast.error('Số lượng nhập thêm phải lớn hơn 0.')
      return
    }

    setBusy(true)
    try {
      await bookApi.update(restockBook.id, {
        title: restockBook.title,
        author: restockBook.author,
        category: restockBook.category,
        availableQuantity: (restockBook.availableQuantity || 0) + added,
        price: restockBook.price,
      })
      toast.success(`Đã nhập thêm ${added} cuốn cho "${restockBook.title}".`)
      setRestockBook(null)
      await loadBooks(keyword)
    } catch (err) {
      toast.error(err.message || 'Không thể nhập thêm kho.')
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

  // Thống kê tổng quan & phân bổ theo thể loại
  const summary = useMemo(() => {
    const totalTitles = books.length
    const totalCopies = books.reduce((sum, b) => sum + (Number(b.availableQuantity) || 0), 0)
    const lowOrOutCount = books.filter((b) => (Number(b.availableQuantity) || 0) <= 2).length
    const totalValue = books.reduce(
      (sum, b) => sum + (Number(b.availableQuantity) || 0) * (Number(b.price) || 0),
      0,
    )
    return { totalTitles, totalCopies, lowOrOutCount, totalValue }
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
      if (stockFilter === 'IN_STOCK' && qty <= 2) return false
      if (stockFilter === 'LOW_STOCK' && (qty === 0 || qty > 2)) return false
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
          <p className="muted">
            Quản lý danh mục đầu sách, kiểm tra tồn kho theo thời gian thực, cập nhật giá bìa và nhập kho.
          </p>
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

      {/* Thẻ thống kê tổng quan kho sách */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
          gap: 12,
          marginBottom: 14,
        }}
      >
        <div style={{ padding: 14, border: '1px solid var(--line)', borderRadius: 10 }}>
          <div className="muted" style={{ fontSize: 13 }}>Tổng đầu sách</div>
          <div style={{ fontSize: 22, fontWeight: 700, marginTop: 4 }}>{summary.totalTitles} đầu sách</div>
        </div>
        <div style={{ padding: 14, border: '1px solid var(--line)', borderRadius: 10 }}>
          <div className="muted" style={{ fontSize: 13 }}>Tổng bản sách trong kho</div>
          <div style={{ fontSize: 22, fontWeight: 700, marginTop: 4 }}>{summary.totalCopies} cuốn</div>
        </div>
        <div style={{ padding: 14, border: '1px solid var(--line)', borderRadius: 10 }}>
          <div className="muted" style={{ fontSize: 13 }}>Sắp hết / Hết hàng (≤ 2)</div>
          <div style={{ fontSize: 22, fontWeight: 700, marginTop: 4, color: 'var(--late)' }}>
            {summary.lowOrOutCount} đầu sách
          </div>
        </div>
        <div style={{ padding: 14, border: '1px solid var(--line)', borderRadius: 10 }}>
          <div className="muted" style={{ fontSize: 13 }}>Tổng giá trị tủ sách</div>
          <div className="mono" style={{ fontSize: 20, fontWeight: 700, marginTop: 4 }}>
            {summary.totalValue.toLocaleString('vi-VN')}đ
          </div>
        </div>
      </div>

      {/* Thanh phân bổ nhanh theo thể loại */}
      {categoryStats.length > 0 && (
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 14, alignItems: 'center' }}>
          <span className="muted" style={{ fontSize: 13 }}>Thể loại:</span>
          <button
            type="button"
            className={selectedCategory === 'ALL' ? 'btn btn-primary btn-sm' : 'btn btn-ghost btn-sm'}
            onClick={() => {
              setSelectedCategory('ALL')
              setCurrentPage(1)
            }}
          >
            Tất cả ({summary.totalTitles})
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
              {cat.name} ({cat.titles} đầu · {cat.copies} cuốn)
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
          <option value="IN_STOCK">Còn hàng (&gt; 2 cuốn)</option>
          <option value="LOW_STOCK">Sắp hết (1–2 cuốn)</option>
          <option value="OUT_OF_STOCK">Hết sách (0 cuốn)</option>
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
                  <td className="mono" style={{ fontWeight: 600 }}>
                    {b.id}
                  </td>
                  <td style={{ fontWeight: 500 }}>
                    <button
                      type="button"
                      onClick={() => handleOpenDetailModal(b)}
                      style={{
                        background: 'none',
                        border: 'none',
                        padding: 0,
                        font: 'inherit',
                        fontWeight: 600,
                        color: 'inherit',
                        cursor: 'pointer',
                        textAlign: 'left',
                        textDecoration: 'underline',
                      }}
                    >
                      {b.title}
                    </button>
                  </td>
                  <td className="muted">{b.author}</td>
                  <td>
                    <span className="badge" style={{ background: 'var(--line)', color: 'var(--ink)' }}>
                      {b.category}
                    </span>
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
                        onClick={() => handleOpenRestock(b)}
                      >
                        + Nhập
                      </button>
                      <button
                        className="btn btn-ghost btn-sm"
                        onClick={() => handleOpenEditModal(b)}
                      >
                        Sửa
                      </button>
                      <button
                        className="btn btn-ghost btn-sm"
                        style={{ color: 'var(--late)' }}
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
          <span className="muted" style={{ fontSize: 13 }}>
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
            <span style={{ fontSize: 13, fontWeight: 600 }}>
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
                <div style={{ display: 'grid', gap: 8, fontSize: 14 }}>
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
                <div
                  style={{
                    marginTop: 10,
                    padding: 10,
                    borderRadius: 8,
                    border: '1px solid var(--line)',
                    fontSize: 13,
                  }}
                >
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
              <button
                type="button"
                className="btn btn-ghost"
                onClick={() => {
                  const target = detailBook
                  setDetailBook(null)
                  handleOpenRestock(target)
                }}
              >
                + Nhập thêm kho
              </button>
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
                      className="input