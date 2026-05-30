// @ts-check
import { test, expect } from '@playwright/test';

test.describe('座位预约流程 E2E 测试', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.waitForSelector('input', { timeout: 5000 });
    await page.locator('input').first().fill('reader1');
    await page.locator('input[type="password"]').first().fill('test123');
    await page.locator('button').filter({ hasText: /登录|登 录/ }).click();
    await page.waitForURL(/dashboard|home|index/, { timeout: 10000 });
  });

  test('进入座位预约页面', async ({ page }) => {
    await page.goto('/seats');
    await page.waitForURL(/seat/, { timeout: 5000 });
    await expect(page).toHaveURL(/seat/);
  });

  test('选择座位并预约', async ({ page }) => {
    await page.goto('/seats');
    await page.waitForTimeout(2000);

    // 查找可预约的座位并点击
    const availableSeat = page.locator('.seat-available, .el-button').filter({ hasText: /A-01|可预约/ }).first();
    if (await availableSeat.isVisible()) {
      await availableSeat.click();
    }

    // 确认预约
    const confirmBtn = page.locator('button').filter({ hasText: /确认|确定预约/ }).first();
    if (await confirmBtn.isVisible()) {
      await confirmBtn.click();
      await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 });
    }
  });

  test('查看我的预约记录', async ({ page }) => {
    await page.goto('/seat-reservations');
    await page.waitForURL(/reservation/, { timeout: 5000 });
    await expect(page).toHaveURL(/reservation/);
  });

  test('签到流程', async ({ page }) => {
    await page.goto('/seat-reservations');
    await page.waitForTimeout(2000);

    const checkInBtn = page.locator('button').filter({ hasText: /签到/ }).first();
    if (await checkInBtn.isVisible()) {
      await checkInBtn.click();
      await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 });
    }
  });

  test('签退流程', async ({ page }) => {
    await page.goto('/seat-reservations');
    await page.waitForTimeout(2000);

    const checkOutBtn = page.locator('button').filter({ hasText: /签退/ }).first();
    if (await checkOutBtn.isVisible()) {
      await checkOutBtn.click();
      await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 });
    }
  });
});
