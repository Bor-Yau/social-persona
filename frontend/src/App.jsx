import { useState, useEffect } from 'react'
import { Routes, Route, useLocation } from 'react-router-dom'
import { LocaleProvider, useLocale } from './LocaleContext'
import LocaleModal from './components/LocaleModal'
import NavHeader from './components/NavHeader'
import OnboardingWizard from './components/OnboardingWizard'
import HealthIndicator from './components/HealthIndicator'
import ErrorBoundary from './components/ErrorBoundary'
import { ToastProvider } from './components/Toast'
import PersonaList from './pages/PersonaList'
import PersonaDetail from './pages/PersonaDetail'
import CreatePersona from './pages/CreatePersona'
import ManualCreate from './pages/ManualCreate'
import Settings from './pages/Settings'
import NotFound from './pages/NotFound'

function AnimatedRoutes() {
  const location = useLocation()
  const [displayLocation, setDisplayLocation] = useState(location)
  const [transitionStage, setTransitionStage] = useState('fadeIn')

  useEffect(() => {
    if (location.pathname !== displayLocation.pathname) {
      setTransitionStage('fadeOut')
      const timeout = setTimeout(() => {
        setDisplayLocation(location)
        setTransitionStage('fadeIn')
      }, 150)
      return () => clearTimeout(timeout)
    }
  }, [location, displayLocation])

  return (
    <div className={`transition-all duration-150 ${
      transitionStage === 'fadeIn' ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-2'
    }`}>
      <Routes location={displayLocation}>
        <Route path="/" element={<PersonaList />} />
        <Route path="/create" element={<CreatePersona />} />
        <Route path="/manual-create" element={<ManualCreate />} />
        <Route path="/persona/:id" element={<PersonaDetail />} />
        <Route path="/settings" element={<Settings />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </div>
  )
}

function AppInner() {
  const { t } = useLocale()
  const hasConfiguredInStorage = !!localStorage.getItem('hasConfigured')
  const [showWizard, setShowWizard] = useState(!hasConfiguredInStorage)

  useEffect(() => {
    fetch('/api/config/key')
      .then(res => res.json())
      .then(data => {
        if (data.hasKey === false) {
          setShowWizard(true)
        }
      })
      .catch(() => { console.error('API key config check failed') })
  }, [])

  useEffect(() => {
    document.title = t('app.title')
  }, [t])

  return (
    <ErrorBoundary>
      <ToastProvider>
      <div className="min-h-screen relative">
        <div className="data-dot" style={{ top: '12%', left: '8%', animation: 'float 7s ease-in-out infinite' }} />
        <div className="data-dot" style={{ top: '22%', right: '12%', animation: 'float 9s ease-in-out infinite 1s' }} />
        <div className="data-dot" style={{ top: '60%', left: '15%', animation: 'float 8s ease-in-out infinite 2s' }} />
        <div className="data-dot" style={{ top: '75%', right: '10%', animation: 'float 10s ease-in-out infinite 0.5s' }} />
        <div className="data-dot" style={{ top: '40%', left: '88%', animation: 'float 7.5s ease-in-out infinite 1.5s' }} />

        <div className="fixed -top-32 -left-32 w-96 h-96 rounded-full pointer-events-none z-0"
             style={{ background: 'radial-gradient(circle, rgba(82,183,136,0.08) 0%, transparent 70%)' }} />
        <div className="fixed -bottom-40 -right-20 w-[30rem] h-[30rem] rounded-full pointer-events-none z-0"
             style={{ background: 'radial-gradient(circle, rgba(45,106,79,0.06) 0%, transparent 70%)' }} />

        <LocaleModal />

        {showWizard && <OnboardingWizard onComplete={() => setShowWizard(false)} />}

        <div className="relative z-10">
          <NavHeader />
          <main className="max-w-5xl mx-auto px-5 py-8">
            <AnimatedRoutes />
          </main>
        </div>

        <HealthIndicator />
      </div>
      </ToastProvider>
    </ErrorBoundary>
  )
}

export default function App() {
  return (
    <LocaleProvider>
      <AppInner />
    </LocaleProvider>
  )
}
