package com.example.demo.validation;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.example.demo.entity.BankAccount;
import com.example.demo.exception.ErrorMessage;
import com.example.demo.exception.InActiveBankAccountException;
import com.example.demo.exception.InsufficientBalanceException;
import com.example.demo.exception.NotVerifiedBankAccountException;

@Component
public class BankAccountValidator {
	
	public void validateForDebit(BankAccount bankAccount, BigDecimal amount) throws InActiveBankAccountException, NotVerifiedBankAccountException, InsufficientBalanceException {
		
		if (!bankAccount.isActive()) {
			throw new InActiveBankAccountException(ErrorMessage.INACTIVE_BANK_ACCOUNT);
		}
		
		if (!bankAccount.isVerified()) {
			throw new NotVerifiedBankAccountException(ErrorMessage.NOT_VERIFIED_BANK_ACCOUNT);
		}
		
		if (bankAccount.getBalance().compareTo(amount) < 0) {
			throw new InsufficientBalanceException(ErrorMessage.INSUFFICIENT_BALANCE);
		}
	}
	
	
}
