import React, { useEffect, useMemo, useState } from 'react';
import { borrowApi } from '../api/borrowApi';

/**
 * Trang Mượn sách (TV1).
 * Quy trình: chọn bạn đọc -> tìm và thêm sách vào phiếu -> chỉnh số lượng, ngày -> lập phiếu.
 * Mọi kiểm tra nghiệp vụ (hạn mức, tồn kho) do backend quyết định; giao diện chỉ cảnh báo trước cho tiện.
 */

const DEFAULT_BORROW_DAYS = 14;

const READER_TYPE_LABELS = {
  STUDENT: 'Sinh viên',
  PRIORITY_STUDENT: 'Sinh viên ưu tiên',
  LECTURER: 'Giảng viên',
};

function toIsoDate(date) {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}

function addDays(isoDate, days) {
  const date = new Date(`${isoDate}T00:00:00`);
  date.setDate(date.getDate() + days);
  return toIsoDate(date);
}

function formatDate(isoDate) {
  if (!isoDate) return '';
  const [y, m, d] = isoDate.split('-');
  return `${d}/${m}/${y}`;
}

const styles = {
  page: { padding: '24px', maxWidth: '1200px', margin: '0 auto', fontFamily: 'inherit', color: '#111827' },
  header: { marginBottom: '20px' },
  title: { margin: 0, fontSize: '24px', fontWeight: 700 },
  subtitle: { margin: '4px 0 0', color: '#6b7280', fontSize: '14px' },
  grid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '20px' },
  card: { backgroundColor: '#fff', border: '1px solid #e5e7eb', borderRadius: '8px', padding: '20px' },
  cardTitle: { margin: '0 0 12px', fontSize: '16px', fontWeight: 600 },
  input: { width: '100%', boxSizing: 'border-box', padding: '10px 12px', borderRadius: '6px', border: '1px solid #d1d5db', fontSize: '14px' },
  label: { display: 'block', fontSize: '13px', fontWeight: 600, color: '#374151', marginBottom: '6px' },
  table: { width: '100%', borderCollapse: 'collapse', fontSize: '14px' },
  th: { textAlign: 'left', padding: '8px', borderBottom: '1px solid #e5e7eb', color: '#6b7280', fontWeight: 600 },
  td: { padding: '8px', borderBottom: '1px solid #f3f4f6', verticalAlign: 'middle' },
  primaryBtn: { padding: '10px 18px', backgroundColor: '#2563eb', color: '#fff', border: 'none', borderRadius: '6px', fontWeight: 600, cursor: 'pointer' },
  smallBtn: { padding: '6px 12px', backgroundColor: '#2563eb', color: '#fff', border: 'none', borderRadius: '6px', fontSize: '13px', cursor: 'pointer' },
  removeBtn: { padding: '6px 10px', backgroundColor: '#fff', color: '#b91c1c', border: '1px solid #fecaca', borderRadius: '6px', fontSize: '13px', cursor: 'pointer' },
  disabled: { opacity: 0.5, cursor: 'not-allowed' },
  success: { padding: '12px 16px', backgroundColor: '#dcfce7', color: '#166534', borderRadius: '6px', marginBottom: '16px' },
  error: { padding: '12px 16px', backgroundColor: '#fee2e2', color: '#991b1b', borderRadius: '6px', marginBottom: '16px' },
  warning: { padding: '10px 12px', backgroundColor: '#fef3c7', color: '#92400e', borderRadius: '6px', fontSize: '13px', marginTop: '12px' },
  muted: { color: '#6b7280', fontSize: '13px' },
};

