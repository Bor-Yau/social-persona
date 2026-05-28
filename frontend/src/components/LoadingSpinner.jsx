export default function LoadingSpinner() {
  return (
    <div className="flex items-center gap-2 text-jade-500 animate-pulse">
      <svg className="w-5 h-5 animate-spin" viewBox="0 0 24 24" fill="none">
        <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" opacity="0.15" />
        <path d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
      </svg>
      <span className="text-sm text-jade-600">正在思考…</span>
    </div>
  )
}
