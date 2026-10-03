-- ============================================================================
-- Migration: V1.1__performance_indexes.sql
-- Description: Performance optimization indexes for high-frequency queries:
--              1. Support requests filtering and sorting (customer and admin)
--              2. Support request history timeline retrieval
--              3. User administration role filtering and temporal sorting
-- Safe for Execution: Idempotent (IF NOT EXISTS)
-- Rollback Instructions: See bottom of file
-- ============================================================================

-- 1. Support Requests
-- Optimizes customer queries: WHERE customer_id = ? ORDER BY created_at DESC
CREATE INDEX IF NOT EXISTS idx_support_requests_customer_created 
    ON support_requests (customer_id, created_at DESC);

-- Optimizes admin queries: WHERE status = ? ORDER BY created_at DESC
CREATE INDEX IF NOT EXISTS idx_support_requests_status_created 
    ON support_requests (status, created_at DESC);

-- Optimizes foreign key joins to support_categories
CREATE INDEX IF NOT EXISTS idx_support_requests_category_id 
    ON support_requests (category_id);

-- Optimizes admin staff assignment queries: WHERE assigned_to_id = ?
CREATE INDEX IF NOT EXISTS idx_support_requests_assigned_to 
    ON support_requests (assigned_to_id);

-- 2. Support Request Histories
-- Optimizes timeline loading: WHERE support_request_id = ? ORDER BY created_at ASC
CREATE INDEX IF NOT EXISTS idx_support_request_histories_request_created 
    ON support_request_histories (support_request_id, created_at ASC);

-- 3. Users Table
-- Optimizes foreign key lookup and role filtering in admin portal
CREATE INDEX IF NOT EXISTS idx_users_role_id 
    ON users (role_id);

-- Optimizes user listing sorted by creation date
CREATE INDEX IF NOT EXISTS idx_users_created_at 
    ON users (created_at DESC);

-- Optimizes phone search / verification lookup
CREATE INDEX IF NOT EXISTS idx_users_phone 
    ON users (phone);

-- ============================================================================
-- ROLLBACK SCRIPT (Run only if rollback is required in staging/production):
-- ============================================================================
-- DROP INDEX IF EXISTS idx_support_requests_customer_created;
-- DROP INDEX IF EXISTS idx_support_requests_status_created;
-- DROP INDEX IF EXISTS idx_support_requests_category_id;
-- DROP INDEX IF EXISTS idx_support_requests_assigned_to;
-- DROP INDEX IF EXISTS idx_support_request_histories_request_created;
-- DROP INDEX IF EXISTS idx_users_role_id;
-- DROP INDEX IF EXISTS idx_users_created_at;
-- DROP INDEX IF EXISTS idx_users_phone;
-- ============================================================================
