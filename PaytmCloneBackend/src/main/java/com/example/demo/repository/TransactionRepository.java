package com.example.demo.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.Transaction;
import com.example.demo.entity.Wallet;
import com.example.demo.entity.enums.TransactionStatus;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>{
	
	/*
	 * We use JOIN FETCH to load the wallets and user details in a single query. 
	 * This avoids the N+1 problem, where the app would otherwise run separate database queries 
	 * for every single user name in your list.
	 * */
	@Query("""
	        SELECT t FROM Transaction t
	        LEFT JOIN FETCH t.fromWallet fw
	        LEFT JOIN FETCH fw.user
	        LEFT JOIN FETCH t.toWallet tw
	        LEFT JOIN FETCH tw.user
	        WHERE (fw.user.id = :userId OR tw.user.id = :userId)
	    """)
	Page<Transaction> findUserTransactions(@Param("userId") Long userId, Pageable pageable);
	
	@Query("SELECT t FROM Transaction t WHERE t.toWallet.user.id = :userId")
    Page<Transaction> findMerchantTransactions(@Param("userId") Long userId, Pageable pageable);
	
	@Query("SELECT SUM(t.amount) FROM Transaction t " +
	           "WHERE t.fromWallet = :wallet " +
	           "AND t.status = :status " +
	           "AND t.createdAt BETWEEN :startDate AND :endDate")
	    BigDecimal sumSpendingByWalletInRange(
	            @Param("wallet") Wallet wallet, 
	            @Param("status") TransactionStatus status, 
	            @Param("startDate") LocalDateTime startDate,
	            @Param("endDate") LocalDateTime endDate
	    );
	
	
	// Sum of successful payments received by a merchant in a time range
    @Query("SELECT SUM(t.amount) FROM Transaction t " +
           "WHERE t.toWallet = :wallet " +
           "AND t.status = 'SUCCESS' " +
           "AND t.createdAt BETWEEN :start AND :end")
    BigDecimal sumReceivedByWalletInRange(
            @Param("wallet") Wallet wallet, 
            @Param("start") LocalDateTime start, 
            @Param("end") LocalDateTime end);
    
    
 // Fetch settlement history for a specific merchant wallet
    @Query("SELECT t FROM Transaction t WHERE t.fromWallet = :wallet " +
           "AND t.transactionType = 'SETTLEMENT' " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findSettlementsByWallet(@Param("wallet") Wallet wallet, Pageable pageable);
}
