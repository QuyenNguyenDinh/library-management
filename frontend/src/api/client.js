// Gọi backend. Backend luôn trả { success, message, data }.
// Thành công -> trả về data. Thất bại -> ném Error(message) kèm error.status (400/404/409/401/500).

async function request(method, url, body) {
  let res
  try {
    res = await fetch(url, {
      method,
      headers: body !== undefined ? { 'Content-Type': 'application/json' } : undefined,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    const err = new Error('Không kết nối được máy chủ. Kiểm tra backend đã chạy ở cổng 8080 chưa.')
    err.status = 0
    throw err
  }

  let payload = null
  try {
    payload = await res.json()
  } catch {
    // body rỗng hoặc không phải JSON
  }

  if (!res.ok || (payload && payload.success === false)) {
    const err = new Error(payload?.message || `Lỗi ${res.status}`)
    err.status = res.status
    throw err
  }
  return payload ? payload.data : null
}

export const api = {
  get: (url) => request('GET', url),
  post: (url, body = {}) => request('POST', url, body),
  put: (url, body) => request('PUT', url, body),
  del: (url) => request('DELETE', url),
}

export const formatMoney = (n) => `${Number(n || 0).toLocaleString('vi-VN')}đ`

export const formatDate = (iso) => {
  if (!iso) return '—'
  const [y, m, d] = iso.split('-')
  return `${d}/${m}/${y}`
}

export const todayIso = () => {
  const d = new Date()
  const pad = (x) => String(x).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
