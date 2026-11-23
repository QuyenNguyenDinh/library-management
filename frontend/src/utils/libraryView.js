
export const READER_TYPES = {
  STUDENT: {
    label: 'Sinh viên',
    limit: 3,
    dailyRate: 5000,
  },
  PRIORITY_STUDENT: {
    label: 'Sinh viên ưu tiên',
    limit: 5,
    dailyRate: 3000,
  },
  LECTURER: {
    label: 'Giảng viên',
    limit: 10,
    dailyRate: 2000,
  },
}

export function readerTypeLabel(type) {
  return READER_TYPES[type]?.label || type || 'Không xác định'
}

export function daysBetween(a, b) {
  if (!a || !b) return 0

  const toUTC = (date) => {
    const [year, month, day] = date.split('-').map(Number)
    return Date.UTC(year, month - 1, day)
  }

  return Math.round((toUTC(b) - toUTC(a)) / 86400000)
}

export function ticketCopies(ticket) {
  return (ticket?.details || []).reduce(
    (total, item) => total + Number(item.quantity || 0),
    0
  )
}

export function ticketBooksText(ticket) {
  return (ticket?.details || [])
    .map((item) => `${item.bookTitle || item.bookName || item.bookId || 'Sách'} × ${item.quantity || 0}`)
    .join(', ')
}

export function ticketState(ticket, today) {
  if (!ticket) {
    return {
      key: 'UNKNOWN',
      lateDays: 0,
      label: 'Không xác định',
      badge: 'badge',
    }
  }

  const dueDate = ticket.dueDate

  if (ticket.status === 'RETURNED') {
    const lateDays = ticket.returnDate
      ? Math.max(0, daysBetween(dueDate, ticket.returnDate))
      : 0

    return {
      key: 'RETURNED',
      lateDays,
      label: lateDays > 0
        ? `Đã trả (trễ ${lateDays} ngày)`
        : 'Đã trả',
      badge: 'badge',
    }
  }

  const currentDate = today || new Date().toLocaleDateString('en-CA')
  const remainingDays = daysBetween(currentDate, dueDate)

  if (remainingDays < 0) {
    const lateDays = Math.abs(remainingDays)

    return {
      key: 'OVERDUE',
      lateDays,
      label: `Quá hạn ${lateDays} ngày`,
      badge: 'badge badge-late',
    }
  }

  if (remainingDays === 0) {
    return {
      key: 'DUE_TODAY',
      lateDays: 0,
      label: 'Đến hạn hôm nay',
      badge: 'badge badge-warn',
    }
  }

  return {
    key: 'ON_TIME',
    lateDays: 0,
    label: `Trong hạn (còn ${remainingDays} ngày)`,
    badge: 'badge badge-ok',
  }
}

export function normalize(value) {
  return String(value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
}

export function matches(keyword, ...fields) {
  const search = normalize(keyword).trim()

  if (!search) return true

  return fields.some((field) =>
    normalize(field).includes(search)
  )
}

export function indexById(list) {
  return new Map(
    (list || []).map((item) => [item.id, item])
  )
}
