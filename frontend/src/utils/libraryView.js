// Hàm dùng chung cho giao diện (phần của Sang). Bản này viết đúng theo mô tả để ReturnPage chạy được;
// Sang có bản chính thức thì thay file này, giữ nguyên tên export.
import { todayIso } from '../api/client.js'

export const READER_TYPES = {
  STUDENT: { label: 'Sinh viên thường', limit: 3, dailyRate: 5000 },
  PRIORITY_STUDENT: { label: 'Sinh viên ưu tiên', limit: 5, dailyRate: 3000 },
  LECTURER: { label: 'Giảng viên', limit: 10, dailyRate: 2000 },
}

export const readerTypeLabel = (type) => READER_TYPES[type]?.label ?? type ?? '—'

/** Số ngày từ a đến b (yyyy-MM-dd). Dùng Date.UTC để không lệch múi giờ. */
export function daysBetween(a, b) {
  const [y1, m1, d1] = a.split('-').map(Number)
  const [y2, m2, d2] = b.split('-').map(Number)
  return Math.round((Date.UTC(y2, m2 - 1, d2) - Date.UTC(y1, m1 - 1, d1)) / 86400000)
}

export const ticketCopies = (t) => (t.details || []).reduce((sum, d) => sum + (d.quantity || 0), 0)

export const ticketBooksText = (t) =>
  (t.details || []).map((d) => `${d.bookTitle || d.bookId} × ${d.quantity}`).join(', ')

/** @returns {{key: string, lateDays: number, label: string, badge: string}} */
export function ticketState(t, today = todayIso()) {
  if (t.status === 'RETURNED') {
    // API trả returnDate là "" khi chưa trả nên kiểm tra bằng truthy
    const lateDays = t.returnDate && t.dueDate ? Math.max(0, daysBetween(t.dueDate, t.returnDate)) : 0
    return {
      key: 'RETURNED',
      lateDays,
      label: lateDays > 0 ? `Đã trả (trễ ${lateDays} ngày)` : 'Đã trả',
      badge: 'badge',
    }
  }
  const diff = daysBetween(t.dueDate, today)
  if (diff > 0) return { key: 'OVERDUE', lateDays: diff, label: `Quá hạn ${diff} ngày`, badge: 'badge badge-late' }
  if (diff === 0) return { key: 'DUE_TODAY', lateDays: 0, label: 'Đến hạn hôm nay', badge: 'badge badge-warn' }
  return { key: 'ON_TIME', lateDays: 0, label: `Trong hạn (còn ${-diff} ngày)`, badge: 'badge badge-ok' }
}

/** Bỏ dấu tiếng Việt, đ -> d, chữ thường. */
export function normalize(s) {
  return String(s ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
}

export function matches(keyword, ...fields) {
  const k = normalize(keyword).trim()
  if (!k) return true
  return fields.some((f) => normalize(f).includes(k))
}

export function indexById(list) {
  return new Map((list || []).map((x) => [x.id, x]))
}
