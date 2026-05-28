import { Link } from 'react-router-dom'
import { useLocale } from '../LocaleContext'

export default function NotFound() {
  const { t } = useLocale()

  return (
    <div className="animate-fade-up max-w-md mx-auto mt-20 text-center">
      <div className="glass-card rounded-3xl p-12">
        <div className="relative mb-6">
          <span className="text-7xl font-serif font-bold text-jade-200 select-none">404</span>
          <div className="absolute inset-0 flex items-center justify-center">
            <span className="text-4xl">🤷</span>
          </div>
        </div>

        <h2 className="font-serif text-xl font-semibold text-jade-700 mb-2">
          {t('notFound.title')}
        </h2>
        <p className="text-sm text-ink-soft mb-8">
          {t('notFound.desc')}
        </p>

        <Link to="/" className="btn-jade inline-block text-sm py-3 px-8">
          {t('notFound.backHome')}
        </Link>
      </div>
    </div>
  )
}
