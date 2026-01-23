-- Script khởi tạo bảng roles và dữ liệu mặc định
-- Chạy script này trong database của bạn để tạo bảng roles

-- Tạo bảng roles nếu chưa tồn tại
CREATE TABLE IF NOT EXISTS roles (
    role_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Tạo bảng user_roles nếu chưa tồn tại (bảng trung gian many-to-many)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE
);

-- Cập nhật bảng users để thêm các cột mới
ALTER TABLE users 
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS is_email_verified BOOLEAN DEFAULT FALSE;

-- Chỉnh sửa constraint của password (bỏ unique)
ALTER TABLE users 
    MODIFY COLUMN password VARCHAR(255) NOT NULL;

-- Thêm dữ liệu roles mặc định
INSERT INTO roles (name, description) VALUES 
    ('USER_DEFAULT', 'Người dùng thông thường'),
    ('ADMIN', 'Quản trị viên hệ thống'),
    ('MODERATOR', 'Người kiểm duyệt')
ON DUPLICATE KEY UPDATE description = VALUES(description);
