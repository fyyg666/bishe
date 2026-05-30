import { defineConfig, mergeConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      '~': resolve(__dirname, 'src/components'),
      'api': resolve(__dirname, 'src/api'),
      'views': resolve(__dirname, 'src/views'),
      'utils': resolve(__dirname, 'src/utils'),
      'store': resolve(__dirname, 'src/store'),
      'assets': resolve(__dirname, 'src/assets'),
    },
  },
  test: {
    // 测试环境
    environment: 'jsdom',
    // 全局注入
    globals: true,
    // 测试文件匹配
    include: ['src/__tests__/**/*.{test,spec}.{js,ts}'],
    // 排除目录
    exclude: ['node_modules', 'dist', 'e2e'],
    // 覆盖率
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html', 'lcov'],
      reportsDirectory: './coverage',
      include: ['src/**/*.{js,ts,vue}'],
      exclude: [
        'src/main.js',
        'src/__tests__/**',
        'e2e/**',
      ],
      thresholds: {
        statements: 65,
        branches: 55,
        functions: 65,
        lines: 65,
      },
    },
    // 在每个测试前执行 setup
    setupFiles: ['./src/__tests__/setup.js'],
    // 模拟 CSS 模块
    css: {
      modules: {
        classNameStrategy: 'non-scoped',
      },
    },
    // 转换配置
    deps: {
      inline: ['element-plus', '@element-plus/icons-vue'],
    },
  },
})
