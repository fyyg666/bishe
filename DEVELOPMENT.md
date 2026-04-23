# 开发指南

本文档为开发人员提供编码规范、Git使用规范和测试指南。

## 开发环境配置

### 1. IDE配置

#### IntelliJ IDEA

```bash
# 安装插件
- Lombok
- MyBatis Plus
- Maven Helper
- SonarLint

# 配置编码
File → Settings → Editor → File Encodings
- Global Encoding: UTF-8
- Project Encoding: UTF-8

# 配置Maven
File → Settings → Build → Maven
- Maven home path: /path/to/maven
- settings.xml: ~/.m2/settings.xml
```

#### VS Code

```json
// settings.json
{
  "editor.formatOnSave": true,
  "editor.tabSize": 2,
  "files.eol": "\n",
  "files.encoding": "utf8",
  "[java]": {
    "editor.defaultFormatter": "redhat.java"
  },
  "[javascript]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "[vue]": {
    "editor.defaultFormatter": "Vue.volar"
  }
}
```

### 2. Git配置

```bash
# 配置用户信息
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# 启用颜色输出
git config --global color.ui auto

# 设置别名
git config --global alias.st status
git config --global alias.co checkout
git config --global alias.br branch
git config --global alias.ci commit
```

## 代码规范

### Java代码规范

#### 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | UpperCamelCase | UserService |
| 方法名 | lowerCamelCase | getUserById |
| 变量名 | lowerCamelCase | userName |
| 常量 | UPPER_SNAKE_CASE | MAX_PAGE_SIZE |
| 包名 | 全小写 | com.library.system |
| 枚举 | UpperCamelCase | UserStatus |

#### 类结构组织

```java
package com.library.system;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 图书管理服务
 *
 * @author Developer
 * @since 2024-01-01
 */
@Service
@Slf4j
public class BookService {

    // 常量
    private static final int MAX_BORROW_COUNT = 5;

    // 依赖注入
    private final BookMapper bookMapper;
    private final CacheService cacheService;

    // 构造函数
    public BookService(BookMapper bookMapper, CacheService cacheService) {
        this.bookMapper = bookMapper;
        this.cacheService = cacheService;
    }

    // 公有方法
    public Book getById(Long id) {
        return bookMapper.selectById(id);
    }

    // 私有方法
    private void validateBook(Book book) {
        // 验证逻辑
    }
}
```

#### 注解使用

```java
// Controller层
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    @GetMapping("/{id}")
    public Result<Book> getById(@PathVariable Long id) {
        return Result.success(bookService.getById(id));
    }

    @PostMapping
    public Result<Book> create(@RequestBody @Valid BookDTO bookDTO) {
        return Result.success(bookService.create(bookDTO));
    }
}

// Service层
@Service
@RequiredArgsConstructor
public class BookService {

    // 业务逻辑
}

// Mapper层
@Mapper
public interface BookMapper extends BaseMapper<Book> {
    // 数据访问
}
```

### 前端代码规范

#### 目录规范

```
src/
├── api/                    # API接口命名: xxx.js
├── components/             # 组件命名: XxxXxx.vue (PascalCase)
├── composables/            # Hooks命名: useXxx.js
├── router/                 # 路由配置: index.js
├── store/                  # Store命名: xxx.js
├── styles/                 # 样式文件: xxx.scss
└── views/                  # 页面命名: Xxx.vue (PascalCase)
```

#### 组件规范

```vue
<template>
  <div class="book-list">
    <el-table :data="tableData">
      <el-table-column prop="title" label="书名" />
    </el-table>
    <el-pagination
      v-model:current-page="currentPage"
      :total="total"
      @current-change="handlePageChange"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useBookApi } from '@/api/book'

// 状态
const tableData = ref([])
const currentPage = ref(1)
const total = ref(0)

// 计算属性
const hasData = computed(() => tableData.value.length > 0)

// 方法
const fetchData = async () => {
  const { data } = await useBookApi.list({
    page: currentPage.value,
    size: 10
  })
  tableData.value = data.list
  total.value = data.total
}

// 生命周期
onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.book-list {
  padding: 16px;
}
</style>
```

#### 样式规范

```scss
// 变量定义
$primary-color: #409eff;
$border-radius: 4px;

// Mixins
@mixin flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

// BEM命名
.book-card {
  &__header {
    @include flex-center;
    padding: 16px;
  }

  &__title {
    font-size: 16px;
    font-weight: bold;
  }

  &--active {
    border-color: $primary-color;
  }
}
```

### Git使用规范

#### 分支命名

```
feature/xxx              # 新功能开发
bugfix/xxx               # Bug修复
hotfix/xxx               # 紧急修复
release/xxx              # 发布分支
develop                  # 开发分支
main                     # 主分支
```

#### 提交信息规范

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type类型**：

| 类型 | 说明 |
|------|------|
| feat | 新功能 |
| fix | Bug修复 |
| docs | 文档更新 |
| style | 代码格式 |
| refactor | 重构 |
| perf | 性能优化 |
| test | 测试相关 |
| chore | 构建/工具 |

