import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout.jsx'
import ReturnPage from './pages/ReturnPage.jsx'
import BorrowPage from './pages/BorrowPage.jsx'
import ReadersPage from './pages/ReadersPage.jsx'

// Trang của thành viên khác chưa merge -> hiện khung chờ.
// Khi trang thật có trên main: import nó và thay element tương ứng (mỗi người chỉ sửa đúng 1 dòng của mình).
function Placeholder({ title, owner }) {
  return (
    <div className="page">
      <h1>{title}</h1>
      <p className="muted">Trang này do {owner} phụ trách, chưa được ghép vào khung.</p>
    </div>
  )
}

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Navigate to="/return" replace />} />
        <Route path="/books" element={<Placeholder title="Quản lý sách" owner="Hòa" />} />
        <Route path="/readers" element={<ReadersPage />} />
        <Route path="/borrow" element={<BorrowPage />} />
        <Route path="/return" element={<ReturnPage />} />
        <Route path="/tickets" element={<Placeholder title="Phiếu mượn" owner="Sang" />} />
        <Route path="/stats" element={<Placeholder title="Thống kê" owner="Sang" />} />
        {/* Sang thêm /login và route guard sau khi có trang Đăng nhập */}
        <Route path="*" element={<Placeholder title="Không tìm thấy trang" owner="nhóm" />} />
      </Route>
    </Routes>
  )
}
