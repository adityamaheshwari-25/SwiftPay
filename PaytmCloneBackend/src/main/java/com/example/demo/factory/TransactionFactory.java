package com.example.demo.factory;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.example.demo.entity.BankAccount;
import com.example.demo.entity.Transaction;
import com.example.demo.entity.Wallet;
import com.example.demo.entity.enums.PaymentMode;
import com.example.demo.entity.enums.TransactionStatus;
import com.example.demo.entity.enums.TransactionType;
import com.example.demo.util.IdGenerator;

@Component
public class TransactionFactory {
	
	public Transaction createAddMoneyTransaction(
				BankAccount fromBank,
				Wallet toWallet,
				BigDecimal amount,
				PaymentMode paymentMode) {
		
		Transaction tx = new Transaction();
		tx.setTxId(IdGenerator.generateTxId());
		tx.setReferenceId(IdGenerator.generateReferenceId());
		tx.setTransactionType(TransactionType.ADD_MONEY);
		tx.setPaymentMode(paymentMode);
		tx.setAmount(amount);
		tx.setFromBankAccount(fromBank);
		tx.setToWallet(toWallet);
		
		tx.setStatus(TransactionStatus.SUCCESS);
	    tx.setNarration("Money added from bank");
		
		return tx;
	}
	
    // Later:
    // createWalletTransferTransaction()
	
	public Transaction createWithdrawTransaction(
				Wallet fromWallet,
				BankAccount toBank,
				BigDecimal amount
			) {
		Transaction tx = new Transaction();
		tx.setTxId(IdGenerator.generateTxId());
		tx.setReferenceId(IdGenerator.generateReferenceId());
		tx.setTransactionType(TransactionType.WITHDRAW);
		tx.setPaymentMode(PaymentMode.WALLET);
		tx.setAmount(amount);
		tx.setFromWallet(fromWallet);
		tx.setToBankAccount(toBank);
		
		tx.setStatus(TransactionStatus.SUCCESS);
	    tx.setNarration("Withdrawn to bank");
		
		return tx;
	}
	
	public Transaction createWalletTransferTransaction(
            Wallet fromWallet,
            Wallet toWallet,
            BigDecimal amount
    ) {
        Transaction tx = new Transaction();
        tx.setTxId(IdGenerator.generateTxId());
        tx.setReferenceId(IdGenerator.generateReferenceId());
        tx.setTransactionType(TransactionType.WALLET_TRANSFER);
        tx.setPaymentMode(PaymentMode.WALLET);
        tx.setAmount(amount);
        tx.setFromWallet(fromWallet);
        tx.setToWallet(toWallet);
        
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setNarration("Wallet transfer");

        return tx;
    }
	
	public Transaction createSettlementTransaction(
            Wallet fromWallet,
            BankAccount toBank,
            BigDecimal amount
	    ) {
	    Transaction tx = new Transaction();
	    tx.setTxId("SETL-" + IdGenerator.generateTxId()); // Prefix for easy tracking
	    tx.setReferenceId(IdGenerator.generateReferenceId());
	    tx.setTransactionType(TransactionType.SETTLEMENT);
	    tx.setPaymentMode(PaymentMode.WALLET); // Settlements are internal wallet-to-bank moves
	    tx.setAmount(amount);
	    tx.setFromWallet(fromWallet);
	    tx.setToBankAccount(toBank);
	    
	    tx.setStatus(TransactionStatus.SUCCESS);
	    tx.setNarration("Daily Automated Settlement to Primary Bank");
	
	    return tx;
	}
	
	public Transaction createSplitPaymentTransaction(
				Wallet fromWallet,
				Wallet toWallet,
				BigDecimal amount,
				String splitReferenceId // recommended: "SPLIT:<splitRequestId>"
			) {
		Transaction tx = new Transaction();
        tx.setTxId(IdGenerator.generateTxId());
        tx.setReferenceId(splitReferenceId);
        tx.setTransactionType(TransactionType.SPLIT);
        tx.setPaymentMode(PaymentMode.WALLET);
        tx.setAmount(amount);
        tx.setFromWallet(fromWallet);
        tx.setToWallet(toWallet);

        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setNarration("Split payment");

        return tx;
	}
	
	
	
}
