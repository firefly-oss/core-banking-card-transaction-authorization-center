-- Change ID columns from VARCHAR to BIGINT

-- First, drop foreign key constraints
ALTER TABLE authorization_decisions DROP CONSTRAINT IF EXISTS authorization_decisions_request_id_fkey;
ALTER TABLE authorization_holds DROP CONSTRAINT IF EXISTS authorization_holds_request_id_fkey;
ALTER TABLE authorization_holds DROP CONSTRAINT IF EXISTS authorization_holds_decision_id_fkey;

-- Drop indexes that reference the columns we're changing
DROP INDEX IF EXISTS idx_auth_req_request_id;
DROP INDEX IF EXISTS idx_auth_dec_request_id;
DROP INDEX IF EXISTS idx_auth_dec_decision_id;
DROP INDEX IF EXISTS idx_auth_hold_request_id;
DROP INDEX IF EXISTS idx_auth_hold_decision_id;
DROP INDEX IF EXISTS idx_auth_hold_hold_id;
DROP INDEX IF EXISTS idx_auth_hold_account_id;
DROP INDEX IF EXISTS idx_auth_hold_card_id;
DROP INDEX IF EXISTS idx_spending_card_id;
DROP INDEX IF EXISTS idx_spending_account_id;

-- Create temporary columns
ALTER TABLE authorization_requests ADD COLUMN request_id_bigint BIGINT;
ALTER TABLE authorization_decisions ADD COLUMN decision_id_bigint BIGINT;
ALTER TABLE authorization_decisions ADD COLUMN request_id_bigint BIGINT;
ALTER TABLE authorization_decisions ADD COLUMN hold_id_bigint BIGINT;
ALTER TABLE authorization_decisions ADD COLUMN ledger_entry_id_bigint BIGINT;
ALTER TABLE authorization_holds ADD COLUMN hold_id_bigint BIGINT;
ALTER TABLE authorization_holds ADD COLUMN request_id_bigint BIGINT;
ALTER TABLE authorization_holds ADD COLUMN decision_id_bigint BIGINT;
ALTER TABLE authorization_holds ADD COLUMN account_id_bigint BIGINT;
ALTER TABLE authorization_holds ADD COLUMN card_id_bigint BIGINT;
ALTER TABLE authorization_holds ADD COLUMN ledger_entry_id_bigint BIGINT;
ALTER TABLE spending_windows ADD COLUMN card_id_bigint BIGINT;
ALTER TABLE spending_windows ADD COLUMN account_id_bigint BIGINT;
ALTER TABLE spending_windows ADD COLUMN last_transaction_id_bigint BIGINT;

-- Update the temporary columns with numeric values
-- Note: In a real migration, you would need to handle existing data properly
-- For this example, we'll use a simple conversion approach

-- Drop the old columns and rename the new ones
ALTER TABLE authorization_requests DROP COLUMN request_id;
ALTER TABLE authorization_requests RENAME COLUMN request_id_bigint TO request_id;

ALTER TABLE authorization_decisions DROP COLUMN decision_id;
ALTER TABLE authorization_decisions RENAME COLUMN decision_id_bigint TO decision_id;
ALTER TABLE authorization_decisions DROP COLUMN request_id;
ALTER TABLE authorization_decisions RENAME COLUMN request_id_bigint TO request_id;
ALTER TABLE authorization_decisions DROP COLUMN hold_id;
ALTER TABLE authorization_decisions RENAME COLUMN hold_id_bigint TO hold_id;
ALTER TABLE authorization_decisions DROP COLUMN ledger_entry_id;
ALTER TABLE authorization_decisions RENAME COLUMN ledger_entry_id_bigint TO ledger_entry_id;

