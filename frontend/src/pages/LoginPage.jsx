
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
        background: 'var(--card)',
        borderRadius: 'var(--radius)',
        border: '1px solid var(--line)',
      }}
    >
      <h1>Đăng nhập</h1>

      <p className="muted" style={{ marginTop: 8, marginBottom: 24 }}>
        Hệ thống quản lý thư viện - Nhóm 04
      </p>

      <form
        onSubmit={handleSubmit}
        style={{ display: 'grid', gap: 12 }}
      >
        <label className="field">
          <span>Tên đăng nhập</span>
          <input
            className="input"
            required
            autoComplete="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
        </label>

        <label className="field">
          <span>Mật khẩu</span>
          <input
            className="input"
            required
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>

        {error && (
          <p className="small fine" role="alert">
            {error}
          </p>
        )}

        <button
          type="submit"
          className="btn btn-primary"
          disabled={busy}
          style={{ width: '100%', marginTop: 16 }}
        >
          {busy ? 'Đang đăng nhập...' : 'Đăng nhập'}
        </button>
      </form>
    </main>
  )
}
