/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#effcf8',
          100: '#d6f5ec',
          500: '#0f766e',
          600: '#115e59',
          700: '#134e4a'
        }
      }
    }
  },
  plugins: []
}
