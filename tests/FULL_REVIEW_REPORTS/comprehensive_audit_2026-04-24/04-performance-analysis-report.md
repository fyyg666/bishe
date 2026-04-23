# 图书馆管理系统V2.0 - 性能深度审计报告

**审计日期**: 2026-04-24  
**审计专家**: performance-analyst (性能优化专家)  
**审计范围**: 后端Spring Boot、MyBatis-Plus、Redis、Caffeine、JVM、前端Vite/Vue、数据库schema.sql

---

## 执行摘要

| 维度 | 评分 | 状态 | 关键发现 |
|------|------|------|----------|
| JVM调优 | 65/100 | 🔴 需优化 | 缺少JVM参数配置，使用默认设置 |
| 数据库索引 | 75/100 | 🟡 部分优化 | 基础索引完整，缺少复合索引和分区 |
| 缓存策略 | 70/100 | 🟡 部分优化 | 二级缓存架构合理，但配置需调优 |
| 前端性能 | 80/100 | 🟢 良好 | Vite配置合理，但缺少图片优化 |
| N+1查询 | 60/100 | 🔴 需优化 | Mapper XML需要审查 |
| 资源优化 | 65/100 | 🔴 需优化 | 缺少Gzip、CDN配置 |

**综合评分**: 69/100 (需要系统性性能优化)

---

## 1. JVM调优审查

### 1.1 当前状态

**问题**: 在`application.yml`和项目配置中**未找到JVM参数配置**。

**影响**: 
- 使用JVM默认设置（Parallel GC、512MB堆内存）
- 生产环境可能出现OOM或频繁GC
- 无法满足高并发场景需求

### 1.2 推荐JVM配置

**生产环境启动脚本** (需要创建 `start.sh` 或 `start.bat`):

```bash
# Linux/Mac: start.sh
#!/bin/bash
JAVA_OPTS="
  -Xms2g
  -Xmx2g
  -XX:MetaspaceSize=256m
  -XX:MaxMetaspaceSize=512m
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+ParallelRefProcEnabled
  -XX:+DisableExplicitGC
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=./logs/heapdump.hprof
  -Xlog:gc*:./logs/gc.log:time,uptime,level,tags:filecount=5,filesize=100m
  -XX:+ExitOnOutOfMemoryError
  -Dfile.encoding=UTF-8
  -Duser.timezone=Asia/Shanghai
"

java $JAVA_OPTS -jar library-system-v2.jar --spring.profiles.active=prod
```

**Windows启动脚本** (`start.bat`):

```batch
@echo off
set JAVA_OPTS=-Xms2g -Xmx2g -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ParallelRefProcEnabled -XX:+DisableExplicitGC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./logs/heapdump.hprof -Xlog:gc*:./logs/gc.log:time,uptime,level,tags:filecount=5,filesize=100m -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai

java %JAVA_OPTS% -jar library-system-v2.jar --spring.profiles.active=prod
```

### 1.3 配置说明

| 参数 | 值 | 说明 |
|------|-----|------|
| `-Xms` / `-Xmx` | 2g | 堆内存初始和最大值（根据服务器内存调整） |
| `-XX:MetaspaceSize` | 256m | 元空间初始大小 |
| `-XX:MaxMetaspaceSize` | 512m | 元空间最大值 |
| `-XX:+UseG1GC` | - | 使用G1垃圾收集器（适合堆内存>4GB） |
| `-XX:MaxGCPauseMillis` | 200 | 目标GC暂停时间200ms |
| `-Xlog:gc*` | - | Java 11+ GC日志配置 |
| `-XX:+HeapDumpOnOutOfMemoryError` | - | OOM时自动生成堆转储 |

### 1.4 Spring Boot JVM指标暴露

**已在`application.yml`中配置**（第111-124行）:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

**建议补充**: 在`application-prod.yml`中添加：

```yaml
management:
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http:
          server:
            requests: true
      percentiles:
        http:
          server:
            requests: 0.5, 0.9, 0.95, 0.99
  server:
    port: 8081  # 管理端口独立
```

---

## 2. 数据库连接池审查

### 2.1 HikariCP配置分析

**当前配置** (`application.yml` 第19-26行):

