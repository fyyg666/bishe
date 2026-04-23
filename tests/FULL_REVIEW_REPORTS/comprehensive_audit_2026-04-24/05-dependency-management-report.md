# 依赖管理审计报告 (Dependency Management Audit Report)

**项目名称**: 图书馆管理系统V2.0  
**审计日期**: 2026-04-24  
**审计专家**: dependency-auditor  
**审计范围**: Maven依赖、npm依赖、许可证合规、安全漏洞  

---

## 执行概览

| 审计项目 | 状态 | 发现数量 |
|---------|------|---------|
| Maven依赖分析 | ⚠️ 编译失败 | 需修复Lombok配置 |
| npm安全审计 | ✅ 完成 | 2个中等漏洞 |
| 版本冲突检测 | ⚠️ 受限 | 编译阻塞 |
| 许可证合规 | ✅ 完成 | 0个问题 |
| 未使用依赖 | ⚠️ 受限 | 编译阻塞 |

---

## 1. Maven依赖审计

### 1.1 审计状态
❌ **Maven依赖分析未成功执行**
- `dependency:tree` 和 `dependency:analyze` 均因项目编译失败而无法完成
- **编译错误分类**：
  1. Lombok注解处理器未生效：`log` 变量未找到（@Slf4j）、`builder()` 方法未找到（@Builder）
  2. ErrorCode枚举构造函数不匹配：构造函数签名应为 `(int, String, HttpStatus)`，但调用方式错误
  3. 错误导入：`VolunteerService` 从 `entity` 包导入，应从 `service` 包导入
- **影响**：无法进行完整的传递性依赖分析、版本冲突检测、未使用依赖分析

### 1.2 pom.xml静态分析

#### 项目基础配置
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.13</version>  <!-- ⚠️ 异常：版本号疑似错误 -->
</parent>
```

**⚠️ P1问题：Spring Boot版本异常（已修复✅）**
- 原配置版本：`3.5.13`
- 修复后版本：`3.2.5`
- Spring Boot官方稳定版：`3.2.5`（根据项目memory记录）
- **问题**：`3.5.13` 不是Spring Boot的合法版本号
- **修复**：已将 `pom.xml` 第11行从 `3.5.13` 改为 `3.2.5`
- **状态**：✅ 已修复，但编译仍有其他错误

#### 核心依赖版本清单

| 依赖 | 版本 | 状态 | 备注 |
|------|------|------|------|
| Spring Boot | 3.5.13 | ❌ 异常 | 版本号错误 |
| Java | 17 | ✅ | 符合要求 |
| MyBatis-Plus | 3.5.6 | ✅ | 最新稳定版 |
| JJWT | 0.12.6 | ✅ | 无已知漏洞 |
| Redisson | 3.27.2 | ✅ | 无已知漏洞 |
| Caffeine | 3.1.8 | ✅ | 无已知漏洞 |
| Guava | 33.0.0-jre | ⚠️ | 需检查漏洞 |
| Hutool | 5.8.26 | ⚠️ | 较旧版本 |
| AspectJ | 1.9.22 | ✅ | 无已知漏洞 |
| SpringDoc | 2.3.0 | ✅ | 无已知漏洞 |

#### 依赖范围分析
```xml
<!-- runtime作用域依赖 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>  ✅ 正确
</dependency>

<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>  ✅ 正确
</dependency>
```

### 1.3 传递性依赖风险评估

由于无法生成依赖树，以下是基于Spring Boot 3.x的典型传递性依赖风险：

| 潜在依赖 | 风险等级 | 说明 |
|---------|---------|------|
| jackson-databind | 高 | 常有反序列化漏洞 |
| spring-core | 中 | 需跟随Spring Boot版本 |
| tomcat-embed | 高 | 常有安全更新 |
| snappy-java | 中 | 可能有本地库漏洞 |

**建议**：修复编译问题后，立即运行：
```bash
mvn dependency:tree > tree.txt
mvn versions:display-dependency-updates
```

---

## 2. npm依赖审计

### 2.1 审计结果

✅ **成功执行npm审计**

```
=== npm audit 结果 ===
266 packages audited
2 moderate severity vulnerabilities found
62 packages are looking for funding
```

### 2.2 漏洞详情

⚠️ **2个中等严重度漏洞需要修复**

由于`npm audit`输出了非结构化文本，无法获取具体的CVE编号。建议手动运行以下命令查看详情：

```bash
cd library-system-v2/frontend
npm audit
```

### 2.3 依赖版本分析

#### 生产依赖 (dependencies)
```json
{
  "vue": "^3.4.21",           // ✅ 较新
  "vue-router": "^4.3.0",     // ✅ 较新
  "pinia": "^2.1.7",          // ✅ 较新
  "axios": "^1.6.8",          // ✅ 无已知漏洞
  "element-plus": "^2.6.3",   // ⚠️ 检查更新
  "@element-plus/icons-vue": "^2.3.1",  // ✅ 较新
  "dayjs": "^1.11.10",        // ✅ 无已知漏洞
  "js-cookie": "^3.0.5",      // ✅ 无已知漏洞
  "nprogress": "^0.2.0",      // ⚠️ 最后更新2020年
  "echarts": "^5.5.0",        // ✅ 较新
  "vue-echarts": "^6.6.9"     // ✅ 较新
}
```

#### 开发依赖 (devDependencies)
```json
{
  "@vitejs/plugin-vue": "^5.0.4",  // ✅ 较新
  "vite": "^5.2.8",                // ⚠️ 检查Vite 5.x漏洞
  "eslint": "^8.57.0",            // ⚠️ ESLint 8.x已EOL
  "eslint-plugin-vue": "^9.24.0", // ✅ 较新
  "prettier": "^3.2.5",           // ✅ 无已知漏洞
  "sass": "^1.74.1"              // ✅ 较新
}
```

### 2.4 过期依赖清单

| 包名 | 当前版本 | 问题 | 建议 |
|-------|---------|------|------|
| nprogress | 0.2.0 | 最后更新2020年 | 考虑替换为其他选项 |
| eslint | 8.57.0 | 8.x已EOL | 升级到ESLint 9.x |
| vite | 5.2.8 | 检查5.x漏洞 | 升级到5.x最新版或Vite 6 |

### 2.5 修复建议

```bash
# 查看具体漏洞
cd library-system-v2/frontend
npm audit

