SET @hash = '$2b$10$7RLJrHlaGH6krBv1F5yuoecIG9BOomgU6ZQuSJktBJPvWUzAf2aE6';
UPDATE sys_user SET password = @hash WHERE username = 'admin';
