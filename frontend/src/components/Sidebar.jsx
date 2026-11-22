
import { NavLink, useNavigate } from 'react-router-dom'

export const NAV_ITEMS = [
  { to: '/books', label: 'Sách' },
  { to: '/readers', label: 'Bạn đọc' },
  { to: '/borrow', label: 'Mượn sách' },
  { to: '/return', label: 'Trả sách' },
  { to: '/tickets', label: 'Phiếu mượn' },
  { to: '/stats', label: 'Thống kê' },
]

export default function Sidebar() {
  const navigate = useNavigate()

  function handleLogout() {
    sessionStorage.removeItem('libraryUser')
    navigate('/login', { replace: true })
  }

  return (
    <aside className="sidebar">
      <div className="brand">
        <span className="brand-mark">TV</span>
        <div>
          <div className="brand-name">Quản lý thư viện</div>
        </div>
      </div>

      <nav>
        {NAV_ITEMS.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              isActive ? 'nav-link active' : 'nav-link'
            }
          >
            {item.label}
          </NavLink>
        ))}
      </nav>

      <button
        type="button"
        className="nav-link"
        onClick={handleLogout}
        style={{
          marginTop: 'auto',
          width: '100%',
          textAlign: 'left',
          background: 'transparent',
          border: 'none',
          font: 'inherit',
          cursor: 'pointer',
        }}
      >
        Đăng xuất
      </button>
    </aside>
  )
}