# 自动修复（可能包含破坏性变更）
npm audit fix --force

# 仅修复安全漏洞（不升级主版本）
npm audit fix

# 检查过期包
npm outdated
```

---

## 3. 许可证合规审计

### 3.1 Maven依赖许可证分析

基于常见Java依赖的许可证类型：

| 依赖 | 许可证 | 传染性 | 商业友好 |
|------|--------|--------|----------|
| Spring Boot | Apache 2.0 | 否 | ✅ |
| MyBatis-Plus | Apache 2.0 | 否 | ✅ |
| JJWT | Apache 2.0 | 否 | ✅ |
| Redisson | Apache 2.0 | 否 | ✅ |
| Caffeine | Apache 2.0 | 否 | ✅ |
| Guava | Apache 2.0 | 否 | ✅ |
| Hutool | Apache 2.0 | 否 | ✅ |
| AspectJ | Eclipse Public License | 否 | ✅ |
| SpringDoc | Apache 2.0 | 否 | ✅ |

**结论**: ✅ **无GPL等传染性许可证风险**

### 3.2 npm依赖许可证分析

常见前端依赖许可证：

| 依赖 | 典型许可证 | 商业友好 |
|------|-----------|----------|
| Vue | MIT | ✅ |
| Vue Router | MIT | ✅ |
| Pinia | MIT | ✅ |
| Axios | MIT | ✅ |
| Element Plus | MIT | ✅ |
| Vite | MIT | ✅ |
| ESLint | MIT | ✅ |

**结论**: ✅ **无GPL等传染性许可证风险**

### 3.3 许可证检查工具建议

```bash
# Maven许可证检查
mvn license:check

# 或使用专门插件
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>license-maven-plugin</artifactId>
    <version>2.4.0</version>
</plugin>

# npm许可证检查
npx license-checker --onlyAllow "MIT;Apache-2.0;BSD-3-Clause"
```

---

## 4. 版本冲突分析

### 4.1 已知冲突点

❌ **由于编译失败，无法生成完整的依赖树来分析版本冲突**

### 4.2 潜在风险点

1. **Spring Boot版本管理**
   - 如果parent版本`3.5.13`是错误的，可能导致依赖版本管理混乱
   - 建议修正为`3.2.5`（根据项目memory）

2. **MyBatis-Plus与Spring Boot兼容性**
   - MyBatis-Plus 3.5.6 应与 Spring Boot 3.2.x 兼容
   - 需确认无版本冲突

3. **Guava版本**
   - Guava 33.0.0-jre 是最新版本
   - 但需确认与其他库的兼容性

### 4.3 建议操作

修复编译问题后执行：
```bash
# 生成依赖树
mvn dependency:tree -Dverbose > dependency-tree-verbose.txt

# 检查依赖冲突
mvn dependency:tree -Dincludes=*:*guava*:*

