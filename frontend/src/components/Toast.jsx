import { createContext, useContext, useState, useCallback } from 'react'

// ============================================================
// Toast 通知系统
// ============================================================
// 提供全局 toast.success / toast.error / toast.info / toast.warning 方法
// 通过 React Context 在应用根级别挂载 Toast 容器
//
// 使用方式：
//   const toast = useToast()
//   toast.success('保存成功')
//   toast.error('网络错误')
// ============================================================

const ToastContext = createContext(null)

let globalToast = null

export function useToast() {
  return useContext(ToastContext)
}

// 供非 React 上下文使用的全局调用（如工具函数中）
export function toastSuccess(message) { globalToast?.success(message) }
export function toastError(message) { globalToast?.error(message) }
export function toastInfo(message) { globalToast?.info(message) }
export function toastWarning(message) { globalToast?.warning(message) }

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const remove = useCallback((id) => {
    setToasts(prev => prev.filter(t => t.id !== id))
  }, [])

  const add = useCallback((message, type = 'info') => {
    const id = Date.now() + Math.random()
    const newToast = { id, message, type }
    setToasts(prev => {
      const next = [...prev, newToast]
      return next.length > 3 ? next.slice(next.length - 3) : next
    })
    setTimeout(() => remove(id), 3000)
    return id
  }, [remove])

  const success = useCallback((msg) => add(msg, 'success'), [add])
  const error = useCallback((msg) => add(msg, 'error'), [add])
  const info = useCallback((msg) => add(msg, 'info'), [add])
  const warning = useCallback((msg) => add(msg, 'warning'), [add])

  const api = { success, error, info, warning }
  globalToast = api

  return (
    <ToastContext.Provider value={api}>
      {children}
      <ToastContainer toasts={toasts} onRemove={remove} />
    </ToastContext.Provider>
  )
}

function ToastContainer({ toasts, onRemove }) {
  if (toasts.length === 0) return null

  return (
    <div className="fixed top-4 right-4 z-[200] flex flex-col gap-2">
      {toasts.map((toast, index) => (
        <ToastItem
          key={toast.id}
          toast={toast}
          index={index}
          onRemove={onRemove}
        />
      ))}
    </div>
  )
}

function ToastItem({ toast, index, onRemove }) {
  const typeStyles = {
    success: {
      bg: 'bg-jade-50',
      border: 'border-jade-400/30',
      text: 'text-jade-700',
      icon: (
        <svg className="w-4 h-4 text-jade-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
          <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
        </svg>
      ),
    },
    error: {
      bg: 'bg-red-50',
      border: 'border-red-400/30',
      text: 'text-red-700',
      icon: (
        <svg className="w-4 h-4 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
          <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
        </svg>
      ),
    },
    info: {
      bg: 'bg-blue-50',
      border: 'border-blue-400/30',
      text: 'text-blue-700',
      icon: (
        <svg className="w-4 h-4 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
          <path strokeLinecap="round" strokeLinejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      ),
    },
    warning: {
      bg: 'bg-amber-50',
      border: 'border-amber-400/30',
      text: 'text-amber-700',
      icon: (
        <svg className="w-4 h-4 text-amber-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
          <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
      ),
    },
  }

  const style = typeStyles[toast.type] || typeStyles.info

  return (
    <div
      className={`flex items-center gap-3 px-4 py-3 rounded-xl shadow-lg backdrop-blur-sm min-w-[240px] max-w-[360px]
        ${style.bg} ${style.border} border animate-toast-in`}
      style={{ animationDelay: `${index * 80}ms` }}
    >
      {style.icon}
      <span className={`text-sm font-medium flex-1 ${style.text}`}>{toast.message}</span>
      <button
        onClick={() => onRemove(toast.id)}
        className="text-ink-soft/40 hover:text-ink-soft transition-colors"
      >
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
          <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>
  )
}
