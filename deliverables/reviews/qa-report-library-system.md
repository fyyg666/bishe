# QA Report: Library Management System v2.0.0

| Field | Value |
|-------|-------|
| **Date** | 2025-07-17 |
| **Project** | `c:\Users\12856\Desktop\论文实现\library-system-v2` |
| **Tier** | Standard (Systematic) |
| **Scope** | User Auth, Book Management, Borrow Management, System Config |
| **Framework** | Backend: Spring Boot 3.2.5 / Frontend: Vue 3.4 + Element Plus |
| **Analysis Mode** | Code Review + Static Analysis |

## Health Score: 62/100

| Category | Score | Notes |
|----------|-------|-------|
| Console/Errors | 70 | API path mismatches cause runtime failures |
| API Consistency | 55 | Multiple frontend-backend contract mismatches |
| Functional | 60 | Registration and password change are broken |
| Security/Auth | 75 | Generally sound, minor gaps |
| UX | 80 | Frontend has good fallback handling |
| Configuration | 50 | Missing env files, swagger path errors |

## Summary

| Severity | Count |
|----------|-------|
| 🔴 Critical | 3 |
| 🟠 High | 3 |
| 🟡 Medium | 3 |
| 🟢 Low | 1 |
| **Total** | **10** |

---

## Issues

---

### 🔴 ISSUE-001 (Critical): Registration broken — confirmPassword removed by frontend but required by backend

| Field | Value |
|-------|-------|
| **Type** | Frontend + Backend |
| **Category** | Functional |
| **Component** | Registration flow |

**Description:** 
The frontend `Register.vue` **removes** `confirmPassword` from the request payload before sending it to the backend (`delete submitData.confirmPassword`). However, the backend `RegisterRequest` DTO has `@NotBlank` validation on `confirmPassword`. This causes ALL registration attempts to fail with a 400 validation error.

**Root Cause:**
- Frontend: `frontend/src/views/auth/Register.vue` line 183: `delete submitData.confirmPassword`
- Backend: `backend/.../dto/RegisterRequest.java` lines 42-43: `@NotBlank(message = "确认密码不能为空") private String confirmPassword;`

**Repro Steps:**
1. Navigate to `/register`
2. Fill all fields with valid data, confirmPassword matching password
3. Click "注册"
4. **Observe:** Frontend deletes confirmPassword → Backend rejects with 400 validation error for missing confirmPassword

**Expected:** Registration should succeed.
**Actual:** Registration always fails with validation error.

---

### 🔴 ISSUE-002 (Critical): Change Password API endpoint mismatch

| Field | Value |
|-------|-------|
| **Type** | Frontend + Backend |
| **Category** | Functional |
| **Component** | Password Change |

**Description:**
The frontend calls `PUT /auth/password` for change password, but the backend has NO such endpoint. The backend's password change endpoint is `POST /readers/{id}/password` in `ReaderController`.

**Frontend code:** `frontend/src/api/auth.js` lines 36-42:
```javascript
export function changePassword(data) {
  return request({
    url: '/auth/password',    // ✗ This endpoint does not exist
    method: 'put',
    data
  })
}
```

**Backend actual endpoint:** `backend/.../controller/ReaderController.java` lines 154-170:
```java
@PostMapping("/{id}/password")    // POST /readers/{id}/password
@PreAuthorize("isAuthenticated()")
public ApiResponse<Void> changePassword(@PathVariable Long id, ...)
```

**Repro Steps:**
1. Login as any user
2. Navigate to Profile / 修改密码
3. Enter old and new password, submit
4. **Observe:** 404 error — endpoint not found

**Impact:** Password change feature is completely non-functional.

---

### 🔴 ISSUE-003 (Critical): Book search parameter mismatch — frontend sends wrong query params

| Field | Value |
|-------|-------|
| **Type** | Frontend + Backend |
| **Category** | Functional |
| **Component** | Book Search |

**Description:**
The `BookList.vue` sends `{name, author, category}` as search parameters to the book list API, but the backend controller expects `{keyword, categoryId}`. The parameter names and types are completely different, making all search/filter operations non-functional.

**Frontend** (`frontend/src/views/book/BookList.vue` lines 238-244):
```javascript
const params = {
  ...searchForm,   // { name: 'xxx', author: 'xxx', category: '文学' }
  current: pagination.current,
  size: pagination.size
}
```

**Backend** (`backend/.../controller/BookController.java` lines 52-58):
```java
public ApiResponse<PageResult<BookResponse>> listBooks(
    @RequestParam(defaultValue = "1") Long current,
    @RequestParam(defaultValue = "10") Long size,
    @RequestParam(required = false) String keyword,   // ✗ Not "name"/"author"
    @RequestParam(required = false) Long categoryId)  // ✗ Not String "category"
```

