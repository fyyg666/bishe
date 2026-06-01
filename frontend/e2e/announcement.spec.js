// @ts-check
import { test, expect } from '@playwright/test';

test.describe('公告管理 E2E 测试', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.waitForSelector('input', { timeout: 5000 });
    await page.locator('input').first().fill('admin');
    await page.locator('input[type="password"]').first().fill('admin123');
    await page.locator('button').filter({ hasText: /登录|登 录/ }).click();
    await page.waitForURL(/dashboard/, { timeout: 10000 });
  });

  test('访问公告管理页面', async ({ page }) => {
    await page.goto('/announcements');
    await page.waitForURL(/announcement/, { timeout: 5000 });
    await expect(page).toHaveURL(/announcement/);
    await expect(page.locator('h2')).toContainText('公告管理');
  });

  test('验证公告列表加载', async ({ page }) => {
    await page.goto('/announcements');
    await page.waitForSelector('.el-table, .el-skeleton', { timeout: 8000 });

    await page.waitForTimeout(2000);

    // 验证表格行存在
    const tableRows = page.locator('.el-table__body-wrapper tr');
    const count = await tableRows.count();
    expect(count).toBeGreaterThan(0);
  });

  test('验证公告列表表头', async ({ page }) => {
    await page.goto('/announcements');
    await page.waitForSelector('.el-table', { timeout: 8000 });

    await expect(page.locator('.el-table__header-wrapper')).toBeVisible();
    await expect(page.locator('.el-table')).toContainText('标题');
    await expect(page.locator('.el-table')).toContainText('类型');
    await expect(page.locator('.el-table')).toContainText('状态');
  });

  test('点击发布公告按钮打开对话框', async ({ page }) => {
    await page.goto('/announcements');
    await page.waitForSelector('.page-header', { timeout: 5000 });

    // 点击"发布公告"按钮
    await page.locator('button').filter({ hasText: /发布公告|新建公告/ }).click();

    // 验证对话框出现
    await expect(page.locator('.el-dialog')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('.el-dialog')).toContainText('发布公告');
  });

  test('填写并提交新建公告表单', async ({ page }) => {
    await page.goto('/announcements');
    await page.waitForSelector('.page-header', { timeout: 5000 });

    // 打开创建对话框
    await page.locator('button').filter({ hasText: /发布公告|新建公告/ }).click();
    await expect(page.locator('.el-dialog')).toBeVisible({ timeout: 5000 });

    // 填写标题
    const titleInput = page.locator('.el-dialog').locator('input').first();
    await titleInput.fill('E2E测试公告标题');

    // 填写内容
    const contentTextarea = page.locator('.el-dialog').locator('textarea').first();
    await contentTextarea.fill('这是通过E2E测试创建的公告内容。');

    // 选择类型为"通知"
    const typeSelect = page.locator('.el-dialog').locator('.el-select').first();
    await typeSelect.click();
    await page.locator('.el-select-dropdown__item').filter({ hasText: '通知' }).click();
    await page.keyboard.press('Escape');

    // 选择状态为"已发布"
    const statusSelects = page.locator('.el-dialog').locator('.el-select');
    const statusSelect = statusSelects.nth(1);
    await statusSelect.click();
    await page.locator('.el-select-dropdown__item').filter({ hasText: '已发布' }).click();
    await page.keyboard.press('Escape');

    // 点击确定提交
    await page.locator('.el-dialog').locator('button').filter({ hasText: '确定' }).click();

    // 等待提交完成
    await page.waitForTimeout(2000);

    // 验证成功提示
    await expect(page.locator('.el-message--success, .el-notification')).toBeVisible({ timeout: 5000 });
  });

  test('验证搜索功能', async ({ page }) => {
    await page.goto('/announcements');
    await page.waitForSelector('.filter-card', { timeout: 5000 });

    // 在关键词搜索框中输入
    const keywordInput = page.locator('.filter-card input').first();
    await keywordInput.fill('新书上架');
    await page.keyboard.press('Enter');

    await page.waitForTimeout(2000);

    // 验证搜索结果包含关键词
    const tableBody = page.locator('.el-table__body-wrapper');
    await expect(tableBody).toContainText('新书上架', { timeout: 5000 });
  });

  test('取消创建公告', async ({ page }) => {
    await page.goto('/announcements');
    await page.waitForSelector('.page-header', { timeout: 5000 });

    await page.locator('button').filter({ hasText: /发布公告|新建公告/ }).click();
    await expect(page.locator('.el-dialog')).toBeVisible({ timeout: 5000 });

    // 点击取消按钮
    await page.locator('.el-dialog').locator('button').filter({ hasText: '取消' }).click();

    // 对话框应关闭
    await expect(page.locator('.el-dialog')).not.toBeVisible({ timeout: 5000 });
  });
});