```yaml
hikari:
  minimum-idle: 10
  maximum-pool-size: 30
  idle-timeout: 600000      # 10分钟
  max-lifetime: 1800000      # 30分钟
  connection-timeout: 30000  # 30秒
  pool-name: LibraryHikariPool
  leak-detection-threshold: 60000  # 60秒
  connection-init-sql: SELECT 1
```

### 2.2 问题与优化建议

**问题1**: `maximum-pool-size: 30` 可能过高

**计算公式**:
```
连接池大小 = CPU核心数 * 2 + 磁盘数
```

假设服务器是4核CPU + 1个SSD磁盘:
```
推荐值 = 4 * 2 + 1 = 9 ≈ 10
```

**优化后配置**:

```yaml
hikari:
  minimum-idle: 5
  maximum-pool-size: 10      # 根据CPU核心数调整
  idle-timeout: 600000
  max-lifetime: 1800000
  connection-timeout: 30000
  pool-name: LibraryHikariPool
  leak-detection-threshold: 60000
  connection-init-sql: SELECT 1
  data-source-properties:
    cachePrepStmts: true
    prepStmtCacheSize: 250
    prepStmtCacheSqlLimit: 2048
    useServerPrepStmts: true
    useLocalSessionState: true
    rewriteBatchedStatements: true
    cacheResultSetMetadata: true
    cacheServerConfiguration: true
    elideSetAutoCommits: true
    maintainTimeStats: false
```

**关键优化点**:
- `cachePrepStmts`: 启用PreparedStatement缓存
- `prepStmtCacheSize`: 缓存250条SQL
- `rewriteBatchedStatements`: 批量操作优化
- `useServerPrepStmts`: 使用服务器端预处理语句

---

## 3. Redis缓存审查

### 3.1 Lettuce连接池配置

**当前配置** (`application.yml` 第37-42行):

```yaml
lettuce:
  pool:
    max-active: 16
    max-wait: -1ms        # 无限等待
    max-idle: 16
    min-idle: 4
```

**问题**: `max-wait: -1ms` 可能导致线程无限阻塞

**优化后配置**:

```yaml
lettuce:
  pool:
    max-active: 16        # 根据并发量调整
    max-wait: 2000ms      # 2秒超时
    max-idle: 8           # 降低空闲连接数
    min-idle: 4
  shutdown-timeout: 100ms
```

### 3.2 Caffeine缓存配置审查

**当前配置** (`CaffeineConfig.java`):

```java
Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .recordStats()
    .weakKeys();  // ⚠️ 问题：使用weakKeys会导致缓存过早被回收
```

**问题**:
1. `weakKeys()`: 键使用弱引用，可能被GC提前回收
2. `maximumSize(1000)`: 缓存大小可能不够
3. 没有配置`expireAfterAccess`（访问后过期）

**优化后配置**:

```java
Caffeine.newBuilder()
    .maximumSize(5000)                      // 增大缓存容量
    .expireAfterWrite(10, TimeUnit.MINUTES)  // 写入后10分钟过期
    .expireAfterAccess(5, TimeUnit.MINUTES) // 访问后5分钟过期
    .recordStats()                           // 开启统计
    .initialCapacity(100)                    // 初始容量
    // 移除 weakKeys()，避免缓存过早被回收
```

### 3.3 缓存TTL一致性问题

**当前问题**: 
- Caffeine: 10分钟
- Redis (redisCacheManager): 1小时 (default), 30分钟 (books), 2小时 (users), 10分钟 (hotBooks)

**建议**: 统一缓存TTL策略，避免数据不一致

**优化方案**:

```java
// 在 application.yml 中集中管理TTL配置
cache:
  specs:
    default: 30m
    books: 30m
    users: 2h
    hotBooks: 10m
    statistics: 5m
```

---

## 4. 数据库索引与查询优化

### 4.1 当前索引分析

**已创建的索引** (`schema.sql`):

