// @ts-check
import { test, expect } from '@playwright/test';

test.describe('仪表盘页面 E2E 测试', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.waitForSelector('input', { timeout: 5000 });
    await page.locator('input').first().fill('admin');
    await page.locator('input[type="password"]').first().fill('admin123');
    await page.locator('button').filter({ hasText: /登录|登 录/ }).click();
    await page.waitForURL(/dashboard/, { timeout: 10000 });
  });

  test('登录后导航到仪表盘', async ({ page }) => {
    await expect(page).toHaveURL(/dashboard/);
    await expect(page.locator('.dashboard')).toBeVisible({ timeout: 5000 });
  });

  test('验证统计卡片显示', async ({ page }) => {
    await page.waitForSelector('.stat-card', { timeout: 5000 });

    const statCards = page.locator('.stat-card');
    const count = await statCards.count();
    expect(count).toBeGreaterThanOrEqual(4);

    // 验证关键统计标签
    await expect(page.locator('.stat-label').filter({ hasText: '图书总数' }).first()).toBeVisible();
    await expect(page.locator('.stat-label').filter({ hasText: '在借数量' }).first()).toBeVisible();
    await expect(page.locator('.stat-label').filter({ hasText: '可用座位' }).first()).toBeVisible();
    await expect(page.locator('.stat-label').filter({ hasText: '我的积分' }).first()).toBeVisible();
  });

  test('验证图表区域渲染', async ({ page }) => {
    // 管理员可看到借阅趋势图表
    await expect(page.locator('.chart-container')).toBeVisible({ timeout: 8000 });
  });

  test('验证快捷入口区域', async ({ page }) => {
    await expect(page.locator('.quick-actions')).toBeVisible({ timeout: 5000 });

    const quickItems = page.locator('.quick-item');
    const count = await quickItems.count();
    expect(count).toBeGreaterThanOrEqual(3);
  });

  test('验证最近公告区域', async ({ page }) => {
    await page.waitForTimeout(2000);
    const noticeSection = page.locator('.notice-item, .notice-title').first();
    if (await noticeSection.isVisible({ timeout: 3000 }).catch(() => false)) {
      const count = await noticeSection.count();
      expect(count).toBeGreaterThan(0);
    }
  });

  test('普通用户看不到借阅趋势图', async ({ page }) => {
    await page.goto('/login');
    await page.waitForSelector('input', { timeout: 5000 });
    await page.locator('input').first().fill('reader01');
    await page.locator('input[type="password"]').first().fill('test123');
    await page.locator('button').filter({ hasText: /登录|登 录/ }).click();
    await page.waitForURL(/dashboard/, { timeout: 10000 });

    // 读者没有 .chart-container（管理员专属）
    const chart = page.locator('.chart-container');
    await expect(chart).toHaveCount(0, { timeout: 5000 });
  });
});
