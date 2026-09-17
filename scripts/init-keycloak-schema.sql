-- Tạo schema riêng biệt cho Keycloak để tránh xung đột với các bảng của nghiệp vụ CGV
CREATE SCHEMA IF NOT EXISTS keycloak;