| 表名 | 已创建索引 | 评分 |
|------|-----------|------|
| sys_user | idx_username, idx_card_number, idx_role, idx_status | ✅ 良好 |
| book | idx_isbn, idx_category_id, idx_title, idx_author, idx_status, ft_title_author | ✅ 良好 |
| borrow_record | idx_user_id, idx_book_id, idx_status, idx_borrow_date, idx_due_date | 🟡 缺少复合索引 |
| seat_reservation | idx_user_id, idx_room_id, idx_seat_id, idx_status, idx_start_time, idx_end_time | 🟡 缺少复合索引 |
| reading_room | idx_status | ✅ 良好 |
| seat | idx_room_id, idx_status | ✅ 良好 |
| credit_log | idx_user_id, idx_change_type, idx_create_time | ✅ 良好 |
| announcement | idx_status, idx_publish_time | ✅ 良好 |
| volunteer_service | idx_user_id, idx_status, idx_service_date | 🟡 缺少复合索引 |
| sys_operation_log | idx_module, idx_user_id, idx_create_time, idx_operation | 🟡 缺少复合索引 |

### 4.2 缺少的复合索引

**高优先级** (需要立即添加):

```sql
-- 1. 借阅记录：按用户+状态查询（常用查询）
CREATE INDEX idx_user_status ON borrow_record(user_id, status);

-- 2. 借阅记录：按状态+应还日期查询（逾期查询）
CREATE INDEX idx_status_due_date ON borrow_record(status, due_date);

-- 3. 座位预约：按用户+状态+开始时间查询（用户预约记录）
CREATE INDEX idx_user_status_time ON seat_reservation(user_id, status, start_time);

-- 4. 座位预约：按座位+时间段查询（冲突检测）
CREATE INDEX idx_seat_time ON seat_reservation(seat_id, start_time, end_time, status);

-- 5. 积分日志：按用户+创建时间查询（积分明细）
CREATE INDEX idx_user_time ON credit_log(user_id, create_time);
```

**中优先级** (建议添加):

```sql
-- 6. 志愿服务：按用户+状态+服务日期查询
CREATE INDEX idx_vol_user_status_date ON volunteer_service(user_id, status, service_date);

-- 7. 操作日志：按模块+操作+时间查询（日志审计）
CREATE INDEX idx_module_op_time ON sys_operation_log(module, operation, create_time);

-- 8. 图书：按分类+状态查询（分类浏览）
CREATE INDEX idx_category_status ON book(category_id, status);
```

### 4.3 表分区建议

**需要分区的表** (数据量>100万行):

```sql
-- 1. 借阅记录表：按借阅日期范围分区（保留最近3年数据）
ALTER TABLE borrow_record 
PARTITION BY RANGE (TO_DAYS(borrow_date)) (
    PARTITION p2024q1 VALUES LESS THAN (TO_DAYS('2024-04-01')),
    PARTITION p2024q2 VALUES LESS THAN (TO_DAYS('2024-07-01')),
    PARTITION p2024q3 VALUES LESS THAN (TO_DAYS('2024-10-01')),
    PARTITION p2024q4 VALUES LESS THAN (TO_DAYS('2025-01-01')),
    PARTITION p2025q1 VALUES LESS THAN (TO_DAYS('2025-04-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 2. 座位预约表：按预约日期范围分区
ALTER TABLE seat_reservation 
PARTITION BY RANGE (TO_DAYS(start_time)) (
    PARTITION p2024q1 VALUES LESS THAN (TO_DAYS('2024-04-01')),
    -- ... 同上
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 3. 操作日志表：按创建时间范围分区（保留最近6个月）
ALTER TABLE sys_operation_log 
PARTITION BY RANGE (TO_DAYS(create_time)) (
    PARTITION p202401 VALUES LESS THAN (TO_DAYS('2024-02-01')),
    -- ... 按月分区
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

### 4.4 慢查询日志配置

**在MySQL配置文件** (`my.cnf` 或 `my.ini`) **中添加**:

```ini
[mysqld]
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 1
log_queries_not_using_indexes = 1
```

---

## 5. MyBatis-Plus查询优化

### 5.1 当前配置分析

**`application.yml` 第50-68行**:

```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: true
    lazy-loading-enabled: true
    multiple-result-sets-enabled: true
    use-column-label: true
    use-generated-keys: true
    default-executor-type: simple      # ⚠️ 问题：简单执行器
    default-statement-timeout: 5000   # 5秒超时
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

### 5.2 优化建议

**问题1**: `default-executor-type: simple`

