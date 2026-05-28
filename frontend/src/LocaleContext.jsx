import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import zhCN from './locales/zh-CN.json'
import en from './locales/en.json'

const LOCALE_KEY = 'app_locale'
const LOCALES = { 'zh-CN': zhCN, en }

const LocaleContext = createContext(null)

export function LocaleProvider({ children }) {
  const [locale, setLocaleState] = useState(() => {
    const stored = localStorage.getItem(LOCALE_KEY)
    if (stored && LOCALES[stored]) return stored
    const nav = navigator.language || ''
    return nav.startsWith('zh') ? 'zh-CN' : 'en'
  })

  const setLocale = useCallback((lang) => {
    setLocaleState(lang)
    localStorage.setItem(LOCALE_KEY, lang)
  }, [])

  const t = useCallback((key, params) => {
    const keys = key.split('.')
    let val = LOCALES[locale]
    for (const k of keys) {
      if (val == null) break
      val = val[k]
    }
    if (typeof val !== 'string') return key
    if (params) {
      return val.replace(/\{(\w+)\}/g, (_, k) => params[k] ?? `{${k}}`)
    }
    return val
  }, [locale])

  return (
    <LocaleContext.Provider value={{ locale, setLocale, t }}>
      {children}
    </LocaleContext.Provider>
  )
}

export function useLocale() {
  const ctx = useContext(LocaleContext)
  if (!ctx) throw new Error('useLocale must be used within LocaleProvider')
  return ctx
}
