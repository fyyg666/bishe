// @ts-check
import { test, expect } from '@playwright/test';

test.describe('认证流程 E2E 测试', () => {

  test('页面加载 - 显示登录界面', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('text=图书管理系统')).toBeVisible();
  });

  test('登录成功 - 使用管理员账号', async ({ page }) => {
    await page.goto('/login');
    await page.waitForSelector('input[type="text"], input[placeholder*="用户"]', { timeout: 5000 });

    // 填写登录表单
    const usernameInput = page.locator('input').first();
    const passwordInput = page.locator('input[type="password"]').first();

    await usernameInput.fill('admin');
    await passwordInput.fill('admin123');

    // 点击登录
    await page.locator('button').filter({ hasText: /登录|登 录/ }).click();

    // 期望跳转到首页或仪表盘
    await page.waitForURL(/dashboard|home|index/, { timeout: 10000 });
    await expect(page).toHaveURL(/dashboard|home|index/);
  });

  test('登录失败 - 错误密码显示提示', async ({ page }) => {
    await page.goto('/login');
    await page.waitForSelector('input', { timeout: 5000 });

    await page.locator('input').first().fill('admin');
    await page.locator('input[type="password"]').first().fill('wrongpassword');
    await page.locator('button').filter({ hasText: /登录|登 录/ }).click();

    // 期望看到错误提示
    await expect(page.locator('.el-message, .el-alert, [role="alert"]'))
      .toBeVisible({ timeout: 5000 });
  });

  test('登出流程', async ({ page }) => {
    // 先登录
    await page.goto('/login');
    await page.waitForSelector('input', { timeout: 5000 });
    await page.locator('input').first().fill('admin');
    await page.locator('input[type="password"]').first().fill('admin123');
    await page.locator('button').filter({ hasText: /登录|登 录/ }).click();
    await page.waitForURL(/dashboard|home|index/, { timeout: 10000 });

    // 点击退出按钮
    await page.locator('text=退出').click();

    // 期望返回登录页
    await expect(page).toHaveURL(/login/, { timeout: 5000 });
  });
});