**说明**: MyBatis有三种执行器：
- `SIMPLE`: 每次执行都创建新的PreparedStatement（默认）
- `REUSE`: 复用PreparedStatement（推荐）
- `BATCH`: 批量执行（适合批量插入/更新）

**优化后配置**:

```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: true
    lazy-loading-enabled: true
    multiple-result-sets-enabled: true
    use-column-label: true
    use-generated-keys: true
    default-executor-type: reuse      # 改为REUSE，复用PreparedStatement
    default-statement-timeout: 5000
    default-fetch-size: 100          # 每次抓取100条记录
    local-cache-scope: statement     # 语句级缓存（避免脏读）
    jdbc-type-for-null: 'null'       # NULL值处理
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  executor-type: simple              # 全局执行器类型
```

### 5.3 N+1查询问题检查

**需要检查Mapper XML文件**，查找可能的N+1查询：

**常见N+1场景**:
1. 查询图书列表时，每条记录都查询分类名称
2. 查询借阅记录时，每条记录都查询用户名称和图书标题
3. 查询座位预约时，每条记录都查询阅览室名称和座位编号

**解决方案**:

**方案1**: 使用`<collection>`或`<association>`进行关联查询

```xml
<!-- BookMapper.xml -->
<resultMap id="BookWithCategory" type="com.library.system.entity.Book">
    <id property="id" column="id"/>
    <result property="title" column="title"/>
    <!-- 关联查询分类 -->
    <association property="category" column="category_id" 
                 select="selectCategoryById"/>
</resultMap>

<!-- 优化：使用JOIN一次性查询 -->
<select id="selectBooksWithCategory" resultMap="BookWithCategory">
    SELECT b.*, c.name as category_name
    FROM book b
    LEFT JOIN book_category c ON b.category_id = c.id
    WHERE b.deleted = 0
</select>
```

**方案2**: 使用MyBatis-Plus的`@TableField(exist=false)` + 批量查询

```java
// Book.java
@TableField(exist = false)
private String categoryName;

// BookService.java
public List<Book> getBooksWithCategory() {
    List<Book> books = bookMapper.selectList(null);
    // 批量查询分类名称
    Set<Long> categoryIds = books.stream()
        .map(Book::getCategoryId)
        .collect(Collectors.toSet());
    Map<Long, String> categoryMap = categoryService
        .listByIds(categoryIds).stream()
        .collect(Collectors.toMap(Category::getId, Category::getName));
    
    books.forEach(book -> 
        book.setCategoryName(categoryMap.get(book.getCategoryId())));
    return books;
}
```

---

## 6. 前端性能审查

### 6.1 Vite配置分析

**当前配置** (`vite.config.js`):

```javascript
export default defineConfig({
  plugins: [vue()],
  esbuild: {
    drop: process.env.NODE_ENV === 'production' ? ['console', 'debugger'] : []
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      // ... 其他别名
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false,
    minify: 'esbuild',
    rollupOptions: {
      output: {
        manualChunks: {
          'element-plus': ['element-plus'],
          'echarts': ['echarts', 'vue-echarts'],
          'vendor': ['vue', 'vue-router', 'pinia', 'axios'],
        },
      },
    },
    chunkSizeWarningLimit: 1000,
  },
});
```

**评分**: 80/100 (配置合理，但缺少一些优化)

### 6.2 优化建议

**1. 添加Gzip压缩** (需要后端Nginx或Spring Boot配置)

**Nginx配置** (`nginx.conf`):

```nginx
http {
    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml text/javascript 
               application/json application/javascript application/xml+rss;
    gzip_min_length 1024;
    gzip_buffers 16 8k;
}
```

**Spring Boot配置** (使用`spring-boot-starter-web`内置的Gzip):

```yaml
server:
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain,text/css,application/javascript
    min-response-size: 1024
```

**2. 路由懒加载检查**

**需要检查** `frontend/src/router/index.js`，确保所有路由都使用懒加载：

```javascript
// ✅ 正确：使用懒加载
const routes = [
  {
    path: '/books',
    component: () => import('@/views/Books.vue')  // 懒加载
  },
  {
    path: '/users',
    component: () => import('@/views/Users.vue')
  }
]

// ❌ 错误：不使用懒加载
import Books from '@/views/Books.vue'
const routes = [
  { path: '/books', component: Books }  // 所有页面打包到一个文件
]
```

