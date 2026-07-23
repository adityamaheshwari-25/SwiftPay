package com.example.demo.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.example.demo.dto.BankAccountResponseDto;
import com.example.demo.dto.CollectionStatsResponse;
import com.example.demo.dto.MerchantDashboardResponseDto;
import com.example.demo.dto.SecurityStatusResponseDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.dto.WalletResponseDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.Wallet;
import com.example.demo.exception.ErrorMessage;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.repository.WalletRepository;
import com.example.demo.security.CurrentUserService;
import com.example.demo.service.AppUserService;
import com.example.demo.service.BankAccountService;
import com.example.demo.service.MerchantService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService{
	
	private final WalletRepository walletRepository;
	private final CurrentUserService currentUserService;
	private final TransactionRepository transactionRepository;
	private final AppUserService appUserService;
	private final BankAccountService bankAccountService;
	
	@Override
	public CollectionStatsResponse getCollectionStats(Authentication auth) throws UserNotFoundException, ResourceNotFoundException {
		log.info("Calculating collection statistics for merchant: {}", auth.getName());
		
		Wallet wallet = getMerchantWallet(auth);
	    LocalDateTime now = LocalDateTime.now();

	    // --- Daily Logic ---
	    LocalDateTime startToday = now.toLocalDate().atStartOfDay();
	    LocalDateTime startYesterday = startToday.minusDays(1);
	    LocalDateTime endYesterday = startToday.minusNanos(1);

	    // --- Monthly Logic ---
	    LocalDateTime startThisMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
	    LocalDateTime startLastMonth = startThisMonth.minusMonths(1);
	    LocalDateTime endLastMonth = startThisMonth.minusNanos(1);

	    log.debug("Stats Date Ranges - Today: [{} to {}], Yesterday: [{} to {}]", startToday, now, startYesterday, endYesterday);
	    
	    // Fetch Totals from Repository
	    BigDecimal todayTotal = transactionRepository.sumReceivedByWalletInRange(wallet, startToday, now);
	    BigDecimal yesterdayTotal = transactionRepository.sumReceivedByWalletInRange(wallet, startYesterday, endYesterday);
	    
	    BigDecimal thisMonthTotal = transactionRepository.sumReceivedByWalletInRange(wallet, startThisMonth, now);
	    BigDecimal lastMonthTotal = transactionRepository.sumReceivedByWalletInRange(wallet, startLastMonth, endLastMonth);

	    // Calculate Growth Rates helper function
	    double dailyGrowth = calculateGrowth(todayTotal, yesterdayTotal);
	    double monthlyGrowth = calculateGrowth(thisMonthTotal, lastMonthTotal);

	    log.info("Stats calculated for Merchant ID {}: Today Total: {}, Monthly Total: {}", 
				wallet.getUser().getId(), todayTotal, thisMonthTotal);
	    
	    return CollectionStatsResponse.builder()
	            .today(todayTotal != null ? todayTotal : BigDecimal.ZERO)
	            .monthly(thisMonthTotal != null ? thisMonthTotal : BigDecimal.ZERO)
	            .pending(wallet.getBalance())
	            .dailyGrowthRate(dailyGrowth)
	            .monthlyGrowthRate(monthlyGrowth)
	            .build();
	}
	
	private Wallet getMerchantWallet(Authentication auth) throws UserNotFoundException, ResourceNotFoundException {
        // Implementation to find wallet by the authenticated merchant's ID
		AppUser user = currentUserService.getCurrentUser(auth);
		
        return walletRepository.findByUser(user)
                .orElseThrow(() -> {
                	log.error("Wallet lookup failed for merchant: {}", auth.getName());
                	return new ResourceNotFoundException(ErrorMessage.WALLET_NOT_FOUND);
                });
    }
	
	private double calculateGrowth(BigDecimal current, BigDecimal previous) {
	    if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
	    	log.debug("Previous period total was zero/null, returning growth as 100 or 0");
	    	return (current != null && current.compareTo(BigDecimal.ZERO) > 0) ? 100.0 : 0.0;
	    }
	    BigDecimal cur = (current != null) ? current : BigDecimal.ZERO;
	    return ((cur.subtract(previous)).doubleValue() / previous.doubleValue()) * 100;
	}

	@Override
	public MerchantDashboardResponseDto getMerchantDashboard(Authentication auth) throws UserNotFoundException, ResourceNotFoundException, UserNotFoundException {
		log.info("Generating full dashboard for merchant: {}", auth.getName());
		
		String email = auth.getName();
		
		CollectionStatsResponse stats = getCollectionStats(auth);
		
		UserResponseDto profile = appUserService.getMyProfile(auth);
		
		List<BankAccountResponseDto> bankAccounts = bankAccountService.getMyBankAccounts(email);
		
		SecurityStatusResponseDto security = appUserService.getSecurityStatus(auth);
		
		log.debug("Successfully aggregated profile, stats, bank accounts, and security status for merchant dashboard.");
		
		return MerchantDashboardResponseDto.builder()
				.profile(profile)
				.stats(stats)
				.security(security)
				.bankAccounts(bankAccounts)
				.build();
	}
	
}
