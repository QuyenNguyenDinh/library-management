/**
 * API client giao tiếp với backend cho phân hệ Quản lý Sách (TV3 - Hòa)
 */
import { api } from './client.js'

const withKeyword = (path, keyword) => (keyword ? `${path}?keyword=${encodeURIComponent(keyword)}` : path)

export const bookApi = {
  // Lấy danh sách sách (hỗ trợ tìm theo mã hoặc tên không phân biệt hoa thường)
  getAll: (keyword = '') => api.get(withKeyword('/api/books', keyword)),

  // Chi tiết sách theo ID
  getById: (id) => api.get(`/api/books/${id}`),

  // Kiểm tra tồn kho sách
  checkAvailability: (id, quantity = 1) =>
    api.get(`/api/books/${id}/availability?quantity=${encodeURIComponent(quantity)}`),

  // Thêm mới sách
  create: (bookData) => api.post('/api/books', bookData),

  // Cập nhật thông tin sách
  update: (id, bookData) => api.put(`/api/books/${id}`, bookData),

  // Xóa sách (backend tự chặn 409 nếu sách đang có phiếu mượn BORROWING)
  delete: (id) => api.del(`/api/books/${id}`),
}