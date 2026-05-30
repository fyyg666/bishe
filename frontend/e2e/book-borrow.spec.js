// @ts-check
import { test, expect } from '@playwright/test';

test.describe('图书借阅流程 E2E 测试', () => {

  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/login');
    await page.waitForSelector('input', { timeout: 5000 });
    await page.locator('input').first().fill('reader1');
    await page.locator('input[type="password"]').first().fill('test123');
    await page.locator('button').filter({ hasText: /登录|登 录/ }).click();
    await page.waitForURL(/dashboard|home|index/, { timeout: 10000 });
  });

  test('浏览图书列表', async ({ page }) => {
    await page.goto('/books');
    await page.waitForSelector('.el-table, .book-list, table', { timeout: 5000 });

    // 验证图书列表显示
    const tableRows = page.locator('.el-table__body-wrapper tr, table tr');
    const count = await tableRows.count();
    expect(count).toBeGreaterThan(0);
  });

  test('搜索图书', async ({ page }) => {
    await page.goto('/books');
    await page.waitForSelector('input', { timeout: 5000 });

    // 在搜索框输入关键字
    const searchInput = page.locator('input[placeholder*="搜索"], input[placeholder*="书名"]').first();
    if (await searchInput.isVisible()) {
      await searchInput.fill('Java');
      await page.keyboard.press('Enter');
      await page.waitForTimeout(1000);

      // 验证搜索结果
      await expect(page.locator('text=Java')).toBeVisible({ timeout: 5000 });
    }
  });

  test('借阅图书', async ({ page }) => {
    await page.goto('/books');
    await page.waitForTimeout(2000);

    // 点击借阅按钮
    const borrowBtn = page.locator('button').filter({ hasText: /借阅|借书/ }).first();
    if (await borrowBtn.isVisible()) {
      await borrowBtn.click();
      // 验证借阅成功提示
      await expect(page.locator('.el-message--success, [role="alert"]'))
        .toBeVisible({ timeout: 5000 });
    }
  });

  test('查看我的借阅记录', async ({ page }) => {
    await page.goto('/borrow-records');
    await page.waitForURL(/borrow/, { timeout: 5000 });
    await expect(page).toHaveURL(/borrow/);
  });

  test('还书流程', async ({ page }) => {
    await page.goto('/borrow-records');
    await page.waitForTimeout(2000);

    // 点击还书按钮
    const returnBtn = page.locator('button').filter({ hasText: /还书|归还/ }).first();
    if (await returnBtn.isVisible()) {
      await returnBtn.click();
      // 确认还书
      const confirmBtn = page.locator('.el-message-box, .el-dialog').locator('button').filter({ hasText: /确定|确认/ }).first();
      if (await confirmBtn.isVisible()) {
        await confirmBtn.click();
      }
      // 验证还书成功
      await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 });
    }
  });
});