**Specific mismatches:**
| Frontend param | Backend param | Issue |
|---------------|---------------|-------|
| `name` (string) | `keyword` (string) | Name not recognized — search by title fails |
| `author` (string) | `keyword` (string) | Author not recognized — search by author fails |
| `category` (string, e.g. "文学") | `categoryId` (Long, e.g. 1) | Type mismatch — filter by category fails |

**Impact:** All book search and filter operations produce empty/wrong results.

---

### 🟠 ISSUE-004 (High): `/categories` endpoint returns 401 for anonymous users

| Field | Value |
|-------|-------|
| **Type** | Backend |
| **Category** | Functional / Security |
| **Component** | Category API |

**Description:**
The `BookCategoryController` exposes `GET /categories` and declares `@PreAuthorize("permitAll()")`, intending it to be public. However, `SecurityConfig` does NOT include `GET /categories` in the `.permitAll()` URL matchers. Since the default rule is `.anyRequest().authenticated()`, unauthenticated requests to `/categories` are blocked at the URL authorization level, before method-level security is evaluated.

**SecurityConfig** (`backend/.../config/SecurityConfig.java` lines 156-158):
```java
// Only /books and /seats GET endpoints are permitAll
.requestMatchers(HttpMethod.GET, "/books", "/books/hot", "/books/{id}").permitAll()
.requestMatchers(HttpMethod.GET, "/books/check-isbn").permitAll()
.requestMatchers(HttpMethod.GET, "/seats", "/seats/check-availability").permitAll()
// ✗ No GET /categories in permitAll!
```
```java
.anyRequest().authenticated()  // catches /categories
```

**Impact:** Anonymous (pre-login) users cannot load categories. The frontend falls back to hardcoded categories, which may be stale or incorrect.

---

### 🟠 ISSUE-005 (High): Swagger OpenAPI group paths don't match actual controller paths

| Field | Value |
|-------|-------|
| **Type** | Backend |
| **Category** | Configuration |
| **Component** | API Documentation |

**Description:**
In `application.yml`, the `springdoc.group-configs` defines path patterns that do not match the actual controller `@RequestMapping` values. This means the Swagger UI grouping is incorrect and some endpoints may not appear in their expected groups.

**Mismatches:**
| Group | Configured Path | Actual Controller Path |
|-------|----------------|----------------------|
| 借阅模块 | `/borrow-records/**` | `/borrows/**` |
| 积分模块 | `/points/**`, `/credits/**` | `/credits/**` only |
| 志愿模块 | `/volunteers/**`, `/volunteer-activities/**` | `/volunteers/**` only |

**Impact:** Swagger docs generate incorrect groupings. "借阅模块" (Borrow) in Swagger shows no endpoints.

---

### 🟠 ISSUE-006 (High): Missing frontend environment configuration files

| Field | Value |
|-------|-------|
| **Type** | Frontend |
| **Category** | Configuration |
| **Component** | Environment |

**Description:**
The frontend project has **no** `.env`, `.env.development`, or `.env.production` files. The `request.js` relies on `import.meta.env.VITE_API_BASE_URL` defaulting to `'/api/v1'`, and `VITE_API_TIMEOUT` defaulting to 30000ms. Without env files:
- API base URL cannot be configured per environment
- Vite dev server proxy (`/api → localhost:8080`) relies on the context path `/api/v1`, but the proxy only matches `/api` — requests to `/api/v1/auth/login` would NOT be proxied

**Vite proxy config** (`frontend/vite.config.js` lines 28-33):
```javascript
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
},
```

**Issue:** The proxy matches `/api/**` but the frontend sends requests to `/api/v1/**`. Since `/api/v1/` starts with `/api`, the proxy will match. This actually works, but is fragile.

**Impact:** Environment-specific configuration is not explicit; relies on implicit defaults.

---

### 🟡 ISSUE-007 (Medium): Password strength validation mismatch — frontend vs backend

| Field | Value |
|-------|-------|
| **Type** | Frontend + Backend |
| **Category** | Functional / UX |
| **Component** | Registration |

**Description:**
The frontend registration form validates passwords as minimum **6** characters, but the backend `RegisterRequest` DTO requires minimum **8** characters with uppercase, lowercase, digit, and special character. This creates a frustrating UX where the frontend accepts a password (e.g., "password1") but the backend rejects it.

**Frontend** (`frontend/src/views/auth/Register.vue` line 140):
```javascript
password: [
  { required: true, message: '请输入密码', trigger: 'blur' },
  { min: 6, message: '密码至少 6 位', trigger: 'blur' }
]
```

**Backend** (`backend/.../dto/RegisterRequest.java` lines 32-36):
```java
@Size(min = 8, max = 50, message = "密码长度必须在8-50个字符之间")
@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
        message = "密码必须包含大小写字母、数字和特殊字符")
```

