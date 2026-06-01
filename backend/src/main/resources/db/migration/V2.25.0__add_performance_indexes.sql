CREATE INDEX idx_book_branch_status ON book(branch_id, status);

CREATE INDEX idx_borrow_branch_status ON borrow_record(branch_id, status);

CREATE INDEX idx_borrow_user_return ON borrow_record(user_id, return_date);

CREATE INDEX idx_book_isbn ON book(isbn);

CREATE INDEX idx_notification_user_read ON notification(user_id, read_at);

CREATE INDEX idx_operation_log_operator_time ON sys_operation_log(user_id, create_time);

CREATE INDEX idx_serial_issue_sub_status_date ON serial_issue(subscription_id, status, expected_date);
