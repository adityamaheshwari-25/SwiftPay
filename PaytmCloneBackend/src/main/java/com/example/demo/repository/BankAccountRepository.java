package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.BankAccount;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;


@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long>{
	List<BankAccount> findByUser(AppUser user);
	Optional<BankAccount> findByIdAndUser(Long id, AppUser user);
	
	// Check if user has any accounts
    boolean existsByUserAndActiveTrue(AppUser user);
    
 // Find the primary account for settlement
    Optional<BankAccount> findByUserAndIsPrimaryTrueAndActiveTrue(AppUser user);
    
    @Modifying
    @Transactional
    @Query("UPDATE BankAccount b SET b.isPrimary = false WHERE b.user = :user")
    void markAllAsNonPrimary(@Param("user") AppUser user);
    
    @Query("SELECT b FROM BankAccount b WHERE b.user.id = :userId AND b.isPrimary = true")
    Optional<BankAccount> findPrimaryByUserId(@Param("userId") Long userId);
}
