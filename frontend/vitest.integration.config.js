import { defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    environment: 'node',
    globals: true,
    include: ['src/__tests__/integration/**/*.{test,spec}.{js,ts}'],
    exclude: ['node_modules', 'dist', 'e2e'],
    setupFiles: [],
    coverage: {
      enabled: false,
    },
    css: false,
  },
})
