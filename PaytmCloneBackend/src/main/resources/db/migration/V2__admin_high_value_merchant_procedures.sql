DELIMITER $$

DROP PROCEDURE IF EXISTS sp_admin_high_value_merchants_summary$$
CREATE PROCEDURE sp_admin_high_value_merchants_summary(
    IN p_min_amount DECIMAL(18,2),
    IN p_q VARCHAR(255),
    IN p_limit INT,
    IN p_offset INT
)
BEGIN
    SET p_q = NULLIF(TRIM(p_q), '');

    SELECT
        m.merchant_id AS merchantId,
        m.merchant_code AS merchantCode,
        m.business_name AS businessName,
        m.category AS category,
        COUNT(t.transaction_id) AS highValueTxnCount,
        SUM(t.amount) AS totalHighValueAmount,
        COUNT(DISTINCT payer.user_id) AS distinctPayers
    FROM merchants m
    JOIN users mu       ON mu.user_id = m.fk_user_id
    JOIN wallets mw     ON mw.fk_user_id = mu.user_id
    JOIN transactions t ON t.to_wallet_id = mw.wallet_id
    LEFT JOIN wallets pw  ON pw.wallet_id = t.from_wallet_id
    LEFT JOIN users payer ON payer.user_id = pw.fk_user_id
    WHERE t.amount > p_min_amount
      AND (p_q IS NULL OR m.business_name LIKE CONCAT('%', p_q, '%'))
    GROUP BY m.merchant_id, m.merchant_code, m.business_name, m.category
    ORDER BY totalHighValueAmount DESC
    LIMIT p_limit OFFSET p_offset;
END$$

DROP PROCEDURE IF EXISTS sp_admin_high_value_merchants_count$$
CREATE PROCEDURE sp_admin_high_value_merchants_count(
    IN p_min_amount DECIMAL(18,2),
    IN p_q VARCHAR(255)
)
BEGIN
    SET p_q = NULLIF(TRIM(p_q), '');

    SELECT COUNT(*) AS totalMerchants
    FROM (
        SELECT m.merchant_id
        FROM merchants m
        JOIN users mu       ON mu.user_id = m.fk_user_id
        JOIN wallets mw     ON mw.fk_user_id = mu.user_id
        JOIN transactions t ON t.to_wallet_id = mw.wallet_id
        WHERE t.amount > p_min_amount
          AND (p_q IS NULL OR m.business_name LIKE CONCAT('%', p_q, '%'))
        GROUP BY m.merchant_id
    ) x;
END$$

DROP PROCEDURE IF EXISTS sp_admin_merchant_high_value_txns_by_merchant$$
CREATE PROCEDURE sp_admin_merchant_high_value_txns_by_merchant(
    IN p_merchant_id BIGINT,
    IN p_min_amount DECIMAL(18,2),
    IN p_limit INT,
    IN p_offset INT
)
BEGIN
    SELECT
        m.merchant_id AS merchantId,
        m.merchant_code AS merchantCode,
        m.business_name AS businessName,
        m.category AS category,
        payer.user_id AS payerUserId,
        payer.name AS payerName,
        payer.email AS payerEmail,
        payer.mobile AS payerMobile,
        t.transaction_id AS transactionDbId,
        t.tx_id AS txId,
        t.reference_id AS referenceId,
        t.transaction_type AS transactionType,
        t.payment_mode AS paymentMode,
        t.amount AS amount,
        t.status AS status,
        t.created_at AS createdAt
    FROM merchants m
    JOIN users mu       ON mu.user_id = m.fk_user_id
    JOIN wallets mw     ON mw.fk_user_id = mu.user_id
    JOIN transactions t ON t.to_wallet_id = mw.wallet_id
    LEFT JOIN wallets pw  ON pw.wallet_id = t.from_wallet_id
    LEFT JOIN users payer ON payer.user_id = pw.fk_user_id
    WHERE m.merchant_id = p_merchant_id
      AND t.amount > p_min_amount
    ORDER BY t.created_at DESC
    LIMIT p_limit OFFSET p_offset;
END$$

DELIMITER ;
