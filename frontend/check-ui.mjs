import { chromium } from 'playwright';

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage();

// 收集控制台错误
const errors = [];
page.on('console', msg => { if (msg.type() === 'error') errors.push(msg.text()); });
page.on('pageerror', err => errors.push(err.message));

try {
  await page.goto('http://localhost:3000', { timeout: 15000, waitUntil: 'networkidle' });
  await page.waitForTimeout(2000);

  const title = await page.title();
  const bodyText = await page.textContent('body');
  const truncated = bodyText.slice(0, 2000);

  console.log('=== PAGE TITLE ===');
  console.log(title);
  console.log('');
  console.log('=== BODY TEXT (first 2000 chars) ===');
  console.log(truncated);
  console.log('');
  console.log('=== CONSOLE ERRORS ===');
  console.log(errors.length ? errors.join('\n') : 'No errors');

  // 检查是否有登录按钮
  const loginBtn = await page.$('text=登录');
  console.log('');
  console.log('=== ELEMENTS FOUND ===');
  console.log('Login button:', !!loginBtn);

  const inputs = await page.$$('input');
  console.log('Input fields:', inputs.length);

  const buttons = await page.$$('button, .el-button');
  console.log('Buttons:', buttons.length);

} catch (e) {
  console.log('ERROR:', e.message);
} finally {
  await browser.close();
}
