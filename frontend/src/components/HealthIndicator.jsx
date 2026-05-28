import { useState, useEffect, useRef } from 'react'

/**
 * 健康指示器组件
 * 显示三个核心服务的状态圆点，点击可展开详细面板
 */
export default function HealthIndicator() {
  const [status, setStatus] = useState({ java: false, python: false, redis: false })
  const [detail, setDetail] = useState(null)
  const [expanded, setExpanded] = useState(false)
  const [lastCheck, setLastCheck] = useState(null)
  const panelRef = useRef(null)

  useEffect(() => {
    const check = async () => {
      try {
        const r = await fetch('/api/health')
        const j = await r.json()
        setStatus({
          java: j.javaStatus === 'ok',
          python: j.pythonStatus?.status === 'ok',
          redis: j.pythonStatus?.memory_connected ?? false,
        })
        setDetail({
          javaVersion: j.javaStatus,
          pythonModel: j.pythonStatus?.model,
          pythonLatency: j.pythonStatus?.latency_ms,
          redisInfo: j.pythonStatus?.memory_type,
        })
        setLastCheck(new Date())
      } catch {
        setStatus({ java: false, python: false, redis: false })
        setDetail(null)
        setLastCheck(new Date())
      }
    }
    check()
    const iv = setInterval(check, 30000)
    return () => clearInterval(iv)
  }, [])

  // 点击外部关闭面板
  useEffect(() => {
    const handler = (e) => {
      if (panelRef.current && !panelRef.current.contains(e.target)) {
        setExpanded(false)
      }
    }
    if (expanded) {
      document.addEventListener('mousedown', handler)
      return () => document.removeEventListener('mousedown', handler)
    }
  }, [expanded])

  const dots = [
    { key: 'java', label: '引擎', icon: '⚙️' },
    { key: 'python', label: 'LLM', icon: '🧠' },
    { key: 'redis', label: '记忆', icon: '💾' },
  ]

  const allHealthy = status.java && status.python && status.redis

  return (
    <div className="fixed bottom-4 right-4 z-50" ref={panelRef}>
      {/* 展开面板 */}
      {expanded && (
        <div className="glass-panel rounded-2xl p-5 mb-3 w-64 animate-fade-up shadow-xl">
          <h4 className="font-serif text-sm font-semibold text-jade-700 mb-3">服务状态</h4>
          <div className="space-y-3">
            {dots.map(d => (
              <div key={d.key} className="flex items-center gap-3">
                <div className={`w-2 h-2 rounded-full flex-shrink-0 ${status[d.key] ? 'bg-jade-400' : 'bg-red-400'}`} />
                <span className="text-xs text-ink-soft w-8">{d.label}</span>
                <span className="text-xs text-ink flex-1">
                  {status[d.key] ? '正常' : '离线'}
                </span>
              </div>
            ))}
          </div>

          {/* 详细信息 */}
          {detail && (
            <div className="mt-3 pt-3 border-t border-jade-600/8 space-y-1.5">
              {detail.pythonModel && (
                <p className="text-[11px] text-ink-soft/60">
                  模型: <span className="text-ink-soft">{detail.pythonModel}</span>
                </p>
              )}
              {detail.pythonLatency !== undefined && (
                <p className="text-[11px] text-ink-soft/60">
                  延迟: <span className="text-ink-soft">{detail.pythonLatency}ms</span>
                </p>
              )}
              {detail.redisInfo && (
                <p className="text-[11px] text-ink-soft/60">
                  存储: <span className="text-ink-soft">{detail.redisInfo}</span>
                </p>
              )}
            </div>
          )}

          {lastCheck && (
            <p className="text-[10px] text-ink-soft/30 mt-3">
              上次检查: {lastCheck.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
            </p>
          )}
        </div>
      )}

      {/* 指示器圆点 */}
      <button
        onClick={() => setExpanded(v => !v)}
        className={`flex items-center gap-2 glass-panel rounded-full px-3 py-1.5 transition-all hover:shadow-md ${expanded ? 'ring-2 ring-jade-400/30' : ''}`}
        title="点击展开服务状态详情"
      >
        {dots.map(d => (
          <div key={d.key} className="flex items-center gap-1">
            <div className={`w-2 h-2 rounded-full transition-colors duration-500 ${
              status[d.key] ? 'bg-jade-400' : 'bg-red-400'
            }`} />
            <span className="text-[10px] text-ink-soft/50">{d.label}</span>
          </div>
        ))}
        {/* 全部正常时显示小勾 */}
        {allHealthy && (
          <span className="text-[10px] text-jade-500 ml-0.5">✓</span>
        )}
      </button>
    </div>
  )
}
