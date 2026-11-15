// API cho phân hệ Mượn sách (TV1 - Quyền). Dùng client chung: thành công trả về data, lỗi ném Error(message).
import { api } from './client.js'

const withKeyword = (path, keyword) => (keyword ? `${path}?keyword=${encodeURIComponent(keyword)}` : path)

export const borrowApi = {
  /** Bạn đọc kèm maxBorrowLimit, currentBorrowCount (API của Sáng). */
  getReaders: (keyword = '') => api.get(withKeyword('/api/readers', keyword)),

  /** Sách, tìm theo mã hoặc tên (API của Hòa). */
  getBooks: (keyword = '') => api.get(withKeyword('/api/books', keyword)),

  /**
   * Lập phiếu mượn.
   * @param {{ readerId: string, items: {bookId: string, quantity: number}[], borrowDate?: string, dueDate?: string }} payload
   * @returns {Promise<object>} phiếu mượn vừa tạo
   */
  borrow: (payload) => api.post('/api/borrow', payload),
}
