package com.example.demo.validation;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.example.demo.entity.Wallet;
import com.example.demo.exception.ErrorMessage;
import com.example.demo.exception.InsufficientBalanceException;

@Component
public class WalletAmountValidator {

    public void validateDebit(Wallet wallet, BigDecimal amount) throws InsufficientBalanceException {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(ErrorMessage.INSUFFICIENT_BALANCE);
        }
    }
}
