import { createContext, useCallback, useContext, useState } from 'react'

const ToastContext = createContext(null)

/** Dùng: const toast = useToast(); toast.success('...'); toast.error('...') */
export function ToastProvider({ children }) {
  const [items, setItems] = useState([])

  const push = useCallback((type, message) => {
    const id = Date.now() + Math.random()
    setItems((prev) => [...prev, { id, type, message }])
    setTimeout(() => setItems((prev) => prev.filter((t) => t.id !== id)), 4500)
  }, [])

  const toast = {
    success: (m) => push('success', m),
    error: (m) => push('error', m),
    info: (m) => push('info', m),
  }

  return (
    <ToastContext.Provider value={toast}>
      {children}
      <div className="toast-stack" role="status" aria-live="polite">
        {items.map((t) => (
          <div key={t.id} className={`toast toast-${t.type}`}>
            {t.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast phải nằm trong <ToastProvider>')
  return ctx
}
