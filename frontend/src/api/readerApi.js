/**
 * API client giao tiếp với backend cho phân hệ Bạn đọc & Xác thực Thủ thư (TV2 - Sáng)
 */
const BASE_URL = '/api';

export const readerApi = {
  // Lấy danh sách bạn đọc (hỗ trợ lọc từ khóa)
  async getAll(keyword = '') {
    const url = keyword ? `${BASE_URL}/readers?keyword=${encodeURIComponent(keyword)}` : `${BASE_URL}/readers`;
    const response = await fetch(url);
    const result = await response.json();
    if (!response.ok) {
      throw new Error(result.message || 'Không thể tải danh sách bạn đọc.');
    }
    return result.data;
  },

  // Chi tiết bạn đọc theo ID
  async getById(id) {
    const response = await fetch(`${BASE_URL}/readers/${id}`);
    const result = await response.json();
    if (!response.ok) {
      throw new Error(result.message || 'Không tìm thấy bạn đọc.');
    }
    return result.data;
  },

  // Thêm mới bạn đọc
  async create(readerData) {
    const response = await fetch(`${BASE_URL}/readers`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(readerData),
    });
    const result = await response.json();
    if (!response.ok) {
      throw new Error(result.message || 'Thêm bạn đọc thất bại.');
    }
    return result.data;
  },

  // Cập nhật thông tin bạn đọc
  async update(id, readerData) {
    const response = await fetch(`${BASE_URL}/readers/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(readerData),
    });
    const result = await response.json();
    if (!response.ok) {
      throw new Error(result.message || 'Cập nhật bạn đọc thất bại.');
    }
    return result.data;
  },

  // Xóa bạn đọc
  async delete(id) {
    const response = await fetch(`${BASE_URL}/readers/${id}`, {
      method: 'DELETE',
    });
    const result = await response.json();
    if (!response.ok) {
      throw new Error(result.message || 'Xóa bạn đọc thất bại.');
    }
    return result;
  },

  // Đăng nhập thủ thư
  async login(username, password) {
    const response = await fetch(`${BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });
    const result = await response.json();
    if (!response.ok) {
      throw new Error(result.message || 'Đăng nhập không thành công.');
    }
    return result.data;
  },
};

