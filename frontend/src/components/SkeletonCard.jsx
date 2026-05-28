export default function SkeletonCard() {
  return (
    <div className="glass-card rounded-2xl p-6 animate-pulse">
      <div className="flex items-start gap-4">
        <div className="w-14 h-14 rounded-2xl bg-jade-600/8 flex-shrink-0" />
        <div className="flex-1 space-y-2">
          <div className="h-5 w-24 bg-jade-600/8 rounded-full" />
          <div className="h-3 w-48 bg-jade-600/6 rounded-full" />
        </div>
      </div>
      <div className="flex items-center gap-3 mt-4 pt-4 border-t border-jade-600/6">
        <div className="h-5 w-14 bg-jade-600/8 rounded-full" />
        <div className="h-5 w-12 bg-jade-600/8 rounded-full" />
        <div className="ml-auto h-3 w-10 bg-jade-600/6 rounded-full" />
      </div>
    </div>
  )
}
