-- Add account_space_id column to relevant tables

-- Add to authorization_decisions table
ALTER TABLE authorization_decisions
ADD COLUMN account_space_id BIGINT;

-- Add to authorization_holds table
ALTER TABLE authorization_holds
ADD COLUMN account_space_id BIGINT;

-- Add to spending_windows table
ALTER TABLE spending_windows
ADD COLUMN account_space_id BIGINT;

-- Create indexes for the new columns
CREATE INDEX idx_auth_dec_account_space_id ON authorization_decisions(account_space_id);
CREATE INDEX idx_auth_hold_account_space_id ON authorization_holds(account_space_id);
CREATE INDEX idx_spending_account_space_id ON spending_windows(account_space_id);
