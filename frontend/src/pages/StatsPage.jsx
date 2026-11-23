
import { useEffect, useState } from 'react'
import { api, formatMoney } from '../api/client.js'
import DataTable from '../components/DataTable.jsx'
import { useToast } from '../components/Toast.jsx'

export default function StatsPage() {
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)
  const toast = useToast()

  useEffect(() => {
    let active = true

    async function loadStats() {
      try {
        const data = await api.get('/api/stats')
        if (active) setStats(data)
      } catch (e) {
        if (active) {
          toast.error(e.message || 'Không thể tải thống kê')
        }
      } finally {
        if (active) setLoading(false)
      }
    }

    loadStats()

    return () => {
      active = false
    }
  }, [])

  const topBooksColumns = [
    { key: 'bookId', label: 'Mã sách' },
    { key: 'title', label: 'Tên sách' },
    {
      key: 'borrowCount',
      label: 'Số lượng mượn',
      align: 'right'
    }
  ]

  const lowStockColumns = [
    { key: 'id', label: 'Mã sách' },
    { key: 'title', label: 'Tên sách' },
    {
      key: 'availableQuantity',
      label: 'Còn lại',
      align: 'right'
    }
  ]

  return (
    <section className="page">
      <header className="page-head">
        <h1>Thống kê</h1>
      </header>

      {stats && (
        <>
          <div
            style={{
              display: 'flex',
              gap: 16,
              flexWrap: 'wrap',
              margin: '20px 0'
            }}
          >
            <div className="card">
              <b>Phiếu quá hạn</b>
              <h2>{stats.overdueCount}</h2>
            </div>

            <div className="card">
              <b>Tổng tiền phạt</b>
              <h2>{formatMoney(stats.totalFine)}</h2>
            </div>
          </div>

          <h2>Sách được mượn nhiều</h2>
          <DataTable
            columns={topBooksColumns}
            rows={stats.topBooks || []}
          />

          <h2 style={{ marginTop: 24 }}>Sách sắp hết</h2>
          <DataTable
            columns={lowStockColumns}
            rows={stats.lowStockBooks || []}
          />
        </>
      )}

      {loading && <p>Đang tải thống kê…</p>}
    </section>
  )
}