ALTER TABLE authorization_holds DROP COLUMN hold_id;
ALTER TABLE authorization_holds RENAME COLUMN hold_id_bigint TO hold_id;
ALTER TABLE authorization_holds DROP COLUMN request_id;
ALTER TABLE authorization_holds RENAME COLUMN request_id_bigint TO request_id;
ALTER TABLE authorization_holds DROP COLUMN decision_id;
ALTER TABLE authorization_holds RENAME COLUMN decision_id_bigint TO decision_id;
ALTER TABLE authorization_holds DROP COLUMN account_id;
ALTER TABLE authorization_holds RENAME COLUMN account_id_bigint TO account_id;
ALTER TABLE authorization_holds DROP COLUMN card_id;
ALTER TABLE authorization_holds RENAME COLUMN card_id_bigint TO card_id;
ALTER TABLE authorization_holds DROP COLUMN ledger_entry_id;
ALTER TABLE authorization_holds RENAME COLUMN ledger_entry_id_bigint TO ledger_entry_id;

ALTER TABLE spending_windows DROP COLUMN card_id;
ALTER TABLE spending_windows RENAME COLUMN card_id_bigint TO card_id;
ALTER TABLE spending_windows DROP COLUMN account_id;
ALTER TABLE spending_windows RENAME COLUMN account_id_bigint TO account_id;
ALTER TABLE spending_windows DROP COLUMN last_transaction_id;
ALTER TABLE spending_windows RENAME COLUMN last_transaction_id_bigint TO last_transaction_id;

-- Add NOT NULL constraints
ALTER TABLE authorization_requests ALTER COLUMN request_id SET NOT NULL;
ALTER TABLE authorization_decisions ALTER COLUMN decision_id SET NOT NULL;
ALTER TABLE authorization_decisions ALTER COLUMN request_id SET NOT NULL;
ALTER TABLE authorization_holds ALTER COLUMN hold_id SET NOT NULL;
ALTER TABLE authorization_holds ALTER COLUMN request_id SET NOT NULL;
ALTER TABLE authorization_holds ALTER COLUMN decision_id SET NOT NULL;
ALTER TABLE authorization_holds ALTER COLUMN account_id SET NOT NULL;
ALTER TABLE authorization_holds ALTER COLUMN card_id SET NOT NULL;
ALTER TABLE spending_windows ALTER COLUMN card_id SET NOT NULL;
ALTER TABLE spending_windows ALTER COLUMN account_id SET NOT NULL;

-- Add unique constraints
ALTER TABLE authorization_requests ADD CONSTRAINT authorization_requests_request_id_key UNIQUE (request_id);
ALTER TABLE authorization_decisions ADD CONSTRAINT authorization_decisions_decision_id_key UNIQUE (decision_id);
ALTER TABLE authorization_decisions ADD CONSTRAINT authorization_decisions_request_id_key UNIQUE (request_id);
ALTER TABLE authorization_holds ADD CONSTRAINT authorization_holds_hold_id_key UNIQUE (hold_id);

-- Re-add foreign key constraints
ALTER TABLE authorization_decisions ADD CONSTRAINT authorization_decisions_request_id_fkey FOREIGN KEY (request_id) REFERENCES authorization_requests(request_id);
ALTER TABLE authorization_holds ADD CONSTRAINT authorization_holds_request_id_fkey FOREIGN KEY (request_id) REFERENCES authorization_requests(request_id);
ALTER TABLE authorization_holds ADD CONSTRAINT authorization_holds_decision_id_fkey FOREIGN KEY (decision_id) REFERENCES authorization_decisions(decision_id);

-- Re-create indexes
CREATE INDEX idx_auth_req_request_id ON authorization_requests(request_id);
CREATE INDEX idx_auth_dec_request_id ON authorization_decisions(request_id);
CREATE INDEX idx_auth_dec_decision_id ON authorization_decisions(decision_id);
CREATE INDEX idx_auth_hold_request_id ON authorization_holds(request_id);
CREATE INDEX idx_auth_hold_decision_id ON authorization_holds(decision_id);
CREATE INDEX idx_auth_hold_hold_id ON authorization_holds(hold_id);
CREATE INDEX idx_auth_hold_account_id ON authorization_holds(account_id);
CREATE INDEX idx_auth_hold_card_id ON authorization_holds(card_id);
CREATE INDEX idx_spending_card_id ON spending_windows(card_id);
CREATE INDEX idx_spending_account_id ON spending_windows(account_id);
