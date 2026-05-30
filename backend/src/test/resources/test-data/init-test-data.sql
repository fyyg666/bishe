-- 测试数据初始化
-- 注意：表名和列名需与实际数据库一致

-- 测试用户 (密码: test123，实际为BCrypt加密)
INSERT INTO sys_user (id, username, password, real_name, role, status, credit_score, borrow_count, version)
VALUES (100, 'admin', '$2a$10$dQw4w9WgXcQ', '管理员', 'ADMIN', 'NORMAL', 100, 0, 0),
       (101, 'librarian1', '$2a$10$dQw4w9WgXcQ', '馆员1', 'LIBRARIAN', 'NORMAL', 100, 0, 0),
       (102, 'reader1', '$2a$10$dQw4w9WgXcQ', '读者张三', 'READER', 'NORMAL', 100, 0, 0),
       (103, 'reader2', '$2a$10$dQw4w9WgXcQ', '读者李四', 'READER', 'NORMAL', 80, 2, 0),
       (104, 'disabled_user', '$2a$10$dQw4w9WgXcQ', '被禁用用户', 'READER', 'DISABLED', 50, 0, 0);

-- 测试图书分类
INSERT INTO book_category (id, name, code, parent_id, sort_order)
VALUES (1, '计算机科学', 'CS', 0, 1),
       (2, '文学', 'LIT', 0, 2),
       (3, '科学', 'SCI', 0, 3);

-- 测试图书
INSERT INTO book (id, isbn, name, author, publisher, category_id, total_stock, available_stock, year, price, description, version)
VALUES (100, '978-7-111-66666-1', 'Java编程思想', 'Bruce Eckel', '机械工业出版社', 1, 10, 8, '2024', 89.00, 'Java经典教材', 0),
       (101, '978-7-111-66666-2', '数据结构与算法', '邓俊辉', '清华大学出版社', 1, 5, 5, '2024', 69.00, '数据结构教材', 0),
       (102, '978-7-111-66666-3', '三体', '刘慈欣', '重庆出版社', 2, 3, 3, '2023', 39.00, '科幻小说', 0);

-- 测试阅览室
INSERT INTO reading_room (id, name, capacity, status)
VALUES (1, '静读室', 30, 'OPEN'),
       (2, '研讨室', 10, 'OPEN');

-- 测试座位
INSERT INTO seat (id, room_id, seat_number, status, version)
VALUES (100, 1, 'A-01', 'AVAILABLE', 0),
       (101, 1, 'A-02', 'AVAILABLE', 0),
       (102, 2, 'B-01', 'AVAILABLE', 0),
       (103, 2, 'B-02', 'OCCUPIED', 0);

-- 测试公告
INSERT INTO announcement (id, title, content, status, create_by, create_time)
VALUES (100, '开馆通知', '图书馆将于2026年1月1日正常开放', 'PUBLISHED', 'admin', NOW()),
       (101, '系统维护', '系统将于本周末进行升级', 'DRAFT', 'admin', NOW());