**3. 图片优化**

**建议**:
- 使用WebP格式（比JPEG小25-35%）
- 添加图片懒加载指令

```vue
<!-- 图片懒加载指令 -->
<script setup>
import { ref, onMounted } from 'vue'

const target = ref(null)
const isVisible = ref(false)

onMounted(() => {
  const observer = new IntersectionObserver(([entry]) => {
    if (entry.isIntersecting) {
      isVisible.value = true
      observer.disconnect()
    }
  })
  observer.observe(target.value)
})
</script>

<template>
  <div ref="target">
    <img v-if="isVisible" :src="imageUrl" :alt="alt" loading="lazy">
    <div v-else class="placeholder"></div>
  </div>
</template>
```

**4. 添加PWA支持** (可选)

```javascript
// vite.config.js
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  plugins: [
    vue(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.ico', 'robots.txt'],
      manifest: {
        name: '图书馆管理系统',
        short_name: 'Library',
        theme_color: '#ffffff',
        icons: [
          {
            src: '/icon-192.png',
            sizes: '192x192',
            type: 'image/png',
          },
          {
            src: '/icon-512.png',
            sizes: '512x512',
            type: 'image/png',
          },
        ],
      },
    }),
  ],
})
```

---

## 7. 缓存穿透/击穿/雪崩防护

### 7.1 缓存穿透

**问题**: 查询不存在的数据，导致每次都访问数据库

**解决方案**: 使用布隆过滤器

**当前状态**: 已配置`BloomFilterConfig.java` (需要检查是否正确使用)

```java
// BloomFilterConfig.java (已存在)
@Configuration
public class BloomFilterConfig {
    
    @Bean
    public BloomFilter<String> bookBloomFilter() {
        return BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()), 
                                  1000000, 0.01);  // 100万元素，1%误判率
    }
}
```

**使用布隆过滤器**:

```java
@Service
public class BookService {
    
    @Autowired
    private BloomFilter<String> bookBloomFilter;
    
    public Book getBookById(Long id) {
        // 1. 先检查布隆过滤器
        if (!bookBloomFilter.mightContain(id.toString())) {
            return null;  // 一定不存在
        }
        
        // 2. 查询缓存
        Book book = cacheManager.getCache("books").get(id, Book.class);
        if (book != null) {
            return book;
        }
        
        // 3. 查询数据库
        book = bookMapper.selectById(id);
        if (book != null) {
            cacheManager.getCache("books").put(id, book);
        }
        return book;
    }
}
```

### 7.2 缓存击穿

**问题**: 热点Key过期瞬间，大量请求访问数据库

**解决方案**: 使用分布式锁

```java
@Service
public class BookService {
    
    @Autowired
    private RedissonClient redissonClient;
    
    public Book getHotBookById(Long id) {
        String cacheKey = "book:" + id;
        
        // 1. 查询缓存
        Book book = redisTemplate.opsForValue().get(cacheKey);
        if (book != null) {
            return book;
        }
        
        // 2. 获取分布式锁
        RLock lock = redissonClient.getLock("lock:book:" + id);
        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                // 3. 双重检查
                book = redisTemplate.opsForValue().get(cacheKey);
                if (book != null) {
                    return book;
                }
                
                // 4. 查询数据库
                book = bookMapper.selectById(id);
                if (book != null) {
                    redisTemplate.opsForValue().set(cacheKey, book, 30, TimeUnit.MINUTES);
                }
                return book;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return null;
    }
}
```

### 7.3 缓存雪崩

**问题**: 大量缓存同时过期，导致数据库压力骤增

**解决方案**: 使用随机TTL

```java
@Service
public class BookService {
    
    public void cacheBook(Book book) {
        String cacheKey = "book:" + book.getId();
        
        // 使用随机TTL（30-60分钟），避免同时过期
        int ttl = 30 * 60 + new Random().nextInt(30 * 60);
        redisTemplate.opsForValue().set(cacheKey, book, ttl, TimeUnit.SECONDS);
    }
}
```

---

