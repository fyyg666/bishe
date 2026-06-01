const BASE = 'http://localhost:8080/api/v1';
const net = require('net');
let token = '';
let errors = [];

function setRedis(key, val) {
  return new Promise(resolve => {
    const c = net.createConnection(6379, '127.0.0.1', () => {
      c.write(`SET ${key} ${val}\r\n`);
      setTimeout(() => { c.end(); resolve(); }, 200);
    });
  });
}

async function api(method, path, data) {
  const opts = { method, headers: { 'Content-Type': 'application/json' } };
  if (token) opts.headers['Authorization'] = `Bearer ${token}`;
  if (data) opts.body = JSON.stringify(data);
  const r = await fetch(`${BASE}${path}`, opts);
  const json = await r.json();
  return { status: r.status, ...json };
}

function ok(label, condition) {
  console.log(condition ? `  ✅ ${label}` : `  ❌ ${label}`);
  if (!condition) errors.push(label);
}
function info(label, val) {
  const s = val != null ? String(val) : 'null';
  console.log(`  📊 ${label}: ${s.substring(0, 120)}`);
}

async function login() {
  const capR = await api('GET', '/captcha');
  const key = capR.data?.captchaKey;
  if (!key) { errors.push('无法获取验证码'); return false; }
  await setRedis('captcha:' + key, 'test');

  const r = await api('POST', '/auth/login', {
    username: 'admin', password: 'admin123',
    captchaKey: key, captchaCode: 'test'
  });
  if (r.code !== 0 || !r.data?.accessToken) {
    errors.push('管理员登录失败: ' + r.message);
    return false;
  }
  token = r.data.accessToken;
  return true;
}

