-- EcoRepairHub database (MySQL / MariaDB, e.g. XAMPP)
-- Import with:  mysql -u root < ecorepairhub.sql   (or via phpMyAdmin > Import)
-- The default admin (admin@ecorepair.com / admin123) is created by the app on startup.

CREATE DATABASE IF NOT EXISTS ecorepairhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ecorepairhub;

CREATE TABLE IF NOT EXISTS users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    phone      VARCHAR(255),
    status     VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME(6),
    CONSTRAINT chk_users_role   CHECK (role IN ('USER','COLLECTOR','ADMIN')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE','BLOCKED','SUSPENDED','DEACTIVATED'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS repair_requests (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    collector_id BIGINT NULL,
    address      VARCHAR(255) NOT NULL,
    pickup_date  DATE NOT NULL,
    time_slot    VARCHAR(255) NOT NULL,
    notes        VARCHAR(1000),
    pickup_otp   VARCHAR(4),
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at   DATETIME(6) NOT NULL,
    CONSTRAINT fk_req_user      FOREIGN KEY (user_id)      REFERENCES users(id),
    CONSTRAINT fk_req_collector FOREIGN KEY (collector_id) REFERENCES users(id),
    CONSTRAINT chk_req_status   CHECK (status IN ('PENDING','ASSIGNED','PICKED_UP','COLLECTED','COMPLETED','CANCELLED'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS request_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id  BIGINT NOT NULL,
    category    VARCHAR(255) NOT NULL,
    quantity    INT NOT NULL,
    description VARCHAR(255),
    CONSTRAINT fk_item_request FOREIGN KEY (request_id) REFERENCES repair_requests(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS certificates (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id    BIGINT NOT NULL UNIQUE,
    user_id       BIGINT NOT NULL,
    weight_kg     DOUBLE NOT NULL,
    co2_offset_kg DOUBLE NOT NULL,
    center_name   VARCHAR(255) NOT NULL,
    issued_date   DATE NOT NULL,
    CONSTRAINT fk_cert_request FOREIGN KEY (request_id) REFERENCES repair_requests(id),
    CONSTRAINT fk_cert_user    FOREIGN KEY (user_id)    REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS feedback (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    subject    VARCHAR(255) NOT NULL,
    message    VARCHAR(2000) NOT NULL,
    created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notifications (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    type       VARCHAR(255) NOT NULL,
    title      VARCHAR(255) NOT NULL,
    message    VARCHAR(1000) NOT NULL,
    is_read    BIT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS messages (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id    BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    content      VARCHAR(1000) NOT NULL,
    is_read      BIT NOT NULL DEFAULT 0,
    created_at   DATETIME(6) NOT NULL,
    CONSTRAINT fk_msg_sender    FOREIGN KEY (sender_id)    REFERENCES users(id),
    CONSTRAINT fk_msg_recipient FOREIGN KEY (recipient_id) REFERENCES users(id)
) ENGINE=InnoDB;
