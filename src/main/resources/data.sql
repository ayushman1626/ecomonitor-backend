-- Users
INSERT INTO users (id, username, full_name, email, password, is_verified, created_at)
VALUES ('11111111-1111-1111-1111-111111111111', 'testuser', 'Test User', 'test@example.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', true, CURRENT_TIMESTAMP);

-- Interface
INSERT INTO interface (id, name, description, created_by, created_at)
VALUES ('22222222-2222-2222-2222-222222222222', 'Main Interface', 'Test Interface', '11111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP);

-- User Interface Access
INSERT INTO user_interface_access (user_id, interface_id, role)
VALUES ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'ADMIN');

-- Devices
INSERT INTO device (id, interface_id, name, type, location, placement_date, is_active, last_value1, last_value2, last_updated, created_at)
VALUES 
('33333333-3333-3333-3333-333333333331', '22222222-2222-2222-2222-222222222222', 'Smart Bin 1', 'SMART_BIN', '28.6139,77.2090', CURRENT_DATE, true, 50.0, 0.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('33333333-3333-3333-3333-333333333332', '22222222-2222-2222-2222-222222222222', 'Smart Bin 2', 'SMART_BIN', '28.5355,77.3910', CURRENT_DATE, true, 75.0, 0.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', 'Smart Bin 3', 'SMART_BIN', '28.7041,77.1025', CURRENT_DATE, true, 20.0, 0.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