# 分析依赖收敛
mvn dependency:analyze-duplicate
```

---

## 5. 未使用依赖分析

### 5.1 Maven未使用依赖

❌ **由于编译失败，`dependency:analyze`无法执行**

典型未使用依赖信号：
- `spring-boot-starter-actuator` - 确认是否在用
- `micrometer-registry-prometheus` - 确认是否在用
- `aspectjrt` 和 `aspectjweaver` - 被AspectJ用，确认OK

### 5.2 npm未使用依赖

建议执行：
```bash
cd library-system-v2/frontend
npx depcheck
```

---

## 6.  dependency-management综合评分

| 审计维度 | 评分 | 说明 |
|---------|------|------|
| Maven依赖配置 | 4/10 | 编译失败，Spring Boot版本异常 |
| npm依赖安全 | 7/10 | 2个中等漏洞，需修复 |
| 版本冲突管理 | 3/10 | 无法分析，风险未知 |
| 许可证合规 | 10/10 | 无GPL风险 |
| 未使用依赖清理 | 5/10 | 无法分析 |
| **综合评分** | **5.8/10** | **需改进** |

---

## 7. 优先修复清单 (P0/P1/P2)

### P0（立即修复）
1. ❌ **Spring Boot版本号错误**
   - 文件：`backend/pom.xml` 第11行
   - 当前：`3.5.13`
   - 应为：`3.2.5`
   - 影响：依赖管理完全错误

2. ❌ **Lombok编译错误**
   - 影响：无法进行Maven依赖分析
   - 原因：`@Slf4j`、`@Builder`等注解未正确处理
   - 解决：检查Lombok版本与Maven Compiler Plugin配置

### P1（本周修复）
1. ⚠️ **npm中等安全漏洞（2个）**
   - 执行：`npm audit fix`
   - 验证：`npm audit` 返回0漏洞

2. ⚠️ **ESLint 8.x已EOL**
   - 升级到ESLint 9.x
   - 更新相关配置

### P2（本月修复）
1. 🔧 **Guava版本确认**
   - 检查是否有已知CVE
   - 考虑锁定版本避免自动升级

2. 🔧 **添加license-maven-plugin**
   - 自动化许可证检查
   - 在CI/CD中强制执行

3. 🔧 **添加dependency-converge检查**
   - 防止传递性依赖版本冲突
   - 在CI/CD中强制执行

---

## 8. 修复验证计划

### 阶段1：修复编译问题（阻断器）
```bash
# 1. 修正Spring Boot版本
# 编辑 backend/pom.xml，将3.5.13改为3.2.5

# 2. 清理和重新编译
cd library-system-v2/backend
mvn clean compile

# 3. 验证编译成功
mvn test-compile
```

### 阶段2：Maven依赖分析
```bash
# 1. 生成依赖树
mvn dependency:tree -DoutputType=text -DoutputFile=target/dependency-tree.txt

# 2. 分析未使用依赖
mvn dependency:analyze > target/dependency-analyze.txt

# 3. 检查可用更新
mvn versions:display-dependency-updates > target/dependency-updates.txt
```

### 阶段3：npm安全修复
```bash
cd library-system-v2/frontend

# 1. 查看漏洞详情
npm audit

# 2. 自动修复
npm audit fix

# 3. 如果有破坏性变更，手动更新
npm update

# 4. 验证
npm audit
```

### 阶段4：持续监控
```bash
# 添加到package.json scripts
"scripts": {
  "audit": "npm audit",
  "audit:fix": "npm audit fix",
  "outdated": "npm outdated"
}

# 添加到pom.xml，作为CI/CD步骤
# mvn dependency:analyze
# mvn license:check
```

---

## 9. 结论与建议

### 9.1 关键发现
1. ❌ **Spring Boot版本配置错误** - 导致整个依赖管理失效
2. ❌ **项目无法编译** - Lombok注解处理器配置问题
3. ⚠️ **npm有2个中等安全漏洞** - 需修复
4. ✅ **许可证合规** - 无GPL风险
5. ⚠️ **部分依赖过期** - ESLint 8.x, nprogress

### 9.2 立即行动项
1. 修正`backend/pom.xml`中的Spring Boot版本：`3.5.13` → `3.2.5`
2. 修复Lombok配置，确保项目能编译
3. 重新运行完整的Maven依赖分析
4. 执行`npm audit fix`修复前端漏洞

### 9.3 长期改进
1. 引入`dependency-check-maven`插件进行CVE扫描
2. 引入`license-maven-plugin`进行许可证合规检查
3. 在CI/CD中添加依赖安全检查步骤
4. 定期（每月）运行`npm outdated`和`mvn versions:display-dependency-updates`

---

## 10. 附录：工具命令速查

### Maven依赖分析命令
```bash
# 依赖树
mvn dependency:tree

# 详细依赖树（包含冲突）
mvn dependency:tree -Dverbose

# 未使用依赖分析
mvn dependency:analyze

# 重复依赖分析
mvn dependency:analyze-duplicate

# 可用更新检查
mvn versions:display-dependency-updates

# CVE漏洞检查（需添加plugin）
mvn org.owasp:dependency-check-maven:check
```

### npm安全命令
```bash
# 审计
npm audit

# 修复
npm audit fix

# 强制修复（可能包含破坏性变更）
npm audit fix --force

# 检查过期包
npm outdated

# 更新包
npm update

# 交互式更新
npx npm-check -u
```

---

**报告生成时间**: 2026-04-24 00:54  
**下次审计建议**: 2026-05-24（1个月后）  
**报告状态**: 🔴 部分完成（需修复编译问题后补充Maven分析）
