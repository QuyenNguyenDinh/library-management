import React, { useState, useEffect } from 'react';
import { readerApi } from '../api/readerApi';

export default function ReadersPage() {
  const [readers, setReaders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [selectedType, setSelectedType] = useState('ALL');
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Modal State
  const [showModal, setShowModal] = useState(false);
  const [modalMode, setModalMode] = useState('CREATE'); // 'CREATE' | 'EDIT'
  const [currentReader, setCurrentReader] = useState({
    id: '',
    fullName: '',
    phone: '',
    type: 'STUDENT',
  });

  const loadReaders = async (searchKw = '') => {
    setLoading(true);
    setErrorMsg('');
    try {
      const data = await readerApi.getAll(searchKw);
      setReaders(data || []);
    } catch (err) {
      setErrorMsg(err.message || 'Lỗi khi tải danh sách bạn đọc.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadReaders();
  }, []);

  const handleSearch = (e) => {
    e.preventDefault();
    loadReaders(keyword);
  };

  const handleOpenCreateModal = () => {
    setModalMode('CREATE');
    setCurrentReader({ id: '', fullName: '', phone: '', type: 'STUDENT' });
    setShowModal(true);
  };

  const handleOpenEditModal = (reader) => {
    setModalMode('EDIT');
    setCurrentReader({
      id: reader.id,
      fullName: reader.fullName,
      phone: reader.phone,
      type: reader.type,
    });
    setShowModal(true);
  };

  const handleSaveReader = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

    if (!currentReader.fullName.trim() || !currentReader.phone.trim()) {
      alert('Vui lòng nhập đầy đủ họ tên và số điện thoại.');
      return;
    }

    try {
      if (modalMode === 'CREATE') {
        await readerApi.create(currentReader);
        setSuccessMsg('Thêm bạn đọc mới thành công!');
      } else {
        await readerApi.update(currentReader.id, currentReader);
        setSuccessMsg('Cập nhật thông tin bạn đọc thành công!');
      }
      setShowModal(false);
      loadReaders(keyword);
    } catch (err) {
      alert(err.message || 'Có lỗi xảy ra khi lưu thông tin.');
    }
  };

  const handleDeleteReader = async (reader) => {
    if (reader.currentBorrowCount > 0) {
      alert(`Không thể xóa bạn đọc ${reader.fullName} vì đang mượn ${reader.currentBorrowCount} cuốn sách!`);
      return;
    }

    if (!window.confirm(`Bạn có chắc chắn muốn xóa bạn đọc ${reader.fullName} (${reader.id})?`)) {
      return;
    }

    try {
      await readerApi.delete(reader.id);
      setSuccessMsg(`Đã xóa bạn đọc ${reader.fullName} thành công.`);
      loadReaders(keyword);
    } catch (err) {
      alert(err.message || 'Không thể xóa bạn đọc này.');
    }
  };

  const filteredReaders = readers.filter((r) => {
    if (selectedType === 'ALL') return true;
    return r.type === selectedType;
  });

  const getTypeBadge = (type) => {
    switch (type) {
      case 'STUDENT':
        return <span style={{ padding: '4px 8px', borderRadius: '4px', backgroundColor: '#e6f4ea', color: '#137333', fontWeight: 'bold' }}>Sinh viên thường</span>;
      case 'PRIORITY_STUDENT':
        return <span style={{ padding: '4px 8px', borderRadius: '4px', backgroundColor: '#e8f0fe', color: '#1a73e8', fontWeight: 'bold' }}>SV ưu tiên</span>;
      case 'LECTURER':
        return <span style={{ padding: '4px 8px', borderRadius: '4px', backgroundColor: '#f3e8fd', color: '#8430ce', fontWeight: 'bold' }}>Giảng viên</span>;
      default:
        return <span>{type}</span>;
    }
  };

  const getFineDescription = (type) => {
    switch (type) {
      case 'STUDENT':
        return '5.000đ/ngày';
      case 'PRIORITY_STUDENT':
        return '3.000đ/ngày';
      case 'LECTURER':
        return '2.000đ/ngày';
      default:
        return '-';
    }
  };

  return (
    <div style={{ padding: '24px', fontFamily: 'sans-serif', maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <div>
          <h1 style={{ margin: 0, fontSize: '24px', color: '#1f2937' }}>Quản lý Bạn đọc Thư viện</h1>
          <p style={{ margin: '4px 0 0', color: '#6b7280', fontSize: '14px' }}>
            Quản lý hồ sơ bạn đọc, phân loại đối tượng, hạn mức mượn và chính sách phạt vi phạm.
          </p>
        </div>
        <button
          onClick={handleOpenCreateModal}
          style={{
            padding: '10px 18px',
            backgroundColor: '#2563eb',
            color: '#fff',
            border: 'none',
            borderRadius: '6px',
            fontWeight: '600',
            cursor: 'pointer',
            boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
          }}
        >
          + Thêm bạn đọc
        </button>
      </div>

      {successMsg && (
        <div style={{ padding: '12px 16px', backgroundColor: '#dcfce7', color: '#166534', borderRadius: '6px', marginBottom: '16px' }}>
          {successMsg}
        </div>
      )}

      {errorMsg && (
        <div style={{ padding: '12px 16px', backgroundColor: '#fee2e2', color: '#991b1b', borderRadius: '6px', marginBottom: '16px' }}>
          {errorMsg}
        </div>
      )}

      {/* Filter and Search Bar */}
      <div style={{ display: 'flex', gap: '12px', marginBottom: '20px', flexWrap: 'wrap' }}>
        <form onSubmit={handleSearch} style={{ display: 'flex', gap: '8px', flex: 1, minWidth: '300px' }}>
          <input
            type="text"
            placeholder="Tìm theo mã RD, họ tên hoặc SĐT..."
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            style={{
              flex: 1,
              padding: '10px 14px',
              borderRadius: '6px',
              border: '1px solid #d1d5db',
              fontSize: '14px',
            }}
          />
          <button
            type="submit"
            style={{
              padding: '10px 16px',
              backgroundColor: '#4b5563',
              color: '#fff',
              border: 'none',
              borderRadius: '6px',
              cursor: 'pointer',
            }}
          >
            Tìm kiếm
          </button>
        </form>

        <select
          value={selectedType}
          onChange={(e) => setSelectedType(e.target.value)}
          style={{
            padding: '10px 14px',
            borderRadius: '6px',
            border: '1px solid #d1d5db',
            fontSize: '14px',
            backgroundColor: '#fff',
          }}
        >
          <option value="ALL">-- Tất cả loại bạn đọc --</option>
          <option value="STUDENT">Sinh viên thông thường</option>
          <option value="PRIORITY_STUDENT">Sinh viên ưu tiên</option>
          <option value="LECTURER">Giảng viên</option>
        </select>
      </div>

      {/* Reader Table */}
      <div style={{ backgroundColor: '#fff', borderRadius: '8px', boxShadow: '0 1px 3px rgba(0,0,0,0.1)', overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
          <thead>
            <tr style={{ backgroundColor: '#f9fafb', borderBottom: '1px solid #e5e7eb', color: '#4b5563' }}>
              <th style={{ padding: '12px 16px' }}>Mã bạn đọc</th>
              <th style={{ padding: '12px 16px' }}>Họ và tên</th>
              <th style={{ padding: '12px 16px' }}>Số điện thoại</th>
              <th style={{ padding: '12px 16px' }}>Đối tượng</th>
              <th style={{ padding: '12px 16px', textAlign: 'center' }}>Hạn mức</th>
              <th style={{ padding: '12px 16px', textAlign: 'center' }}>Đang mượn</th>
              <th style={{ padding: '12px 16px' }}>Đơn giá phạt</th>
              <th style={{ padding: '12px 16px', textAlign: 'center' }}>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan="8" style={{ padding: '32px', textAlign: 'center', color: '#6b7280' }}>
                  Đang tải danh sách...
                </td>
              </tr>
            ) : filteredReaders.length === 0 ? (
              <tr>
                <td colSpan="8" style={{ padding: '32px', textAlign: 'center', color: '#6b7280' }}>
                  Không tìm thấy bạn đọc nào phù hợp.
                </td>
              </tr>
            ) : (
              filteredReaders.map((r) => (
                <tr key={r.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                  <td style={{ padding: '12px 16px', fontWeight: '600', color: '#111827' }}>{r.id}</td>
                  <td style={{ padding: '12px 16px' }}>{r.fullName}</td>
                  <td style={{ padding: '12px 16px', color: '#4b5563' }}>{r.phone}</td>
                  <td style={{ padding: '12px 16px' }}>{getTypeBadge(r.type)}</td>
                  <td style={{ padding: '12px 16px', textAlign: 'center', fontWeight: 'bold' }}>{r.maxBorrowLimit} cuốn</td>
                  <td style={{ padding: '12px 16px', textAlign: 'center' }}>
                    <span
                      style={{
                        padding: '2px 8px',
                        borderRadius: '10px',
                        fontWeight: 'bold',
                        backgroundColor: r.currentBorrowCount > 0 ? '#fef3c7' : '#f3f4f6',
                        color: r.currentBorrowCount > 0 ? '#b45309' : '#6b7280',
                      }}
                    >
                      {r.currentBorrowCount || 0}
                    </span>
                  </td>
                  <td style={{ padding: '12px 16px', color: '#dc2626', fontSize: '13px' }}>{getFineDescription(r.type)}</td>
                  <td style={{ padding: '12px 16px', textAlign: 'center' }}>
                    <button
                      onClick={() => handleOpenEditModal(r)}
                      style={{
                        padding: '6px 12px',
                        backgroundColor: '#f3f4f6',
                        color: '#1f2937',
                        border: '1px solid #d1d5db',
                        borderRadius: '4px',
                        cursor: 'pointer',
                        marginRight: '8px',
                      }}
                    >
                      Sửa
                    </button>
                    <button
                      onClick={() => handleDeleteReader(r)}
                      style={{
                        padding: '6px 12px',
                        backgroundColor: '#fee2e2',
                        color: '#b91c1c',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer',
                      }}
                    >
                      Xóa
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Modal Thêm / Sửa bạn đọc */}
      {showModal && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
          }}
        >
          <div
            style={{
              backgroundColor: '#fff',
              borderRadius: '8px',
              padding: '24px',
              width: '100%',
              maxWidth: '460px',
              boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
            }}
          >
            <h2 style={{ margin: '0 0 16px', fontSize: '18px', color: '#111827' }}>
              {modalMode === 'CREATE' ? 'Thêm mới Bạn đọc' : `Chỉnh sửa Bạn đọc (${currentReader.id})`}
            </h2>

            <form onSubmit={handleSaveReader}>
              <div style={{ marginBottom: '14px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', marginBottom: '6px', color: '#374151' }}>
                  Họ và tên *
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ví dụ: Nguyễn Văn A"
                  value={currentReader.fullName}
                  onChange={(e) => setCurrentReader({ ...currentReader, fullName: e.target.value })}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '4px', border: '1px solid #d1d5db', boxSizing: 'border-box' }}
                />
              </div>

              <div style={{ marginBottom: '14px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', marginBottom: '6px', color: '#374151' }}>
                  Số điện thoại *
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ví dụ: 0901234567"
                  value={currentReader.phone}
                  onChange={(e) => setCurrentReader({ ...currentReader, phone: e.target.value })}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '4px', border: '1px solid #d1d5db', boxSizing: 'border-box' }}
                />
              </div>

              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', marginBottom: '6px', color: '#374151' }}>
                  Loại đối tượng *
                </label>
                <select
                  value={currentReader.type}
                  onChange={(e) => setCurrentReader({ ...currentReader, type: e.target.value })}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '4px', border: '1px solid #d1d5db', boxSizing: 'border-box' }}
                >
                  <option value="STUDENT">Sinh viên thường (Hạn mức: 3 cuốn, Phạt: 5.000đ/ngày)</option>
                  <option value="PRIORITY_STUDENT">Sinh viên ưu tiên (Hạn mức: 5 cuốn, Phạt: 3.000đ/ngày)</option>
                  <option value="LECTURER">Giảng viên (Hạn mức: 10 cuốn, Phạt: 2.000đ/ngày)</option>
                </select>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  style={{
                    padding: '8px 16px',
                    backgroundColor: '#e5e7eb',
                    color: '#374151',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer',
                  }}
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  style={{
                    padding: '8px 16px',
                    backgroundColor: '#2563eb',
                    color: '#fff',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    fontWeight: '600',
                  }}
                >
                  {modalMode === 'CREATE' ? 'Thêm mới' : 'Lưu thay đổi'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

