package com.example.demo.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.enums.Role;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.service.SettlementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AutomatedSettlementJob {
	
	private final AppUserRepository appUserRepository;
    private final SettlementService settlementService;
    
    /**
     * Runs every day at 2:00 AM.
     * Cron Format: second, minute, hour, day of month, month, day of week
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void runDailySettlement() {
    	log.info("Starting Automated Daily Settlement Job...");
    	
    	List<AppUser> merchants = appUserRepository.findByRole(Role.MERCHANT);
    	
    	for (AppUser merchant : merchants) {
    		try {
    			settlementService.processSettlement(merchant);
    		} catch (Exception e) {
    			log.error("Failed to process settlement for merchant: {} - Error: {}", 
                        merchant.getEmail(), e.getMessage());
    		}
    	}
    	
    	log.info("Automated Settlement Job Completed.");
    	
    	
    }
}
