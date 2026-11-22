
import { useCallback, useEffect, useState } from 'react'
import { api, formatDate, formatMoney } from '../api/client.js'
import DataTable from '../components/DataTable.jsx'
import { useToast } from '../components/Toast.jsx'

export default function TicketsPage() {
  const [rows, setRows] = useState([])
  const [status, setStatus] = useState('')
  const [readerId, setReaderId] = useState('')
  const [searchReaderId, setSearchReaderId] = useState('')
  const [loading, setLoading] = useState(false)

  const toast = useToast()

  const load = useCallback(async (signal) => {
    setLoading(true)

    try {
      const q = new URLSearchParams()

      if (status) q.set('status', status)
      if (searchReaderId) q.set('readerId', searchReaderId)

      const url = '/api/tickets' + (q.toString() ? '?' + q.toString() : '')
      const data = await api.get(url)

      if (!signal?.aborted) {
        setRows(Array.isArray(data) ? data : [])
      }
    } catch (e) {
      if (!signal?.aborted) {
        toast.error(e.message || 'Không thể tải phiếu mượn')
      }
    } finally {
      if (!signal?.aborted) setLoading(false)
    }
  }, [status, searchReaderId, toast])

  useEffect(() => {
    const controller = new AbortController()
    load(controller.signal)
    return () => controller.abort()
  }, [load])

  function handleSearch(e) {
    e.preventDefault()
    setSearchReaderId(readerId.trim())
  }

  const columns = [
    { key: 'id', label: 'Mã phiếu' },
    { key: 'readerId', label: 'Mã bạn đọc' },
    {
      key: 'borrowDate',
      label: 'Ngày mượn',
      render: r => formatDate(r.borrowDate)
    },
    {
      key: 'dueDate',
      label: 'Hạn trả',
      render: r => formatDate(r.dueDate)
    },
    {
      key: 'daysBorrowed',
      label: 'Số ngày mượn',
      align: 'right'
    },
    {
      key: 'overdue',
      label: 'Quá hạn',
      render: r => r.overdue ? 'Có' : 'Không'
    },
    {
      key: 'status',
      label: 'Trạng thái',
      render: r => {
        if (r.status === 'RETURNED') {
          return <span className="badge">Đã trả</span>
        }

        if (r.overdue) {
          return <span className="badge badge-late">Quá hạn</span>
        }

        return <span className="badge badge-ok">Trong hạn</span>
      }
    },
    {
      key: 'fineAmount',
      label: 'Tiền phạt',
      align: 'right',
      render: r => formatMoney(r.fineAmount)
    }
  ]

  return (
    <section className="page">
      <header className="page-head">
        <h1>Phiếu mượn</h1>
      </header>

      <form
        onSubmit={handleSearch}
        style={{
          display: 'flex',
          gap: 12,
          margin: '16px 0',
          flexWrap: 'wrap'
        }}
      >
        <select
          className="input"
          value={status}
          onChange={e => setStatus(e.target.value)}
        >
          <option value="">Tất cả trạng thái</option>
          <option value="BORROWING">Đang mượn</option>
          <option value="OVERDUE">Quá hạn</option>
          <option value="RETURNED">Đã trả</option>
        </select>

        <input
          className="input"
          placeholder="Mã bạn đọc"
          value={readerId}
          onChange={e => setReaderId(e.target.value)}
        />

        <button type="submit" className="btn btn-ghost">
          Tìm
        </button>

        <button
          type="button"
          className="btn btn-ghost"
          onClick={() => load()}
        >
          Tải lại
        </button>
      </form>

      <DataTable
        columns={columns}
        rows={rows}
        loading={loading}
      />
    </section>
  )
}
