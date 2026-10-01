-- V2: Add status column to users table
-- Allows soft-delete (ACTIVE/INACTIVE) for admin user management

ALTER TABLE users 
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

COMMENT ON COLUMN users.status IS 'User account status: ACTIVE or INACTIVE (soft delete)';