package com.example.demo.util;

public class MaskingUtil {
	public static String maskAccountNumber(String accountNumber) {
		return "****" + accountNumber.substring(accountNumber.length()-4);
	}
}
