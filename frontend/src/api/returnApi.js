// API cho phân hệ Trả sách (TV4 - Đạt). Dùng client chung: thành công trả về data, lỗi ném Error(message).
import { api } from './client.js'

export const returnApi = {
  /** Các phiếu đang mượn (API của Sang: GET /api/tickets). */
  getBorrowingTickets: () => api.get('/api/tickets?status=BORROWING'),

  /** Lịch sử trả sách. */
  getReturnRecords: () => api.get('/api/return-records'),

  /**
   * Trả sách theo phiếu.
   * @param {string} ticketId mã phiếu mượn
   * @param {string} [returnDate] yyyy-MM-dd, bỏ trống thì backend lấy hôm nay
   * @returns {Promise<object>} bản ghi trả (overdueDays, fineAmount, policyName, ...)
   */
  returnBook: (ticketId, returnDate) => api.post(`/api/return/${encodeURIComponent(ticketId)}`, returnDate ? { returnDate } : {}),
}
