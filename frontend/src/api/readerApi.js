/**
 * API client giao tiếp với backend cho phân hệ Bạn đọc & Xác thực Thủ thư (TV2 - Sáng)
 */
import { api } from './client.js'

const withKeyword = (path, keyword) => (keyword ? `${path}?keyword=${encodeURIComponent(keyword)}` : path)

export const readerApi = {
  // Lấy danh sách bạn đọc (hỗ trợ lọc từ khóa)
  getAll: (keyword = '') => api.get(withKeyword('/api/readers', keyword)),

  // Chi tiết bạn đọc theo ID
  getById: (id) => api.get(`/api/readers/${id}`),

  // Thêm mới bạn đọc
  create: (readerData) => api.post('/api/readers', readerData),

  // Cập nhật thông tin bạn đọc
  update: (id, readerData) => api.put(`/api/readers/${id}`, readerData),

  // Xóa bạn đọc
  delete: (id) => api.del(`/api/readers/${id}`),

  // Đăng nhập thủ thư
  login: (username, password) => api.post('/api/auth/login', { username, password }),
}
