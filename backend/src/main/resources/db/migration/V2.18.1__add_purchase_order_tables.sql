CREATE TABLE IF NOT EXISTS purchase_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '采购单ID',
    order_no VARCHAR(32) NOT NULL UNIQUE COMMENT '采购单号',
    title VARCHAR(200) COMMENT '采购单标题',
    supplier VARCHAR(100) COMMENT '供应商',
    total_amount DECIMAL(10,2) DEFAULT 0 COMMENT '总金额',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING待审批/APPROVED已审批/RECEIVED已收货/CANCELLED已取消',
    created_by BIGINT COMMENT '创建人ID',
    remark VARCHAR(500) COMMENT '备注',
    version INT DEFAULT 0 COMMENT '版本号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_order_no (order_no),
    INDEX idx_status (status),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单表';

CREATE TABLE IF NOT EXISTS purchase_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '采购明细ID',
    order_id BIGINT NOT NULL COMMENT '采购单ID',
    book_title VARCHAR(200) NOT NULL COMMENT '书名',
    isbn VARCHAR(20) COMMENT 'ISBN',
    author VARCHAR(100) COMMENT '作者',
    publisher VARCHAR(100) COMMENT '出版社',
    price DECIMAL(10,2) COMMENT '单价',
    quantity INT NOT NULL DEFAULT 1 COMMENT '采购数量',
    received_quantity INT DEFAULT 0 COMMENT '已收货数量',
    cataloged_quantity INT DEFAULT 0 COMMENT '已入库数量',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING待收货/RECEIVED已收货/CATALOGED已入库',
    remark VARCHAR(500) COMMENT '备注',
    version INT DEFAULT 0 COMMENT '版本号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_order_id (order_id),
    INDEX idx_isbn (isbn),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单明细表';

INSERT INTO purchase_order (order_no, title, supplier, total_amount, status, remark) VALUES
('PO20260531001', '2026年春季图书采购', '新华书店', 2560.00, 'RECEIVED', '春季学期教材及参考书采购');

INSERT INTO purchase_order_item (order_id, book_title, isbn, author, publisher, price, quantity, received_quantity, cataloged_quantity, status) VALUES
(1, 'Spring Boot实战', '9787115417319', '丁雪丰', '人民邮电出版社', 69.00, 5, 5, 0, 'RECEIVED'),
(1, '算法(第4版)', '9787115293800', 'Robert Sedgewick', '人民邮电出版社', 99.00, 3, 3, 0, 'RECEIVED'),
(1, '深入理解计算机系统', '9787111544937', 'Randal E.Bryant', '机械工业出版社', 139.00, 2, 2, 0, 'RECEIVED');
