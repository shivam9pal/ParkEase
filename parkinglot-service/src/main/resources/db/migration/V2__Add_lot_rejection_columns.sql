-- V2__Add_lot_rejection_columns.sql
-- Migration to add approval status and rejection tracking columns to parking_lot table

ALTER TABLE parking_lot 
ADD COLUMN IF NOT EXISTS approval_status VARCHAR(50) DEFAULT 'PENDING' NOT NULL;

ALTER TABLE parking_lot 
ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(1000);

ALTER TABLE parking_lot 
ADD COLUMN IF NOT EXISTS rejection_date TIMESTAMP;

-- Create index for approval_status queries
CREATE INDEX IF NOT EXISTS idx_parking_lot_approval_status 
ON parking_lot(approval_status);