async function run() {
  console.log('═══════════════════════════════════════');
  console.log('🔍 综合 API 操作测试 (v2)');
  console.log('═══════════════════════════════════════\n');

  if (!await login()) { console.log('登录失败，终止测试'); return; }
  console.log('── 1. 认证模块 ──');
  ok('管理员登录成功', true);

  // ── 2. 图书 ──
  console.log('\n── 2. 图书模块 ──');
  let r = await api('GET', '/books?current=1&size=5');
  ok('图书列表', r.code === 0 && r.data?.records);
  info('总数', r.data?.total + '/' + r.data?.records?.length);

  r = await api('GET', '/books/hot'); ok('热门图书', r.code === 0);
  r = await api('GET', '/books/new'); ok('新书推荐', r.code === 0);
  r = await api('GET', '/books/facets/categories'); ok('分类聚合', r.code === 0);

  // CRUD
  const testBook = { title: 'AutoTest_' + Date.now(), author: 'test', isbn: '978-0-00-' + String(Date.now()).slice(-7, -1) + '-0', publisher: 'test', categoryId: 1, totalCount: 3 };
  r = await api('POST', '/books', testBook);
  const bid = r.data?.id;
  ok('新增图书', r.code === 0 && !!bid);
  if (bid) {
    r = await api('PUT', `/books/${bid}`, { ...testBook, title: 'AutoTest_modified' });
    ok('更新图书', r.code === 0);
    r = await api('DELETE', `/books/${bid}`);
    ok('删除图书', r.code === 0);
  }

  // ── 3. 读者 ──
  console.log('\n── 3. 读者模块 ──');
  r = await api('GET', '/readers?size=5');
  ok('读者列表', r.code === 0);
  info('总数', r.data?.total);

  const testReader = { username: 'test_' + Date.now(), realName: 'test', email: 't@t.com', phone: '13900000000', password: 'Test1234!' };
  r = await api('POST', '/readers', testReader);
  const rid = r.data?.id;
  ok('新增读者', r.code === 0 && !!rid);
  if (rid) { r = await api('DELETE', `/readers/${rid}`); ok('删除读者', r.code === 0); }

  // ── 4. 借阅 ──
  console.log('\n── 4. 借阅模块 ──');
  r = await api('GET', '/borrows?size=5');
  ok('借阅列表', r.code === 0);
  r = await api('GET', '/borrow-rules');
  ok('借阅规则', r.code === 0);

  // ── 5. 座位 ──
  console.log('\n── 5. 座位模块 ──');
  r = await api('GET', '/seats');
  ok('座位列表', r.code === 0);
  const seatData = r.data;
  const seatTotal = Array.isArray(seatData) ? seatData.length : (seatData?.total || 0);
  info('seat表总数', seatTotal);

  r = await api('GET', '/seats/reading-rooms');
  let roomTotal = 0;
  if (r.code === 0 && Array.isArray(r.data)) {
    roomTotal = r.data.reduce((s, rm) => s + (rm.totalSeats || 0), 0);
    info('阅览室合计', roomTotal);
  }
  if (seatTotal !== roomTotal) {
    errors.push(`数据不一致：seat表=${seatTotal}, reading_room合计=${roomTotal}`);
    console.log(`  ⚠️  座位数不一致！`);
  }

  // ── 6. 统计 ──
  console.log('\n── 6. 统计交叉校验 ──');
  r = await api('GET', '/statistics/overview');
  ok('综合概览', r.code === 0);
  const ovSeats = r.data?.seatStatistics?.totalSeats || 0;
  const ovBooks = r.data?.bookStatistics?.totalBooks || 0;
  const ovBorrows = r.data?.borrowStatistics?.totalBorrows || 0;
  info('统计-座位', ovSeats);
  info('统计-图书', ovBooks);
  info('统计-借阅', ovBorrows);

  r = await api('GET', '/books');
  const bookCount = r.data?.total || 0;
  if (ovSeats !== seatTotal) errors.push(`统计座椅${ovSeats}≠seat表${seatTotal}`);
  if (ovBooks !== bookCount && bookCount > 0) errors.push(`统计图书${ovBooks}≠实际${bookCount}`);

  r = await api('GET', '/statistics/borrow-trend'); ok('借阅趋势', r.code === 0);
  r = await api('GET', '/statistics/hot-books'); ok('热门图书榜', r.code === 0);

  // ── 7. 公告/通知 ──
  console.log('\n── 7. 公告&通知 ──');
  r = await api('GET', '/announcements'); ok('公告', r.code === 0);
  r = await api('GET', '/notifications'); ok('通知', r.code === 0);
  r = await api('GET', '/notifications/unread-count'); ok('未读计数', r.code === 0);

  // ── 8. 快速检查 ──
  console.log('\n── 8. 其他模块快速检查 ──');
  for (const [label, path] of [
    ['志愿服务', '/volunteers'], ['赔偿', '/compensations'], ['预算', '/budget-funds'],
    ['荐购', '/purchase-suggestions'], ['供应商', '/vendors'], ['采购', '/purchase-orders'],
    ['数字资源', '/digital-resources'], ['期刊', '/serial/subscriptions'],
    ['分馆', '/branches'], ['报表', '/reports/templates'], ['积分', '/credits'],
    ['积分日志', '/credits/logs'], ['统一搜索', '/search?keyword=test']
  ]) {
    r = await api('GET', path);
    ok(label, r.code === 0);
  }

  // ── 汇总 ──
  console.log('\n═══════════════════════════════════════');
  console.log(`测试完成。检查项: ${errors.length > 0 ? errors.length + '个问题' : '全部通过 ✅'}`);
  for (const e of errors) console.log(`  ⚠️  ${e}`);
  console.log('\n已知问题 (需后端修复):');
  console.log('  - /purchase-orders 返回500内部错误 (PurchaseOrderServiceImpl)');
  console.log('  - V2.27.0__seed_more_seats.sql 迁移需重启后端才能生效');
  console.log('  - /seats/reading-rooms 端点需重启后端才能生效');
}

run().catch(e => { console.error('FATAL:', e.message); errors.push(e.message); });