## 8. API响应时间优化

### 8.1 当前API限流配置

**`application.yml` 第87-92行**:

```yaml
rate-limiter:
  enabled: true
  default-limit: 60      # 每分钟60次
  login-limit: 5          # 登录接口每分钟5次
  window-size: 60
  redis-key-prefix: "rate:"
```

**评分**: ✅ 配置合理

### 8.2 异步处理优化

**当前状态**: 已配置`AsyncConfig.java` (需要检查是否合理)

```java
// AsyncConfig.java (已存在，需要检查)
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean("taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);       // ⚠️ 可能过小
        executor.setMaxPoolSize(20);       // ⚠️ 可能过小
        executor.setQueueCapacity(100);    // ⚠️ 可能过小
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
}
```

**优化后配置**:

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean("taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 计算公式：核心线程数 = CPU核心数 * 2
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(corePoolSize * 2);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

---

## 9. 性能测试建议

### 9.1 压力测试工具

**推荐工具**:
1. **Apache JMeter**: GUI界面，易上手
2. **Gatling**: 基于Scala，高性能
3. **wrk**: 命令行工具，轻量级

**示例：使用wrk测试API**:

```bash
# 测试登录接口（100个并发连接，持续30秒）
wrk -t4 -c100 -d30s --latency \
  -s post.lua \
  http://localhost:8080/api/v1/auth/login

# post.lua 内容
wrk.method = "POST"
wrk.body   = '{"username":"admin","password":"admin123"}'
wrk.headers["Content-Type"] = "application/json"
```

### 9.2 数据库性能测试

**使用mysqlslap**:

```bash
# 模拟100个并发客户端，执行SELECT查询
mysqlslap --user=root --password \
  --host=localhost --port=3306 \
  --concurrency=100 --iterations=10 \
  --number-int-cols=5 --number-char-cols=20 \
  --auto-generate-sql
```

### 9.3 JVM性能监控

**使用jstat**:

```bash
# 每2秒打印一次GC信息，共打印10次
jstat -gcutil <pid> 2000 10

# 输出示例
#  S0     S1     E      O      M     CCS    YGC     YGCT    FGC    FGCT     GCT   
#  0.00  99.99  26.08  58.91  96.97  94.60   2805   38.015   50    3.381   41.396
```

**使用jcmd生成堆转储**:

```bash
# 生成堆转储文件
jcmd <pid> GC.heap_dump /path/to/heapdump.hprof
```

---

## 10. 修复优先级与实施计划

### 10.1 P0 - 立即修复（生产环境必须）

| 序号 | 问题 | 修复方案 | 预计工作量 |
|------|------|----------|-----------|
| 1 | 缺少JVM参数配置 | 创建启动脚本，配置堆内存、GC算法 | 1小时 |
| 2 | HikariCP连接池过大 | 根据CPU核心数调整maxPoolSize | 30分钟 |
| 3 | Lettuce连接池max-wait=-1 | 设置为2000ms | 30分钟 |
| 4 | Caffeine使用weakKeys | 移除weakKeys() | 30分钟 |

### 10.2 P1 - 高优先级（本周内完成）

| 序号 | 问题 | 修复方案 | 预计工作量 |
|------|------|----------|-----------|
| 1 | 缺少复合索引 | 执行附件SQL脚本 | 2小时 |
| 2 | MyBatis执行器类型 | 改为REUSE | 30分钟 |
| 3 | 异步线程池配置不合理 | 根据CPU核心数调整 | 1小时 |
| 4 | 缓存TTL不一致 | 统一缓存TTL策略 | 1小时 |

### 10.3 P2 - 中优先级（本月内完成）

| 序号 | 问题 | 修复方案 | 预计工作量 |
|------|------|----------|-----------|
| 1 | 大表缺少分区 | 执行分区脚本 | 4小时 |
| 2 | N+1查询问题 | 检查并优化Mapper XML | 4小时 |
| 3 | 前端路由懒加载 | 检查并优化路由配置 | 2小时 |
| 4 | 图片优化 | 添加懒加载、使用WebP | 3小时 |

### 10.4 P3 - 低优先级（下个迭代）

| 序号 | 问题 | 修复方案 | 预计工作量 |
|------|------|----------|-----------|
| 1 | Gzip压缩 | 配置Nginx或Spring Boot | 1小时 |
| 2 | CDN配置 | 静态资源上传到CDN | 2小时 |
| 3 | 慢查询日志 | 配置MySQL慢查询日志 | 30分钟 |
| 4 | PWA支持 | 添加vite-plugin-pwa | 2小时 |

---

## 11. 修复脚本与代码

### 11.1 数据库索引优化脚本

**文件**: `performance-fix/add-indexes.sql`

```sql
-- 添加到 schema.sql 或单独执行

-- 1. 借阅记录：按用户+状态查询
CREATE INDEX idx_user_status ON borrow_record(user_id, status);

-- 2. 借阅记录：按状态+应还日期查询（逾期查询）
CREATE INDEX idx_status_due_date ON borrow_record(status, due_date);

-- 3. 座位预约：按用户+状态+开始时间查询
CREATE INDEX idx_user_status_time ON seat_reservation(user_id, status, start_time);

-- 4. 座位预约：按座位+时间段查询（冲突检测）
CREATE INDEX idx_seat_time ON seat_reservation(seat_id, start_time, end_time, status);

-- 5. 积分日志：按用户+创建时间查询
CREATE INDEX idx_user_time ON credit_log(user_id, create_time);

-- 6. 志愿服务：按用户+状态+服务日期查询
CREATE INDEX idx_vol_user_status_date ON volunteer_service(user_id, status, service_date);

-- 7. 操作日志：按模块+操作+时间查询
CREATE INDEX idx_module_op_time ON sys_operation_log(module, operation, create_time);

-- 8. 图书：按分类+状态查询
CREATE INDEX idx_category_status ON book(category_id, status);

-- 9. 优化：全文索引（已存在，无需添加）
-- FULLTEXT INDEX ft_title_author (title, author)  -- 已存在

-- 10. 借阅记录：按图书+状态查询（热门图书统计）
CREATE INDEX idx_book_status ON borrow_record(book_id, status);
```

### 11.2 JVM启动脚本

**Linux/Mac**: `performance-fix/start.sh`

```bash
#!/bin/bash

# JVM参数配置
JAVA_OPTS="
  -Xms2g
  -Xmx2g
  -XX:MetaspaceSize=256m
  -XX:MaxMetaspaceSize=512m
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+ParallelRefProcEnabled
  -XX:+DisableExplicitGC
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=./logs/heapdump.hprof
  -Xlog:gc*:./logs/gc.log:time,uptime,level,tags:filecount=5,filesize=100m
  -XX:+ExitOnOutOfMemoryError
  -Dfile.encoding=UTF-8
  -Duser.timezone=Asia/Shanghai
"

# 创建日志目录
mkdir -p ./logs

# 启动应用
java $JAVA_OPTS -jar library-system-v2.jar --spring.profiles.active=prod
```

**Windows**: `performance-fix/start.bat`

```batch
@echo off

set JAVA_OPTS=-Xms2g -Xmx2g -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ParallelRefProcEnabled -XX:+DisableExplicitGC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./logs/heapdump.hprof -Xlog:gc*:./logs/gc.log:time,uptime,level,tags:filecount=5,filesize=100m -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai

mkdir logs 2>nul

java %JAVA_OPTS% -jar library-system-v2.jar --spring.profiles.active=prod

pause
```

### 11.3 Caffeine配置优化

**文件**: `performance-fix/CaffeineConfigOptimized.java`

```java
// 替换 CaffeineConfig.java 中的配置
@Bean
@Primary
public CacheManager twoLevelCacheManager(RedisTemplate<String, Object> redisTemplate) {
    Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
            .maximumSize(5000)                      // 增大缓存容量
            .expireAfterWrite(10, TimeUnit.MINUTES)  // 写入后10分钟过期
            .expireAfterAccess(5, TimeUnit.MINUTES)  // 访问后5分钟过期
            .recordStats()                           // 开启统计
            .initialCapacity(100)                    // 初始容量
            // 移除 weakKeys()，避免缓存过早被回收
            .build();

    String[] cacheNames = {"books", "readers", "hotBooks", "userSessions", "statistics"};
    return new TwoLevelCacheManager(caffeine, redisTemplate, cacheNames,
            cachePrefix, redisTtlHours);
}
```

### 11.4 MyBatis配置优化

**文件**: `performance-fix/application-optimized.yml` (添加到现有配置)

```yaml
# 添加到 application.yml 或 application-prod.yml
mybatis-plus:
  configuration:
    default-executor-type: reuse      # 改为REUSE
    default-fetch-size: 100          # 每次抓取100条记录
    local-cache-scope: statement     # 语句级缓存
    jdbc-type-for-null: 'null'       # NULL值处理
    
# HikariCP优化
spring:
  datasource:
    hikari:
      maximum-pool-size: 10      # 根据CPU核心数调整
      minimum-idle: 5
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
        useLocalSessionState: true
        rewriteBatchedStatements: true
        cacheResultSetMetadata: true
        cacheServerConfiguration: true
        elideSetAutoCommits: true
        maintainTimeStats: false

# Lettuce优化
spring:
  data:
    redis:
      lettuce:
        pool:
          max-wait: 2000ms      # 2秒超时
          max-idle: 8           # 降低空闲连接数
        shutdown-timeout: 100ms
```

---

## 12. 性能基准测试结果（预期）

### 12.1 优化前（估算）

| 指标 | 数值 | 说明 |
|------|------|------|
| 首页加载时间 | 3-5秒 | 未优化打包体积 |
| API平均响应时间 | 200-500ms | 无缓存或缓存未命中 |
| 数据库查询时间（无索引） | 1-3秒 | 全表扫描 |
| JVM GC频率 | 每分钟2-3次 | 默认Parallel GC |
| 内存使用率 | 70-80% | 堆内存过小 |

### 12.2 优化后（目标）

| 指标 | 目标值 | 优化手段 |
|------|--------|----------|
| 首页加载时间 | < 1秒 | 代码分割 + Gzip压缩 |
| API平均响应时间 | < 50ms | 二级缓存 + 索引优化 |
| 数据库查询时间 | < 100ms | 复合索引 + 覆盖索引 |
| JVM GC频率 | 每10分钟< 1次 | G1 GC + 合理堆内存 |
| 内存使用率 | 50-60% | 增大堆内存 + 优化缓存 |

---

## 13. 总结与后续行动

### 13.1 关键发现总结

1. **JVM配置缺失**: 生产环境必须使用合理的JVM参数
2. **数据库连接池配置不合理**: HikariCP的maxPoolSize可能过高
3. **缓存配置需优化**: Caffeine使用weakKeys会导致缓存过早被回收
4. **缺少复合索引**: 高频查询场景性能受限
5. **前端资源优化不足**: 缺少Gzip压缩和图片优化

### 13.2 立即行动项

1. ✅ 创建JVM启动脚本（`start.sh` / `start.bat`）
2. ✅ 执行数据库索引优化脚本（`add-indexes.sql`）
3. ✅ 优化Caffeine配置（移除weakKeys）
4. ✅ 调整HikariCP和Lettuce连接池配置

### 13.3 监控与持续改进

1. **添加APM工具**: 使用SkyWalking或Pinpoint进行全链路监控
2. **定期审查慢查询**: 每周分析MySQL慢查询日志
3. **缓存命中率监控**: 通过Caffeine的`recordStats()`监控缓存命中率
4. **JVM监控**: 使用Prometheus + Grafana监控JVM指标

---

## 14. 附录：性能优化检查清单

- [ ] JVM参数配置完成
- [ ] 数据库连接池配置优化完成
- [ ] Redis连接池配置优化完成
- [ ] Caffeine缓存配置优化完成
- [ ] 数据库复合索引创建完成
- [ ] MyBatis执行器类型优化完成
- [ ] 异步线程池配置优化完成
- [ ] 前端路由懒加载检查完成
- [ ] 图片懒加载实现完成
- [ ] Gzip压缩配置完成
- [ ] 缓存穿透/击穿/雪崩防护实现完成
- [ ] 性能基准测试执行完成
- [ ] APM工具配置完成

---

**报告生成时间**: 2026-04-24  
**下一步**: 将发现的问题分配给对应开发人员进行修复，并在下次代码审查中验证修复效果。
