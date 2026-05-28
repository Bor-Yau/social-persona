import { Component } from 'react'
import { Link } from 'react-router-dom'

/**
 * 错误边界组件
 * 捕获子组件树中的 JavaScript 错误，防止整个应用崩溃
 */
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null, errorInfo: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  componentDidCatch(error, errorInfo) {
    this.setState({ errorInfo })
    // 生产环境可在此接入错误上报服务
  }

  render() {
    if (this.state.hasError) {
      const isDev = import.meta.env?.DEV

      return (
        <div className="min-h-screen flex items-center justify-center px-5">
          <div className="glass-card rounded-3xl p-10 text-center max-w-lg w-full animate-fade-up">
            {/* 图标 */}
            <div className="w-20 h-20 mx-auto mb-6 rounded-2xl flex items-center justify-center"
                 style={{ background: 'linear-gradient(135deg, #fef2f2, #fee2e2)' }}>
              <span className="text-3xl">😴</span>
            </div>

            {/* 主标题 */}
            <h3 className="font-serif text-xl font-semibold text-ink mb-2">
              引擎睡着了
            </h3>
            <p className="text-ink-soft text-sm mb-6 leading-relaxed">
              页面遇到了一点小问题。别担心，你的数据都还在。
              <br />试试下面的方法恢复。
            </p>

            {/* 恢复选项 */}
            <div className="flex flex-col sm:flex-row items-center justify-center gap-3 mb-6">
              <button
                onClick={() => window.location.reload()}
                className="btn-jade text-sm w-full sm:w-auto">
                刷新页面
              </button>
              <Link
                to="/"
                onClick={() => this.setState({ hasError: false, error: null, errorInfo: null })}
                className="px-5 py-2.5 rounded-full text-sm font-medium bg-jade-50 text-jade-600 hover:bg-jade-100 transition-all w-full sm:w-auto text-center">
                返回首页
              </Link>
            </div>

            {/* 排查提示 */}
            <div className="rounded-xl bg-jade-50/50 p-4 text-left">
              <p className="text-xs font-medium text-jade-700 mb-2">如果问题持续，请检查：</p>
              <ul className="text-xs text-ink-soft/70 space-y-1">
                <li className="flex items-start gap-2">
                  <span className="text-jade-400 mt-0.5">●</span>
                  Java 后端服务是否正常运行（端口 8080）
                </li>
                <li className="flex items-start gap-2">
                  <span className="text-jade-400 mt-0.5">●</span>
                  Python LLM 服务是否正常运行（端口 8000）
                </li>
                <li className="flex items-start gap-2">
                  <span className="text-jade-400 mt-0.5">●</span>
                  浏览器控制台是否有更多错误信息
                </li>
              </ul>
            </div>

            {/* 开发模式显示错误详情 */}
            {isDev && this.state.error && (
              <div className="mt-4 text-left">
                <details className="rounded-xl bg-red-50/50 p-4">
                  <summary className="text-xs font-medium text-red-600 cursor-pointer">
                    查看错误详情（开发模式）
                  </summary>
                  <pre className="mt-3 text-[11px] text-red-500 overflow-auto max-h-48 leading-relaxed">
                    {this.state.error.toString()}
                    {this.state.errorInfo?.componentStack}
                  </pre>
                </details>
              </div>
            )}
          </div>
        </div>
      )
    }
    return this.props.children
  }
}
