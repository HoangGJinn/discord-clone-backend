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

-- Tạo bảng profile_effects và chèn dữ liệu
CREATE TABLE IF NOT EXISTS profile_effects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    image_url VARCHAR(255),
    animation_url VARCHAR(255),
    price DOUBLE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO profile_effects (name, description, image_url, price)
SELECT * FROM (SELECT 'Aespa Fanlight', 'Hiệu ứng ánh sáng Aespa', 'assets/avatar/frame_aespa_fanlight.png', 4.99) AS tmp
WHERE NOT EXISTS (SELECT name FROM profile_effects WHERE name = 'Aespa Fanlight') LIMIT 1;

INSERT INTO profile_effects (name, description, image_url, price)
SELECT * FROM (SELECT 'Black Hole', 'Hố đen vũ trụ', 'assets/avatar/frame_black_hole.png', 5.99) AS tmp
WHERE NOT EXISTS (SELECT name FROM profile_effects WHERE name = 'Black Hole') LIMIT 1;

INSERT INTO profile_effects (name, description, image_url, price)
SELECT * FROM (SELECT 'Aurora', 'Hiệu ứng cực quang', 'assets/avatar/frame_aurora.png', 5.99) AS tmp
WHERE NOT EXISTS (SELECT name FROM profile_effects WHERE name = 'Aurora') LIMIT 1;

INSERT INTO profile_effects (name, description, image_url, price)
SELECT * FROM (SELECT 'Bubble Tea', 'Trà sữa trân châu', 'assets/avatar/frame_bubble_tea.png', 3.99) AS tmp
WHERE NOT EXISTS (SELECT name FROM profile_effects WHERE name = 'Bubble Tea') LIMIT 1;

INSERT INTO profile_effects (name, description, image_url, price)
SELECT * FROM (SELECT 'Cozy Cat', 'Mèo lười', 'assets/avatar/frame_cozycat.png', 3.99) AS tmp
WHERE NOT EXISTS (SELECT name FROM profile_effects WHERE name = 'Cozy Cat') LIMIT 1;
