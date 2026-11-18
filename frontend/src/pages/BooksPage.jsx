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

export default function BooksPage() {
  const toast = useToast()
  const [books, setBooks] = useState([])
  const [loading, setLoading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [keyword, setKeyword] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('ALL')

  // Modal Thêm / Sửa sách
  const [showModal, setShowModal] = useState(false)
  const [modalMode, setModalMode] = useState('CREATE') // 'CREATE' | 'EDIT'
  const [currentBook, setCurrentBook] = useState(EMPTY_BOOK)

  // Modal xác nhận xóa sách
  const [deletingBook, setDeletingBook] = useState(null)

  const loadBooks = useCallback(
    async (searchKw = '') => {
      setLoading(true)
      try {
        const data = await bookApi.getAll(searchKw)
        setBooks(data || [])
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
      // Nếu sách đang được mượn (409 BookInUseException), backend trả về message rõ lý do
      toast.error(err.message || 'Không thể xóa sách này.')
      setDeletingBook(null)
    } finally {
      setBusy(false)
    }
  }

  const categories = useMemo(() => {
    return Array.from(new Set(books.map((b) => b.category).filter(Boolean)))
  }, [books])

  const filteredBooks = useMemo(() => {
    return books.filter((b) => {
      if (selectedCategory === 'ALL') return true
      return b.category === selectedCategory
    })
  }, [books, selectedCategory])

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <h1>Quản lý Sách Thư viện</h1>
          <p className="muted">
            Quản lý danh mục đầu sách, cập nhật số lượng tồn kho, giá bìa và tra cứu nhanh.
          </p>
        </div>
        <button className="btn btn-primary" onClick={handleOpenCreateModal}>
          + Thêm sách mới
        </button>
      </header>

      {/* Thanh tìm kiếm & lọc thể loại */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap', alignItems: 'center' }}>
        <form onSubmit={handleSearch} style={{ display: 'flex', gap: 8, flex: 1, minWidth: 260 }}>
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
          value={selectedCategory}
          onChange={(e) => setSelectedCategory(e.target.value)}
        >
          <option value="ALL">Tất cả thể loại</option>
          {categories.map((cat) => (
            <option key={cat} value={cat}>
              {cat}
            </option>
          ))}
        </select>
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
            {!loading && filteredBooks.length === 0 && (
              <tr>
                <td colSpan="7" className="empty">
                  Không tìm thấy cuốn sách nào phù hợp.
                </td>
              </tr>
            )}
            {!loading &&
              filteredBooks.map((b) => (
                <tr key={b.id}>
                  <td className="mono" style={{ fontWeight: 600 }}>
                    {b.id}
                  </td>
                  <td style={{ fontWeight: 500 }}>{b.title}</td>
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