export default function BorrowPage() {
  const [readers, setReaders] = useState([]);
  const [books, setBooks] = useState([]);
  const [readerKeyword, setReaderKeyword] = useState('');
  const [bookKeyword, setBookKeyword] = useState('');
  const [selectedReaderId, setSelectedReaderId] = useState('');
  const [cart, setCart] = useState([]); // [{ bookId, title, availableQuantity, quantity }]
  const [borrowDate, setBorrowDate] = useState(() => toIsoDate(new Date()));
  const [dueDate, setDueDate] = useState(() => addDays(toIsoDate(new Date()), DEFAULT_BORROW_DAYS));
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [createdTicket, setCreatedTicket] = useState(null);

  const loadData = async (readerKw = readerKeyword, bookKw = bookKeyword) => {
    setLoading(true);
    try {
      const [readerList, bookList] = await Promise.all([
        borrowApi.getReaders(readerKw),
        borrowApi.getBooks(bookKw),
      ]);
      setReaders(readerList || []);
      setBooks(bookList || []);
    } catch (err) {
      setErrorMsg(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData('', '');
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const selectedReader = useMemo(
    () => readers.find((r) => r.id === selectedReaderId) || null,
    [readers, selectedReaderId]
  );

  const cartTotal = cart.reduce((sum, item) => sum + (Number(item.quantity) || 0), 0);
  const currentCount = selectedReader?.currentBorrowCount ?? 0;
  const limit = selectedReader?.maxBorrowLimit ?? null;
  const remaining = limit === null ? null : limit - currentCount;
  const overLimit = remaining !== null && cartTotal > remaining;
  const overStock = cart.some((item) => Number(item.quantity) > item.availableQuantity);
  const invalidQuantity = cart.some((item) => !(Number(item.quantity) > 0));
  const invalidDates = !borrowDate || !dueDate || dueDate < borrowDate;
  const canSubmit = selectedReader && cart.length > 0 && !invalidQuantity && !invalidDates && !submitting;

  const handleSearchReaders = async (e) => {
    e.preventDefault();
    try {
      setReaders(await borrowApi.getReaders(readerKeyword));
    } catch (err) {
      setErrorMsg(err.message);
    }
  };

  const handleSearchBooks = async (e) => {
    e.preventDefault();
    try {
      setBooks(await borrowApi.getBooks(bookKeyword));
    } catch (err) {
      setErrorMsg(err.message);
    }
  };

  const addToCart = (book) => {
    setCreatedTicket(null);
    setCart((prev) => {
      const existing = prev.find((item) => item.bookId === book.id);
      if (existing) {
        return prev.map((item) =>
          item.bookId === book.id ? { ...item, quantity: Number(item.quantity) + 1 } : item
        );
      }
      return [...prev, { bookId: book.id, title: book.title, availableQuantity: book.availableQuantity, quantity: 1 }];
    });
  };

  const updateQuantity = (bookId, value) => {
    setCart((prev) => prev.map((item) => (item.bookId === bookId ? { ...item, quantity: value } : item)));
  };

  const removeFromCart = (bookId) => {
    setCart((prev) => prev.filter((item) => item.bookId !== bookId));
  };

  const handleBorrowDateChange = (value) => {
    setBorrowDate(value);
    if (value) setDueDate(addDays(value, DEFAULT_BORROW_DAYS));
  };

  const handleSubmit = async () => {
    setErrorMsg('');
    setCreatedTicket(null);
    setSubmitting(true);
    try {
      const { ticket } = await borrowApi.borrow({
        readerId: selectedReaderId,
        items: cart.map((item) => ({ bookId: item.bookId, quantity: Number(item.quantity) })),
        borrowDate,
        dueDate,
      });
      setCreatedTicket(ticket);
      setCart([]);
      await loadData(); // cập nhật lại tồn kho và số sách đang mượn
    } catch (err) {
      setErrorMsg(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={styles.page}>
      <div style={styles.header}>
        <h1 style={styles.title}>Mượn sách</h1>
        <p style={styles.subtitle}>Chọn bạn đọc, thêm sách vào phiếu rồi bấm "Lập phiếu mượn".</p>
      </div>

      {createdTicket && (
        <div style={styles.success}>
          Đã lập phiếu <strong>{createdTicket.id}</strong> cho bạn đọc <strong>{createdTicket.readerId}</strong>:{' '}
          {createdTicket.details.map((d) => `${d.bookTitle || d.bookId} × ${d.quantity}`).join(', ')}. Hạn trả{' '}
          <strong>{formatDate(createdTicket.dueDate)}</strong>.
        </div>
      )}
      {errorMsg && <div style={styles.error}>{errorMsg}</div>}

      <div style={styles.grid}>
        {/* Bước 1: chọn bạn đọc */}
        <section style={styles.card}>
          <h2 style={styles.cardTitle}>1. Bạn đọc</h2>
          <form onSubmit={handleSearchReaders} style={{ display: 'flex', gap: '8px', marginBottom: '12px' }}>
            <input
              style={styles.input}
              placeholder="Tìm theo mã RD, họ tên hoặc SĐT..."
              value={readerKeyword}
              onChange={(e) => setReaderKeyword(e.target.value)}
            />
            <button type="submit" style={styles.smallBtn}>Tìm</button>
          </form>

          <label style={styles.label} htmlFor="reader-select">Chọn bạn đọc</label>
          <select
            id="reader-select"
            style={styles.input}
            value={selectedReaderId}
            onChange={(e) => {
              setSelectedReaderId(e.target.value);
              setCreatedTicket(null);
              setErrorMsg('');
            }}
          >
            <option value="">-- Chọn bạn đọc --</option>
            {readers.map((r) => (
              <option key={r.id} value={r.id}>
                {r.id} — {r.fullName} ({READER_TYPE_LABELS[r.type] || r.type})
              </option>
            ))}
          </select>

          {selectedReader && (
            <div style={{ marginTop: '12px', fontSize: '14px', lineHeight: 1.7 }}>
              <div>Loại: <strong>{READER_TYPE_LABELS[selectedReader.type] || selectedReader.type}</strong></div>
              <div>
                Đang mượn: <strong>{currentCount}</strong> / {limit ?? '?'} cuốn
                {remaining !== null && <span style={styles.muted}> (còn được mượn {Math.max(remaining, 0)} cuốn)</span>}
              </div>
            </div>
          )}

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginTop: '16px' }}>
            <div>
              <label style={styles.label} htmlFor="borrow-date">Ngày mượn</label>
              <input id="borrow-date" type="date" style={styles.input} value={borrowDate}
                onChange={(e) => handleBorrowDateChange(e.target.value)} />
            </div>
            <div>
              <label style={styles.label} htmlFor="due-date">Hạn trả</label>
              <input id="due-date" type="date" style={styles.input} value={dueDate} min={borrowDate}
                onChange={(e) => setDueDate(e.target.value)} />
            </div>
          </div>
          {invalidDates && <div style={styles.warning}>Hạn trả không được trước ngày mượn.</div>}
        </section>

        {/* Bước 2: chọn sách */}
        <section style={styles.card}>
          <h2 style={styles.cardTitle}>2. Sách</h2>
          <form onSubmit={handleSearchBooks} style={{ display: 'flex', gap: '8px', marginBottom: '12px' }}>
            <input
              style={styles.input}
              placeholder="Tìm theo mã BK hoặc tên sách..."
              value={bookKeyword}
              onChange={(e) => setBookKeyword(e.target.value)}
            />
            <button type="submit" style={styles.smallBtn}>Tìm</button>
          </form>

          {loading ? (
            <p style={styles.muted}>Đang tải dữ liệu...</p>
          ) : books.length === 0 ? (
            <p style={styles.muted}>Không có sách phù hợp.</p>
          ) : (
            <div style={{ maxHeight: '360px', overflowY: 'auto' }}>
              <table style={styles.table}>
                <thead>
                  <tr>
                    <th style={styles.th}>Mã</th>
                    <th style={styles.th}>Tên sách</th>
                    <th style={styles.th}>Còn</th>
                    <th style={styles.th}></th>
                  </tr>
                </thead>
                <tbody>
                  {books.map((book) => {
                    const outOfStock = book.availableQuantity <= 0;
                    return (
                      <tr key={book.id}>
                        <td style={styles.td}>{book.id}</td>
                        <td style={styles.td}>{book.title}</td>
                        <td style={{ ...styles.td, color: outOfStock ? '#b91c1c' : undefined }}>{book.availableQuantity}</td>
                        <td style={{ ...styles.td, textAlign: 'right' }}>
                          <button
                            type="button"
                            style={{ ...styles.smallBtn, ...(outOfStock ? styles.disabled : {}) }}
                            disabled={outOfStock}
                            onClick={() => addToCart(book)}
                          >
                            {outOfStock ? 'Hết' : 'Thêm'}
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>

      {/* Bước 3: phiếu mượn */}
      <section style={{ ...styles.card, marginTop: '20px' }}>
        <h2 style={styles.cardTitle}>3. Phiếu mượn</h2>
        {cart.length === 0 ? (
          <p style={styles.muted}>Chưa có sách nào trong phiếu. Bấm "Thêm" ở danh sách sách.</p>
        ) : (
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>Mã</th>
                <th style={styles.th}>Tên sách</th>
                <th style={styles.th}>Còn trong kho</th>
                <th style={styles.th}>Số lượng mượn</th>
                <th style={styles.th}></th>
              </tr>
            </thead>
            <tbody>
              {cart.map((item) => (
                <tr key={item.bookId}>
                  <td style={styles.td}>{item.bookId}</td>
                  <td style={styles.td}>{item.title}</td>
                  <td style={styles.td}>{item.availableQuantity}</td>
                  <td style={styles.td}>
                    <input
                      type="number"
                      min="1"
                      max={item.availableQuantity}
                      value={item.quantity}
                      onChange={(e) => updateQuantity(item.bookId, e.target.value)}
                      style={{ ...styles.input, width: '90px' }}
                    />
                  </td>
                  <td style={{ ...styles.td, textAlign: 'right' }}>
                    <button type="button" style={styles.removeBtn} onClick={() => removeFromCart(item.bookId)}>Bỏ</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {invalidQuantity && <div style={styles.warning}>Số lượng mượn mỗi đầu sách phải lớn hơn 0.</div>}
        {overStock && <div style={styles.warning}>Có đầu sách vượt quá số lượng còn trong kho.</div>}
        {overLimit && (
          <div style={styles.warning}>
            Tổng {cartTotal} cuốn vượt hạn mức: bạn đọc chỉ còn được mượn {Math.max(remaining, 0)} cuốn.
          </div>
        )}

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '16px', flexWrap: 'wrap', gap: '12px' }}>
          <span style={styles.muted}>
            Tổng: <strong style={{ color: '#111827' }}>{cartTotal}</strong> cuốn
            {borrowDate && dueDate && !invalidDates && <> · Mượn {formatDate(borrowDate)} → hạn trả {formatDate(dueDate)}</>}
          </span>
          <button
            type="button"
            style={{ ...styles.primaryBtn, ...(canSubmit ? {} : styles.disabled) }}
            disabled={!canSubmit}
            onClick={handleSubmit}
          >
            {submitting ? 'Đang lập phiếu...' : 'Lập phiếu mượn'}
          </button>
        </div>
      </section>
    </div>
  );
}
