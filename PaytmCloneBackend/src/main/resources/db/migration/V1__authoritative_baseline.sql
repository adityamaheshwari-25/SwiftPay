CREATE TABLE audit_logs (
    actor_user_id BIGINT,
    auditlog_id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6),
    action VARCHAR(255) NOT NULL,
    actor_role VARCHAR(255),
    endpoint VARCHAR(255),
    failure_reason TEXT,
    http_method VARCHAR(255),
    ip_address VARCHAR(255),
    outcome VARCHAR(255) NOT NULL,
    PRIMARY KEY (auditlog_id)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE bank_accounts (
    active BIT NOT NULL,
    balance DECIMAL(18,2) NOT NULL,
    is_primary BIT NOT NULL,
    verified BIT NOT NULL,
    bankaccount_id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6),
    fk_user_id BIGINT NOT NULL,
    account_number VARCHAR(255) NOT NULL,
    bank_name VARCHAR(255),
    ifsc VARCHAR(255),
    PRIMARY KEY (bankaccount_id)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE idempotency_keys (
    created_at DATETIME(6),
    expires_at DATETIME(6),
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT,
    api_name VARCHAR(255),
    idempotency_key VARCHAR(255),
    request_hash TEXT,
    response_body TEXT,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE kyc_document (
    file_size BIGINT,
    fk_user_id BIGINT,
    kycdocument_id BIGINT NOT NULL AUTO_INCREMENT,
    reviewed_at DATETIME(6),
    submitted_at DATETIME(6),
    file_path VARCHAR(512) NOT NULL,
    content_type VARCHAR(255),
    document_type VARCHAR(255),
    file_name VARCHAR(255),
    rejection_reason VARCHAR(255),
    status ENUM ('APPROVED', 'NOT_APPLIED', 'PENDING', 'REJECTED'),
    PRIMARY KEY (kycdocument_id)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE merchants (
    created_at DATETIME(6),
    fk_user_id BIGINT,
    merchant_id BIGINT NOT NULL AUTO_INCREMENT,
    business_name VARCHAR(255),
    category VARCHAR(255),
    merchant_code VARCHAR(255) NOT NULL,
    PRIMARY KEY (merchant_id)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE split_participants (
    share_amount DECIMAL(18,2) NOT NULL,
    paid_at DATETIME(6),
    paid_transaction_id BIGINT,
    participant_user_id BIGINT NOT NULL,
    split_participant_id BIGINT NOT NULL AUTO_INCREMENT,
    split_request_id BIGINT NOT NULL,
    version BIGINT,
    state ENUM ('DECLINED', 'EXPIRED', 'PAID', 'PENDING') NOT NULL,
    PRIMARY KEY (split_participant_id)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE split_requests (
    total_amount DECIMAL(18,2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    initiator_user_id BIGINT NOT NULL,
    split_request_id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    split_code VARCHAR(64) NOT NULL,
    note VARCHAR(255),
    split_type ENUM ('CUSTOM', 'EQUAL') NOT NULL,
    status ENUM ('CANCELLED', 'COMPLETE', 'EXPIRED', 'OPEN', 'PARTIALLY_PAID') NOT NULL,
    PRIMARY KEY (split_request_id)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE transactions (
    amount DECIMAL(18,2) NOT NULL,
    created_at DATETIME(6),
    from_bank_account_id BIGINT,
    from_wallet_id BIGINT,
    to_bank_account_id BIGINT,
    to_wallet_id BIGINT,
    transaction_id BIGINT NOT NULL AUTO_INCREMENT,
    narration VARCHAR(255),
    reference_id VARCHAR(255) NOT NULL,
    tx_id VARCHAR(255) NOT NULL,
    payment_mode ENUM ('CREDIT_CARD', 'DEBIT_CARD', 'NETBANKING', 'UPI', 'WALLET') NOT NULL,
    status ENUM ('FAILED', 'PENDING', 'SUCCESS') NOT NULL,
    transaction_type ENUM ('ADD_MONEY', 'REFUND', 'SETTLEMENT', 'SPLIT', 'WALLET_TRANSFER', 'WITHDRAW'),
    PRIMARY KEY (transaction_id)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE users (
    active BIT NOT NULL,
    kyc_verified BIT NOT NULL,
    mpin_set BIT NOT NULL,
    created_at DATETIME(6),
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    mobile VARCHAR(255) NOT NULL,
    mpin_hash VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM ('ADMIN', 'MERCHANT', 'SUPER_ADMIN', 'USER'),
    PRIMARY KEY (user_id)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE wallets (
    balance DECIMAL(18,2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    fk_user_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT,
    wallet_id BIGINT NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (wallet_id)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

ALTER TABLE idempotency_keys
    ADD CONSTRAINT uq_idempotency_keys_user_api_key UNIQUE (user_id, api_name, idempotency_key);
ALTER TABLE kyc_document
    ADD CONSTRAINT uq_kyc_document_user UNIQUE (fk_user_id);
ALTER TABLE merchants
    ADD CONSTRAINT uq_merchants_user UNIQUE (fk_user_id);
ALTER TABLE merchants
    ADD CONSTRAINT uq_merchants_code UNIQUE (merchant_code);
ALTER TABLE split_participants
    ADD CONSTRAINT uq_split_participants_request_user UNIQUE (split_request_id, participant_user_id);
ALTER TABLE split_participants
    ADD CONSTRAINT uq_split_participants_paid_transaction UNIQUE (paid_transaction_id);
ALTER TABLE split_requests
    ADD CONSTRAINT uq_split_requests_code UNIQUE (split_code);
ALTER TABLE transactions
    ADD CONSTRAINT uq_transactions_tx_id UNIQUE (tx_id);
ALTER TABLE users
    ADD CONSTRAINT uq_users_email UNIQUE (email);
ALTER TABLE users
    ADD CONSTRAINT uq_users_mobile UNIQUE (mobile);
ALTER TABLE wallets
    ADD CONSTRAINT uq_wallets_user UNIQUE (fk_user_id);

ALTER TABLE bank_accounts
    ADD CONSTRAINT fk_bank_accounts_user FOREIGN KEY (fk_user_id) REFERENCES users (user_id);
ALTER TABLE kyc_document
    ADD CONSTRAINT fk_kyc_document_user FOREIGN KEY (fk_user_id) REFERENCES users (user_id);
ALTER TABLE merchants
    ADD CONSTRAINT fk_merchants_user FOREIGN KEY (fk_user_id) REFERENCES users (user_id);
ALTER TABLE split_participants
    ADD CONSTRAINT fk_split_participants_paid_transaction FOREIGN KEY (paid_transaction_id) REFERENCES transactions (transaction_id);
ALTER TABLE split_participants
    ADD CONSTRAINT fk_split_participants_user FOREIGN KEY (participant_user_id) REFERENCES users (user_id);
ALTER TABLE split_participants
    ADD CONSTRAINT fk_split_participants_request FOREIGN KEY (split_request_id) REFERENCES split_requests (split_request_id);
ALTER TABLE split_requests
    ADD CONSTRAINT fk_split_requests_initiator FOREIGN KEY (initiator_user_id) REFERENCES users (user_id);
ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_from_bank FOREIGN KEY (from_bank_account_id) REFERENCES bank_accounts (bankaccount_id);
ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_from_wallet FOREIGN KEY (from_wallet_id) REFERENCES wallets (wallet_id);
ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_to_bank FOREIGN KEY (to_bank_account_id) REFERENCES bank_accounts (bankaccount_id);
ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_to_wallet FOREIGN KEY (to_wallet_id) REFERENCES wallets (wallet_id);
ALTER TABLE wallets
    ADD CONSTRAINT fk_wallets_user FOREIGN KEY (fk_user_id) REFERENCES users (user_id);
