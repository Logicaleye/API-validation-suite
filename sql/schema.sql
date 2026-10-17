CREATE DATABASE IF NOT EXISTS apisuite_db;
USE apisuite_db;

DROP TABLE IF EXISTS users;
CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  role VARCHAR(30) NOT NULL DEFAULT 'user'
);

INSERT INTO users (name, email, role) VALUES
  ('Aarav Sharma',  'aarav@example.com',  'admin'),
  ('Diya Verma',    'diya@example.com',   'user'),
  ('Rohan Mehta',   'rohan@example.com',  'user'),
  ('Sneha Iyer',    'sneha@example.com',  'manager'),
  ('Kabir Singh',   'kabir@example.com',  'user'),
  ('Ananya Gupta',  'ananya@example.com', 'user');
