/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        paper: '#f9faf7',
        jade: {
          50: '#f0f7f4',
          100: '#d8ebe2',
          200: '#b2d7c5',
          300: '#83bfa4',
          400: '#52b788',
          500: '#40916c',
          600: '#2d6a4f',
          700: '#1b4332',
          800: '#081c15',
        },
        amber: {
          warm: '#e6a817',
          glow: '#f4c430',
        },
        ink: {
          DEFAULT: '#1a1a2e',
          soft: '#4a4a6a',
        },
      },
      fontFamily: {
        serif: ['"Noto Serif SC"', '"Source Han Serif SC"', 'Georgia', 'serif'],
        sans: ['"Noto Sans SC"', '"Source Han Sans SC"', 'system-ui', 'sans-serif'],
      },
      animation: {
        'fade-up': 'fadeUp 0.6s ease-out both',
        'float': 'float 6s ease-in-out infinite',
        'sheen': 'sheen 2s ease-in-out infinite',
        'grid-pulse': 'gridPulse 8s ease-in-out infinite',
      },
      keyframes: {
        fadeUp: {
          '0%': { opacity: '0', transform: 'translateY(24px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-8px)' },
        },
        sheen: {
          '0%': { transform: 'translateX(-100%)' },
          '100%': { transform: 'translateX(200%)' },
        },
        gridPulse: {
          '0%, 100%': { opacity: '0.03' },
          '50%': { opacity: '0.06' },
        },
      },
      backdropBlur: {
        xs: '2px',
      },
    },
  },
  plugins: [],
}
