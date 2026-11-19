
import { useState } from 'react'
import { Navigate, useNavigate, useLocation } from 'react-router-dom'
import { api } from '../api/client.js'

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const navigate = useNavigate()
  const location = useLocation()

  if (sessionStorage.getItem('libraryUser')) {
    return <Navigate to="/tickets" replace />
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setBusy(true)
    setError('')

    try {
      const response = await api.post('/api/auth/login', {
        username,
        password,
      })

      if (response?.success === false) {
        throw new Error(response.message || 'Đăng nhập thất bại')
      }

      const user = response?.data ?? response

      if (!user) {
        throw new Error('Không nhận được thông tin người dùng')
      }

      sessionStorage.setItem('libraryUser', JSON.stringify(user))

      navigate(location.state?.from || '/tickets', {
        replace: true,
      })
    } catch (err) {
      setError(err.message || 'Không thể đăng nhập')
    } finally {
      setBusy(false)
    }
  }

  return (
    <main
      style={{
        maxWidth: 420,
        margin: '8vh auto',
        padding: 28,
        background: '#fff',
        borderRadius: 14,
        border: '1px solid #e5e7eb',
      }}
    >
      <h1>Đăng nhập</h1>

      <form
        onSubmit={handleSubmit}
        style={{ display: 'grid', gap: 12 }}
      >
        <label>
          Tên đăng nhập
          <input
            required
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
        </label>

        <label>
          Mật khẩu
          <input
            required
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>

        {error && (
          <p style={{ color: 'crimson' }}>
            {error}
          </p>
        )}

        <button type="submit" disabled={busy}>
          {busy ? 'Đang đăng nhập...' : 'Đăng nhập'}
        </button>
      </form>
    </main>
  )
}