**示例**：

```bash
# 新功能
git commit -m "feat(book): add ISBN duplicate check

- Add ISBN validation in BookService
- Add BloomFilter for cache penetration prevention"

# Bug修复
git commit -m "fix(borrow): fix concurrent booking issue

- Add distributed lock for seat reservation
- Update reservation status check logic"

# 重构
git commit -m "refactor(auth): extract token refresh logic

- Create TokenRefreshService
- Update JwtTokenProvider"
```

#### 工作流程

```bash
# 1. 创建功能分支
git checkout develop
git pull origin develop
git checkout -b feature/book-management

# 2. 开发并提交
git add .
git commit -m "feat(book): implement book CRUD"

# 3. 同步最新代码
git fetch origin
git rebase origin/develop

# 4. 推送并创建PR
git push origin feature/book-management

# 5. Code Review后合并
git checkout develop
git merge --no-ff feature/book-management
git push origin develop

# 6. 删除功能分支
git branch -d feature/book-management
git push origin --delete feature/book-management
```

### 测试指南

#### 单元测试规范

```java
// 命名规范
// 测试类: XxxServiceTest
// 测试方法: void should_xxx_when_xxx()

@SpringBootTest
class BookServiceTest {

    @MockBean
    private BookMapper bookMapper;

    @Autowired
    private BookService bookService;

    @Test
    void should_return_book_when_book_exists() {
        // Given
        Long bookId = 1L;
        Book expectedBook = Book.builder()
            .id(bookId)
            .title("测试图书")
            .build();
        when(bookMapper.selectById(bookId)).thenReturn(expectedBook);

        // When
        Book result = bookService.getById(bookId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("测试图书");
    }

    @Test
    void should_throw_exception_when_book_not_found() {
        // Given
        Long bookId = 999L;
        when(bookMapper.selectById(bookId)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> bookService.getById(bookId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("图书不存在");
    }
}
```

#### 前端测试规范

```javascript
// 测试文件: xxx.test.js
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import BookList from '@/views/book/BookList.vue'

describe('BookList.vue', () => {
  it('should render book list', () => {
    const wrapper = mount(BookList, {
      props: {
        books: [
          { id: 1, title: 'Book 1' },
          { id: 2, title: 'Book 2' }
        ]
      }
    })

    expect(wrapper.findAll('.book-item')).toHaveLength(2)
  })

  it('should emit page-change event', async () => {
    const wrapper = mount(BookList)

    await wrapper.find('.el-pagination').vm.$emit('current-change', 2)

    expect(wrapper.emitted('page-change')).toBeTruthy()
    expect(wrapper.emitted('page-change')[0]).toEqual([2])
  })
})
```

### API开发规范

#### RESTful风格

```java
// 资源命名: 复数名词
// GET /api/books          - 查询列表
// GET /api/books/{id}     - 查询单个
// POST /api/books         - 创建
// PUT /api/books/{id}     - 更新
// DELETE /api/books/{id}  - 删除

@RestController
@RequestMapping("/api/books")
public class BookController {

    @GetMapping
    public Result<PageResult<BookVO>> list(BookQueryDTO query) {
        return Result.success(bookService.list(query));
    }

    @GetMapping("/{id}")
    public Result<BookVO> getById(@PathVariable Long id) {
        return Result.success(bookService.getById(id));
    }

    @PostMapping
    public Result<BookVO> create(@RequestBody @Valid CreateBookDTO dto) {
        return Result.success(bookService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody @Valid UpdateBookDTO dto) {
        bookService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return Result.success();
    }
}
```

#### DTO设计

```java
// 查询DTO - 继承分页参数
@Data
public class BookQueryDTO extends PageDTO {
    private String title;
    private String author;
    private String category;
    private String isbn;
}

// 创建DTO - 参数校验
@Data
public class CreateBookDTO {
    @NotBlank(message = "ISBN不能为空")
    @Pattern(regexp = "^\\d{10}|\\d{13}$", message = "ISBN格式不正确")
    private String isbn;

    @NotBlank(message = "书名不能为空")
    @Size(max = 200, message = "书名长度不能超过200")
    private String title;

    @NotBlank(message = "作者不能为空")
    private String author;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    @Min(value = 1, message = "库存至少为1")
    private Integer totalCopies;
}
```

### 代码审查清单

#### 提交前自检

- [ ] 代码格式符合规范
- [ ] 编译通过，无错误
- [ ] 单元测试通过
- [ ] 新功能有测试覆盖
- [ ] 无敏感信息硬编码
- [ ] API文档已更新
- [ ] Git提交信息规范

#### Review关注点

- [ ] 业务逻辑正确性
- [ ] 异常处理完整性
- [ ] 安全性（SQL注入、XSS等）
- [ ] 性能影响
- [ ] 代码可读性
- [ ] 测试覆盖度
