// @ts-check
import { defineConfig, devices } from '@playwright/test';

/**
 * @see https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  testDir: './',
  /* 每个测试超时时间 */
  timeout: 30000,
  expect: {
    timeout: 10000,
  },
  /* 测试执行重试 */
  retries: process.env.CI ? 2 : 0,
  /* 并行执行 */
  workers: process.env.CI ? 2 : undefined,
  /* 输出目录 */
  outputDir: './test-results/',
  /* 报告 */
  reporter: [
    ['html', { outputFolder: './playwright-report' }],
    ['json', { outputFile: './playwright-report/results.json' }],
    ['list'],
  ],
  /* 全局设置 */
  globalSetup: undefined,
  /* 使用配置 */
  use: {
    /* 基础 URL */
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:5173',
    /* 追踪 */
    trace: 'on-first-retry',
    /* 截图 */
    screenshot: 'only-on-failure',
    /* 录制视频 */
    video: 'retain-on-failure',
  },

  /* 浏览器配置 */
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    // 可选：跨浏览器测试
    // {
    //   name: 'firefox',
    //   use: { ...devices['Desktop Firefox'] },
    // },
    // {
    //   name: 'webkit',
    //   use: { ...devices['Desktop Safari'] },
    // },
  ],
});