**Impact:** Users who enter a 6-7 character password will pass frontend validation but receive a backend 400 error with no clear explanation.

---

### 🟡 ISSUE-008 (Medium): Empty publishDate causes backend validation failure

| Field | Value |
|-------|-------|
| **Type** | Frontend + Backend |
| **Category** | Functional |
| **Component** | Book Add/Edit |

**Description:**
When adding a book without selecting a publish date, the frontend sends `publishDate: ''` (empty string). The backend `BookRequest` DTO has `@Pattern` validation on `publishDate`, and empty strings fail `@Pattern` validation (only null values pass through).

**Frontend** (`frontend/src/views/book/BookAdd.vue` line 242):
```javascript
publishDate: form.publishDate ? form.publishDate.substring(0, 7) : '',
// When no date selected: publishDate = ''
```

**Backend** (`backend/.../dto/BookRequest.java` lines 57-58):
```java
@Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "出版日期格式应为yyyy-MM")
private String publishDate;
```

**Impact:** Adding a book without selecting a publish date will fail with validation error.

---

### 🟡 ISSUE-009 (Medium): Hardcoded category name-to-ID mapping in BookAdd.vue

| Field | Value |
|-------|-------|
| **Type** | Frontend |
| **Category** | Functional |
| **Component** | Book Add/Edit |

**Description:**
The `BookAdd.vue` uses a hardcoded JavaScript object to map Chinese category names to numeric IDs:

```javascript
const categoryNameToId = {
  '文学': 1, '科技': 2, '历史': 3,
  '艺术': 4, '哲学': 5, '经济': 6
}
```

If the database's `book_category` table has different IDs, or if categories are added/removed in the database, this mapping becomes incorrect. The frontend will assign wrong `categoryId` values when creating/editing books.

---

### 🟢 ISSUE-010 (Low): AuthController logout has minimal token validation

| Field | Value |
|-------|-------|
| **Type** | Backend |
| **Category** | Functional |
| **Component** | Auth |

**Description:**
The logout endpoint only checks if `authHeader.startsWith("Bearer ")` but does not validate that there is actual token content after the prefix. An empty token like `"Bearer "` (nothing after the space) would pass this check.

```java
// AuthController.java line 107:
if (authHeader != null && authHeader.startsWith(Constants.Token.BEARER_PREFIX)) {
    String accessToken = authHeader.substring(Constants.Token.BEARER_PREFIX.length());
    authService.logout(accessToken);
```

While `AuthService.logout()` handles a null JTI gracefully, this is still a code quality concern.

---

## Ship Readiness Assessment

| Metric | Value |
|--------|-------|
| **Health Score** | **62/100** |
| **Issues Found** | 10 |
| **🔴 Critical (must fix before ship)** | 3 — Registration, Password Change, Book Search |
| **🟠 High (should fix before ship)** | 3 — Category API auth, Swagger config, Missing env |
| **🟡 Medium** | 3 |
| **🟢 Low** | 1 |

**Ship verdict: 🚫 DO NOT SHIP** — 3 critical bugs render core features (registration, password change, book search) non-functional.

### Top 3 Things to Fix

1. **🔴 ISSUE-001**: Fix registration by either (a) removing `@NotBlank` on `confirmPassword` in backend and validating in service, or (b) not deleting confirmPassword on frontend
2. **🔴 ISSUE-002**: Align frontend change-password API call (`PUT /auth/password`) with backend endpoint (`POST /readers/{id}/password`)
3. **🔴 ISSUE-003**: Align frontend book search params (`name`/`author`/`category`) with backend expected params (`keyword`/`categoryId`)

---

## Issue Classification Summary

| # | Severity | Layer | Component | Root Cause |
|---|----------|-------|-----------|------------|
| 001 | 🔴 Critical | FE + BE | Registration | confirmPassword field deleted by FE but required by BE DTO |
| 002 | 🔴 Critical | FE + BE | Password Change | Frontend calls wrong endpoint URL |
| 003 | 🔴 Critical | FE + BE | Book Search | Frontend sends wrong query parameter names |
| 004 | 🟠 High | BE | Category API | Missing URL permitAll rule in SecurityConfig |
| 005 | 🟠 High | BE | Swagger | Wrong path patterns in springdoc group-configs |
| 006 | 🟠 High | FE | Config | Missing .env files |
| 007 | 🟡 Medium | FE + BE | Registration | Password min-length mismatch (FE:6 / BE:8) |
| 008 | 🟡 Medium | FE + BE | Book Add | Empty string vs null for optional `publishDate` |
| 009 | 🟡 Medium | FE | Book Add | Hardcoded category→ID mapping |
| 010 | 🟢 Low | BE | Auth | Weak token prefix validation in logout |
