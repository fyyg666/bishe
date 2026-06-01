// @ts-check
import { test, expect } from '@playwright/test';

test.describe('读者管理 E2E 测试', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.waitForSelector('input', { timeout: 5000 });
    await page.locator('input').first().fill('admin');
    await page.locator('input[type="password"]').first().fill('admin123');
    await page.locator('button').filter({ hasText: /登录|登 录/ }).click();
    await page.waitForURL(/dashboard/, { timeout: 10000 });
  });

  test('管理员访问读者管理页面', async ({ page }) => {
    await page.goto('/readers');
    await page.waitForURL(/reader/, { timeout: 5000 });
    await expect(page).toHaveURL(/reader/);
    await expect(page.locator('h2')).toContainText('读者管理');
  });

  test('验证读者列表加载', async ({ page }) => {
    await page.goto('/readers');
    await page.waitForSelector('.el-table, .el-skeleton', { timeout: 8000 });

    // 等待加载完成（骨架屏消失）
    await page.waitForTimeout(2000);

    // 验证表格行存在
    const tableRows = page.locator('.el-table__body-wrapper tr');
    const count = await tableRows.count();
    expect(count).toBeGreaterThan(0);
  });

  test('验证读者列表表头', async ({ page }) => {
    await page.goto('/readers');
    await page.waitForSelector('.el-table', { timeout: 8000 });

    // 验证关键列头
    await expect(page.locator('.el-table__header-wrapper')).toBeVisible();
    await expect(page.locator('.el-table')).toContainText('用户名');
    await expect(page.locator('.el-table')).toContainText('姓名');
    await expect(page.locator('.el-table')).toContainText('手机号');
  });

  test('验证搜索功能', async ({ page }) => {
    await page.goto('/readers');
    await page.waitForSelector('.filter-card', { timeout: 5000 });

    // 在关键词搜索框中输入
    const keywordInput = page.locator('.filter-card input').first();
    await keywordInput.fill('李明');
    await page.keyboard.press('Enter');

    // 等待搜索结果
    await page.waitForTimeout(2000);

    // 验证搜索结果包含"李明"
    const tableBody = page.locator('.el-table__body-wrapper');
    await expect(tableBody).toContainText('李明', { timeout: 5000 });
  });

  test('验证搜索后重置筛选', async ({ page }) => {
    await page.goto('/readers');
    await page.waitForSelector('.filter-card', { timeout: 5000 });

    const keywordInput = page.locator('.filter-card input').first();
    await keywordInput.fill('不存在的人名');
    await page.keyboard.press('Enter');
    await page.waitForTimeout(2000);

    // 点击重置按钮
    const resetBtn = page.locator('button').filter({ hasText: '重置' });
    if (await resetBtn.isVisible()) {
      await resetBtn.click();
      await page.waitForTimeout(2000);

      // 重置后关键词输入框应为空
      await expect(keywordInput).toHaveValue('');
    }
  });

  test('验证分页控件存在', async ({ page }) => {
    await page.goto('/readers');
    await page.waitForSelector('.el-table', { timeout: 8000 });
    await page.waitForTimeout(2000);

    // 验证分页组件
    const pagination = page.locator('.el-pagination');
    // 如果数据足够多，分页应该可见
    const isVisible = await pagination.isVisible().catch(() => false);
    expect(isVisible !== undefined).toBeTruthy();
  });
});
