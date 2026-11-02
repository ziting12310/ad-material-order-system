/**
 * TailwindCSS 配置 — 对齐 UI_UX_SPEC.md Design Token
 * 主色 #2563EB / 圆角 8px / 7 状态徽章色 / 浅色企业后台
 */
/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js}'],
  theme: {
    extend: {
      colors: {
        // 品牌主色阶（UI_UX_SPEC §Design Token）
        brand: {
          DEFAULT: '#2563EB',
          50: '#EFF6FF',
          100: '#DBEAFE',
          600: '#2563EB',
          700: '#1D4ED8',
          800: '#1E40AF'
        },
        // 7 状态徽章色（CREATED 灰 / ASSIGNED 蓝 / DESIGNING 橙 / WAIT_APPROVE 紫 / APPROVED 青 / DELIVERED 绿 / CANCELLED 深灰）
        status: {
          created: '#6B7280',
          assigned: '#2563EB',
          designing: '#F59E0B',
          waitApprove: '#8B5CF6',
          approved: '#06B6D4',
          delivered: '#10B981',
          cancelled: '#374151'
        }
      },
      borderRadius: {
        // 基准圆角 8px
        DEFAULT: '8px',
        md: '8px'
      },
      boxShadow: {
        card: '0 1px 3px 0 rgba(0,0,0,0.06)',
        dropdown: '0 4px 12px 0 rgba(0,0,0,0.10)'
      }
    }
  },
  plugins: []
}
