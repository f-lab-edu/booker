/** @type {import('tailwindcss').Config} */
export default {
  content: ['./src/**/*.{js,ts,jsx,tsx,mdx}'],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#8b5cf6',
          light: '#a78bfa',
          dark: '#7c3aed',
        },
        violet: {
          400: '#a78bfa',
          500: '#8b5cf6',
          600: '#7c3aed',
        },
        accent: {
          pink: '#E91E63',
          blue: '#2196F3',
        },
        status: {
          borrowed: '#EF4444',
          available: '#10B981',
          waitlist: '#F59E0B',
          processing: '#F97316',
          shipped: '#3B82F6',
          delivered: '#10B981',
          cancelled: '#6B7280',
        },
      },
      fontWeight: {
        thin: '100',
        extralight: '200',
        light: '300',
        normal: '400',
        medium: '500',
        semibold: '600',
        bold: '700',
      },
    },
  },
};
