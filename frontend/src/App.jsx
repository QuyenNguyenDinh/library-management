
import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout.jsx'
import RequireAuth from './components/RequireAuth.jsx'
import LoginPage from './pages/LoginPage.jsx'
import ReturnPage from './pages/ReturnPage.jsx'
import BorrowPage from './pages/BorrowPage.jsx'
import ReadersPage from './pages/ReadersPage.jsx'
import BooksPage from './pages/BooksPage.jsx'
import TicketsPage from './pages/TicketsPage.jsx'
import StatsPage from './pages/StatsPage.jsx'

function Placeholder({ title, owner }) {
  return (
    <div className="page">
      <h1>{title}</h1>
      <p className="muted">
        Trang này do {owner} phụ trách, chưa được ghép vào khung.
      </p>
    </div>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route element={<RequireAuth />}>
        <Route element={<Layout />}>
          <Route path="/" element={<Navigate to="/return" replace />} />
          <Route path="/books" element={<BooksPage />} />
          <Route path="/readers" element={<ReadersPage />} />
          <Route path="/borrow" element={<BorrowPage />} />
          <Route path="/return" element={<ReturnPage />} />
          <Route path="/tickets" element={<TicketsPage />} />
          <Route path="/stats" element={<StatsPage />} />
        </Route>
      </Route>

      <Route
        path="*"
        element={
          <Placeholder title="Không tìm thấy trang" owner="nhóm" />
        }
      />
    </Routes>
  )
}
