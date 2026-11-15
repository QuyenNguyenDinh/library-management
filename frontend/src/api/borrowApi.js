/**
 * API client cho phân hệ Mượn sách (TV1).
 * Dùng chung đường dẫn /api (Vite proxy sang backend :8080).
 */
const BASE_URL = '/api';

async function request(url, options) {
  const response = await fetch(url, options);
  let result = null;
  try {
    result = await response.json();
  } catch {
    // Response không phải JSON (ví dụ backend chưa chạy)
  }
  if (!response.ok || (result && result.success === false)) {
    throw new Error((result && result.message) || `Lỗi kết nối máy chủ (HTTP ${response.status}).`);
  }
  return result;
}

export const borrowApi = {
  /** Danh sách bạn đọc kèm maxBorrowLimit, currentBorrowCount (API của TV2). */
  async getReaders(keyword = '') {
    const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : '';
    const result = await request(`${BASE_URL}/readers${query}`);
    return result.data;
  },

  /** Danh sách sách, có thể tìm theo mã hoặc tên (API của TV3). */
  async getBooks(keyword = '') {
    const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : '';
    const result = await request(`${BASE_URL}/books${query}`);
    return result.data;
  },

  /**
   * Lập phiếu mượn.
   * @param {{ readerId: string, items: {bookId: string, quantity: number}[], borrowDate?: string, dueDate?: string }} payload
   * @returns {Promise<{ message: string, ticket: object }>}
   */
  async borrow(payload) {
    const result = await request(`${BASE_URL}/borrow`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    return { message: result.message, ticket: result.data };
  },
};
