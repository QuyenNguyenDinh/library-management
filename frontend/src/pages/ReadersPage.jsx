import { useCallback, useEffect, useMemo, useState } from 'react'
import { readerApi } from '../api/readerApi.js'
import ConfirmModal from '../components/ConfirmModal.jsx'
import { useToast } from '../components/Toast.jsx'

const FINE_DESCRIPTIONS = {
  STUDENT: '5.000đ/ngày',
  PRIORITY_STUDENT: '3.000đ/ngày',
  LECTURER: '2.000đ/ngày',
}

export default function ReadersPage() {
  const toast = useToast()
  const [readers, setReaders] = useState([])
  const [loading, setLoading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [keyword, setKeyword] = useState('')
  const [selectedType, setSelectedType] = useState('ALL')

  // Modal Thêm / Sửa bạn đọc
  const [showModal, setShowModal] = useState(false)
  const [modalMode, setModalMode] = useState('CREATE') // 'CREATE' | 'EDIT'
  const [currentReader, setCurrentReader] = useState({
    id: '',
    fullName: '',
    phone: '',
    type: 'STUDENT',
  })

  // Modal xác nhận xoá bạn đọc
  const [deletingReader, setDeletingReader] = useState(null)

  const loadReaders = useCallback(
    async (searchKw = '') => {
      setLoading(true)
      try {
        const data = await readerApi.getAll(searchKw)
        setReaders(data || [])
      } catch (err) {
        toast.error(err.message || 'Lỗi khi tải danh sách bạn đọc.')
      } finally {
        setLoading(false)
      }
    },
    [toast],
  )

  useEffect(() => {
    loadReaders()
  }, [loadReaders])

  const handleSearch = (e) => {
    e.preventDefault()
    loadReaders(keyword)
  }

  const handleOpenCreateModal = () => {
    setModalMode('CREATE')
    setCurrentReader({ id: '', fullName: '', phone: '', type: 'STUDENT' })
    setShowModal(true)
  }

  const handleOpenEditModal = (reader) => {
    setModalMode('EDIT')
    setCurrentReader({
      id: reader.id,
      fullName: reader.fullName,
      phone: reader.phone,
      type: reader.type,
    })
    setShowModal(true)
  }

  const handleSaveReader = async (e) => {
    e.preventDefault()

    if (!currentReader.fullName.trim() || !currentReader.phone.trim()) {
      toast.error('Vui lòng nhập đầy đủ họ tên và số điện thoại.')
      return
    }

    setBusy(true)
    try {
      if (modalMode === 'CREATE') {
        await readerApi.create(currentReader)
        toast.success('Thêm bạn đọc mới thành công!')
      } else {
        await readerApi.update(currentReader.id, currentReader)
        toast.success('Cập nhật thông tin bạn đọc thành công!')
      }
      setShowModal(false)
      await loadReaders(keyword)
    } catch (err) {
      toast.error(err.message || 'Có lỗi xảy ra khi lưu thông tin.')
    } finally {
      setBusy(false)
    }
  }

  const handleDeleteClick = (reader) => {
    if (reader.currentBorrowCount > 0) {
      toast.error(`Không thể xóa bạn đọc ${reader.fullName} vì đang mượn ${reader.currentBorrowCount} cuốn sách!`)
      return
    }
    setDeletingReader(reader)
  }

  const handleConfirmDelete = async () => {
    if (!deletingReader) return
    setBusy(true)
    try {
      await readerApi.delete(deletingReader.id)
      toast.success(`Đã xóa bạn đọc ${deletingReader.fullName} thành công.`)
      setDeletingReader(null)
      await loadReaders(keyword)
    } catch (err) {
      toast.error(err.message || 'Không thể xóa bạn đọc này.')
    } finally {
      setBusy(false)
    }
  }

  const filteredReaders = useMemo(() => {
    return readers.filter((r) => {
      if (selectedType === 'ALL') return true
      return r.type === selectedType
    })
  }, [readers, selectedType])

  const renderTypeBadge = (type) => {
    switch (type) {
      case 'PRIORITY_STUDENT':
        return <span className="badge badge-ok">SV ưu tiên</span>
      case 'LECTURER':
        return (
          <span className="badge" style={{ background: 'var(--line)', color: 'var(--ink)' }}>
            Giảng viên
          </span>
        )
      case 'STUDENT':
      default:
        return (
          <span className="badge" style={{ background: 'var(--line)', color: 'var(--ink)' }}>
            Sinh viên thường
          </span>
        )
    }
  }

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <h1>Quản lý Bạn đọc Thư viện</h1>
          <p className="muted">
            Quản lý hồ sơ bạn đọc, phân loại đối tượng, hạn mức mượn và chính sách phạt vi phạm.
          </p>
        </div>
        <button className="btn btn-primary" onClick={handleOpenCreateModal}>
          + Thêm bạn đọc
        </button>
      </header>

      {/* Filter and Search Bar */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap', alignItems: 'center' }}>
        <form onSubmit={handleSearch} style={{ display: 'flex', gap: 8, flex: 1, minWidth: 260 }}>
          <input
            type="text"
            className="input"
            placeholder="Tìm theo mã RD, họ tên hoặc SĐT…"
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
          value={selectedType}
          onChange={(e) => setSelectedType(e.target.value)}
        >
          <option value="ALL">Tất cả loại bạn đọc</option>
          <option value="STUDENT">Sinh viên thông thường</option>
          <option value="PRIORITY_STUDENT">Sinh viên ưu tiên</option>
          <option value="LECTURER">Giảng viên</option>
        </select>
      </div>

      {/* Reader Table */}
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Mã bạn đọc</th>
              <th>Họ và tên</th>
              <th>Số điện thoại</th>
              <th>Đối tượng</th>
              <th className="right">Hạn mức</th>
              <th className="right">Đang mượn</th>
              <th>Đơn giá phạt</th>
              <th className="right">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td colSpan="8" className="empty">
                  Đang tải danh sách…
                </td>
              </tr>
            )}
            {!loading && filteredReaders.length === 0 && (
              <tr>
                <td colSpan="8" className="empty">
                  Không tìm thấy bạn đọc nào phù hợp.
                </td>
              </tr>
            )}
            {!loading &&
              filteredReaders.map((r) => (
                <tr key={r.id}>
                  <td className="mono" style={{ fontWeight: 600 }}>
                    {r.id}
                  </td>
                  <td>{r.fullName}</td>
                  <td className="mono muted">{r.phone}</td>
                  <td>{renderTypeBadge(r.type)}</td>
                  <td className="right mono" style={{ fontWeight: 600 }}>
                    {r.maxBorrowLimit} cuốn
                  </td>
                  <td className="right">
                    <span className={r.currentBorrowCount > 0 ? 'badge badge-late' : 'badge badge-ok'}>
                      {r.currentBorrowCount || 0}
                    </span>
                  </td>
                  <td className="fine">{FINE_DESCRIPTIONS[r.type] || '—'}</td>
                  <td className="right">
                    <div style={{ display: 'inline-flex', gap: 6 }}>
                      <button
                        className="btn btn-ghost btn-sm"
                        onClick={() => handleOpenEditModal(r)}
                      >
                        Sửa
                      </button>
                      <button
                        className="btn btn-ghost btn-sm"
                        style={{ color: 'var(--late)' }}
                        onClick={() => handleDeleteClick(r)}
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

      {/* Modal Thêm / Sửa bạn đọc */}
      {showModal && (
        <div className="modal-backdrop" onClick={() => !busy && setShowModal(false)}>
          <div className="modal" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
            <h3>{modalMode === 'CREATE' ? 'Thêm mới Bạn đọc' : `Chỉnh sửa Bạn đọc (${currentReader.id})`}</h3>
            <form onSubmit={handleSaveReader}>
              <div className="modal-body">
                <label className="field">
                  <span>Họ và tên *</span>
                  <input
                    type="text"
                    className="input"
                    required
                    placeholder="Ví dụ: Nguyễn Văn A"
                    value={currentReader.fullName}
                    onChange={(e) => setCurrentReader({ ...currentReader, fullName: e.target.value })}
                  />
                </label>

                <label className="field">
                  <span>Số điện thoại *</span>
                  <input
                    type="text"
                    className="input"
                    required
                    placeholder="Ví dụ: 0901234567"
                    value={currentReader.phone}
                    onChange={(e) => setCurrentReader({ ...currentReader, phone: e.target.value })}
                  />
                </label>

                <label className="field">
                  <span>Loại đối tượng *</span>
                  <select
                    className="input"
                    value={currentReader.type}
                    onChange={(e) => setCurrentReader({ ...currentReader, type: e.target.value })}
                  >
                    <option value="STUDENT">Sinh viên thường (Hạn mức: 3 cuốn, Phạt: 5.000đ/ngày)</option>
                    <option value="PRIORITY_STUDENT">Sinh viên ưu tiên (Hạn mức: 5 cuốn, Phạt: 3.000đ/ngày)</option>
                    <option value="LECTURER">Giảng viên (Hạn mức: 10 cuốn, Phạt: 2.000đ/ngày)</option>
                  </select>
                </label>
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
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={busy}
                >
                  {busy ? 'Đang lưu…' : modalMode === 'CREATE' ? 'Thêm mới' : 'Lưu thay đổi'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal xác nhận xoá */}
      <ConfirmModal
        open={!!deletingReader}
        title="Xác nhận xóa bạn đọc"
        confirmText="Xóa"
        cancelText="Huỷ"
        busy={busy}
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeletingReader(null)}
      >
        <p>
          Bạn có chắc chắn muốn xóa bạn đọc <strong>{deletingReader?.fullName}</strong> (
          <span className="mono">{deletingReader?.id}</span>)?
        </p>
      </ConfirmModal>
    </div>
  )
}
