import { useState, useEffect } from 'react'
import TestConnectionModal from './TestConnectionModal'
import { useLocale } from '../LocaleContext'

export default function ProviderSelector({ onSave, onClose, initialProvider, initialKey, initialModel, initialUrl, endpoint = '/api/config/key', filterType }) {
  const { t } = useLocale()
  const [providers, setProviders] = useState([])
  const [selected, setSelected] = useState(initialProvider || 'deepseek')
  const [apiKey, setApiKey] = useState(initialKey || '')
  const [model, setModel] = useState(initialModel || '')
  const [baseUrl, setBaseUrl] = useState(initialUrl || '')
  const [models, setModels] = useState([])
  const [refreshing, setRefreshing] = useState(false)
  const [refreshMsg, setRefreshMsg] = useState('')
  const [refreshError, setRefreshError] = useState(false)
  const [showTest, setShowTest] = useState(false)
  const [saving, setSaving] = useState(false)

  const currentProvider = providers.find(p => p.id === selected) || {}

  const localized = (provider, field) => {
    const key = `providerSelector.${field}_${provider.id}`
    const translated = t(key)
    return translated === key ? provider[field] : translated
  }

  useEffect(() => {
    const fetchUrl = filterType ? `/api/config/providers?type=${filterType}` : '/api/config/providers'
    fetch(fetchUrl)
      .then(r => r.json())
      .then(data => {
        const list = Array.isArray(data) ? data : []
        setProviders(list)
        // 自动选第一个模型的默认值
        const sel = list.find(p => p.id === (initialProvider || (list[0]?.id ?? 'deepseek')))
        if (!initialModel && sel?.models?.[0]) setModel(sel.models[0].id)
        if (!initialUrl && sel?.default_url) setBaseUrl(sel.default_url)
      })
  }, [filterType])

  // 切换 Provider 时自动切模型和 URL
  useEffect(() => {
    const sel = providers.find(p => p.id === selected)
    if (sel && !initialModel) {
      setModels(sel.models || [])
      if (sel.models?.length > 0) setModel(sel.models[0].id)
      if (sel.default_url && !initialUrl) setBaseUrl(sel.default_url)
    }
  }, [selected, providers])

  const refreshModels = async () => {
    if (!apiKey.trim()) return setRefreshMsg(t('providerSelector.enterKeyFirst'))
    setRefreshing(true)
    setRefreshMsg('')
    try {
      const r = await fetch(`/api/config/providers/${selected}/refresh-models`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ apiKey, baseUrl }),
      })
      const data = await r.json()
      if (data.success) {
        setModels(data.models)
        setRefreshMsg(t('providerSelector.modelsFetched', { count: data.models.length }))
        setRefreshError(false)
        setTimeout(() => setRefreshMsg(''), 3000)
      } else {
        setRefreshMsg(data.error)
        setRefreshError(true)
      }
    } catch {
      setRefreshMsg(t('providerSelector.networkError'))
      setRefreshError(true)
    } finally {
      setRefreshing(false)
    }
  }

  const handleSave = async () => {
    setSaving(true)
    await fetch(endpoint, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ provider: selected, apiKey, baseUrl, model }),
    })
    onSave?.({ provider: selected, apiKey, model, baseUrl })
    setSaving(false)
  }

  return (
    <>
      <div className="fixed inset-0 z-[200] flex items-center justify-center"
           style={{ background: 'rgba(26,26,46,0.55)', backdropFilter: 'blur(10px)' }}>
        <div className="glass-panel rounded-3xl w-full max-w-lg p-8 mx-5 animate-fade-up max-h-[85vh] overflow-y-auto relative">
          {/* 右上角关闭按钮 */}
          <button
            onClick={() => onClose?.()}
            className="absolute top-4 right-4 w-8 h-8 rounded-full flex items-center justify-center text-ink-soft/40 hover:text-ink-soft hover:bg-jade-50 transition-all"
            title={t('common.close')}
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>

          <h2 className="font-serif text-xl font-semibold text-jade-700 mb-6">{t('providerSelector.selectProvider')}</h2>

          {/* Provider 列表 */}
          <div className="grid grid-cols-2 gap-3 mb-6">
            {providers.map(p => (
              <div key={p.id}
                onClick={() => setSelected(p.id)}
                className={`cursor-pointer rounded-2xl p-4 transition-all duration-300 ${
                  selected === p.id
                    ? 'ring-2 ring-jade-400/40 shadow-lg'
                    : 'hover:shadow-md opacity-70 hover:opacity-100'
                }`}
                style={{
                  background: selected === p.id
                    ? getGradient(p.id)
                    : 'rgba(255,255,255,0.6)',
                }}
              >
                <div className="flex items-center gap-2 mb-1">
                  <div className="w-8 h-8 rounded-lg flex items-center justify-center text-white font-bold text-sm"
                       style={{ background: 'rgba(255,255,255,0.2)' }}>
                    {getIcon(p.id)}
                  </div>
                  <span className={`font-semibold text-sm ${selected === p.id ? 'text-white drop-shadow-sm' : 'text-ink'}`}>
                    {localized(p, 'name')}
                  </span>
                </div>
                <p className={`text-xs leading-relaxed ${selected === p.id ? 'text-white/80 drop-shadow-sm' : 'text-ink-soft/50'}`}>
                  {localized(p, 'desc')}
                </p>
              </div>
            ))}
          </div>

          {/* Provider 详情配置 */}
          {selected && (
            <div className="space-y-4">
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="text-xs font-medium text-ink-soft">{t('providerSelector.model')}</label>
                  {(currentProvider.models_url || baseUrl.trim()) && (
                    <button onClick={refreshModels} disabled={refreshing}
                      className="text-[11px] text-jade-500 hover:text-jade-600 transition-colors">
                      {refreshing ? t('providerSelector.refreshing') : t('providerSelector.fetchModels')}
                    </button>
                  )}
                </div>
                {refreshMsg && (
                  <p className={`text-[10px] mb-2 ${refreshError ? 'text-red-400' : 'text-jade-500'}`}>
                    {refreshMsg}
                  </p>
                )}
                {models.length > 0 ? (
                  <div className="space-y-1.5">
                    {models.map(m => (
                      <label key={m.id} className={`flex items-center gap-3 rounded-xl px-4 py-3 cursor-pointer transition-all ${
                        model === m.id ? 'bg-jade-50 ring-1 ring-jade-400/30' : 'bg-white/40 hover:bg-white/60'
                      }`}>
                        <div className={`w-4 h-4 rounded-full border-2 flex items-center justify-center ${
                          model === m.id ? 'border-jade-400' : 'border-jade-600/15'
                        }`}>
                          {model === m.id && <div className="w-2 h-2 rounded-full bg-jade-400" />}
                        </div>
                        <input type="radio" value={m.id} checked={model === m.id}
                          onChange={() => setModel(m.id)} className="hidden" />
                        <div>
                          <span className="text-sm font-medium text-ink">{m.name}</span>
                          {m.desc && <span className="text-[10px] text-ink-soft/40 ml-2">{m.desc}</span>}
                        </div>
                      </label>
                    ))}
                  </div>
                ) : (
                  <input value={model} onChange={e => setModel(e.target.value)}
                    placeholder={t('providerSelector.modelPlaceholder')}
                    className="w-full rounded-xl px-4 py-3 text-sm bg-white/60 border border-jade-600/10 focus:outline-none focus:border-jade-400/40" />
                )}
              </div>

              <div>
                <label className="block text-xs font-medium text-ink-soft mb-1.5">API Key</label>
                <input type="password" value={apiKey} onChange={e => setApiKey(e.target.value)}
                  placeholder={currentProvider.id === 'anthropic' ? 'sk-ant-...' : 'sk-...'}
                  className="w-full rounded-xl px-4 py-3 text-sm bg-white/60 border border-jade-600/10 focus:outline-none focus:border-jade-400/40" />
              </div>

              <div>
                <label className="block text-xs font-medium text-ink-soft mb-1.5">
                  {t('providerSelector.baseUrl')} <span className="text-ink-soft/30">({currentProvider.default_url || t('providerSelector.providerCustom')})</span>
                </label>
                <input value={baseUrl} onChange={e => setBaseUrl(e.target.value)}
                  placeholder={currentProvider.default_url}
                  className="w-full rounded-xl px-4 py-3 text-sm bg-white/60 border border-jade-600/10 focus:outline-none focus:border-jade-400/40" />
              </div>

              <div className="flex items-center gap-3 pt-2">
                <button onClick={() => setShowTest(true)} disabled={!apiKey.trim() || !model.trim()}
                  className={`px-5 py-2.5 rounded-full text-sm font-medium bg-indigo-50 text-indigo-600 hover:bg-indigo-100 transition-all ${(!apiKey.trim() || !model.trim()) ? 'opacity-40 pointer-events-none' : ''}`}>
                  {t('providerSelector.testConnection')}
                </button>
                <button onClick={handleSave} disabled={!apiKey.trim() || !model.trim() || saving}
                  className={`btn-jade text-sm flex-1 ${(!apiKey.trim() || !model.trim() || saving) ? 'opacity-40 pointer-events-none' : ''}`}>
                  {saving ? t('providerSelector.saving') : t('providerSelector.save')}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {showTest && (
        <TestConnectionModal
          provider={selected} apiKey={apiKey}
          baseUrl={baseUrl || currentProvider.default_url || ''}
          model={model} type="chat"
          onClose={() => setShowTest(false)}
          onConnected={() => { handleSave(); setShowTest(false) }} />
      )}
    </>
  )
}

function getGradient(id) {
  const map = {
    deepseek: 'linear-gradient(160deg, #2563eb 0%, #1e40af 60%, #1e3a5f 100%)',
    openai: 'linear-gradient(135deg, #166534, #22c55e)',
    anthropic: 'linear-gradient(135deg, #92400e, #f59e0b)',
    custom: 'linear-gradient(135deg, #4338ca, #818cf8)',
    openai_image: 'linear-gradient(135deg, #065f46, #34d399)',
    volcengine_image: 'linear-gradient(135deg, #3730a3, #6366f1)',
    custom_image: 'linear-gradient(135deg, #5b21b6, #a78bfa)',
  }
  return map[id] || map.custom
}

function getIcon(id) {
  const map = { deepseek: 'D', openai: 'O', anthropic: 'C', custom: '⚡', openai_image: '🎨', volcengine_image: '🌋', custom_image: '🖼' }
  return map[id] || '?'
}
