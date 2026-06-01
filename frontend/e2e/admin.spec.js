// @ts-check
import { test, expect } from '@playwright/test';

test.describe('管理员后台 E2E 测试', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.waitForSelector('input', { timeout: 5000 });
    await page.locator('input').first().fill('admin');
    await page.locator('input[type="password"]').first().fill('admin123');
    await page.locator('button').filter({ hasText: /登录|登 录/ }).click();
    await page.waitForURL(/dashboard|home|index/, { timeout: 10000 });
  });

  test('访问读者管理页面', async ({ page }) => {
    await page.goto('/readers');
    await page.waitForURL(/reader/, { timeout: 5000 });
    await expect(page).toHaveURL(/reader/);
  });

  test('访问统计学���页面', async ({ page }) => {
    await page.goto('/statistics');
    await page.waitForTimeout(2000);
    // 验证统计图表组件渲染
    await expect(page.locator('.chart-container, canvas, .echarts')).toHaveCount(1, { timeout: 5000 });
  });

  test('访问公告管理页面', async ({ page }) => {
    await page.goto('/announcements');
    await page.waitForURL(/announcement/, { timeout: 5000 });
    await expect(page).toHaveURL(/announcement/);
  });

  test('读者角色无法访问管理页面', async ({ page }) => {
    // 使用读者账号登录
    await page.goto('/login');
    await page.waitForSelector('input', { timeout: 5000 });
    await page.locator('input').first().fill('reader01');
    await page.locator('input[type="password"]').first().fill('test123');
    await page.locator('button').filter({ hasText: /登录|登 录/ }).click();
    await page.waitForURL(/dashboard|home|index/, { timeout: 10000 });

    // 尝试访问管理员页面 - 应被重定向或返回403
    await page.goto('/readers');
    await page.waitForTimeout(2000);
    // 期望被重定向到登录页或显示无权限提示
    const url = page.url();
    expect(
      url.includes('login') || url.includes('403') || url.includes('unauthorized')
    ).toBeTruthy();
  });
});
