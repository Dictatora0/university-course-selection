USE course_selection;

DROP TABLE IF EXISTS Administrator;

CREATE TABLE Administrator (
    admin_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL, -- Should be hashed in a real application
    role VARCHAR(50) NOT NULL,       -- e.g., SUPER_ADMIN, COURSE_ADMIN
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL DEFAULT NULL
);

SELECT 'Administrator table created successfully' AS 'Execution Result'; 