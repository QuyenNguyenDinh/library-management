
import { Navigate, Outlet, useLocation } from 'react-router-dom'

export default function RequireAuth() {
  const location = useLocation()

  let user = null

  try {
    user = JSON.parse(sessionStorage.getItem('libraryUser'))
  } catch {
    sessionStorage.removeItem('libraryUser')
  }

  if (!user) {
    return (
      <Navigate
        to="/login"
        state={{ from: location.pathname + location.search }}
        replace
      />
    )
  }

  return <Outlet />
}
