package com.github.andlyticsproject.model;

import java.util.Arrays;
import java.util.List;

import android.annotation.SuppressLint;


public class Revenue {

	private static final String DECIMALS_CURRENCY_FORMAT = "%.2f";
	private static final String NO_DECIMALS_FORMAT = "%.0f";
	private static final String DECIMAL_CURRENCY_FORMAT = "%.2f %s";
	private static final String NO_DECIMALS_CURRENCY_FORMAT = "%.0f %s";
	private static final int DECIMAL_DISPLAY_THRESHOLD = 1000;

	public enum Type {
		TOTAL, APP_SALES, IN_APP, SUBSCRIPTIONS
	}

	// TODO add others
	public static final String[] NO_DECIMAL_CURRENCIES_ARR = { "JPY" };
	public static final List<String> NO_DECIMAL_CURRENCIES = Arrays
			.asList(NO_DECIMAL_CURRENCIES_ARR);

	private Type type;
	private String currency;
	private double amount;

	public Revenue(Type type, double amount, String currency) {
		this.type = type;
		this.amount = amount;
		this.currency = currency;
	}

	public Type getType() {
		return type;
	}

	public String getCurrency() {
		return currency;
	}

	public double getAmount() {
		return amount;
	}

	@SuppressLint("DefaultLocale")
	public String asString() {
		if (NO_DECIMAL_CURRENCIES.contains(currency)) {
			return String.format(NO_DECIMALS_CURRENCY_FORMAT, amount, currency);
		}

		return String.format((amount < DECIMAL_DISPLAY_THRESHOLD ? DECIMAL_CURRENCY_FORMAT
				: NO_DECIMALS_CURRENCY_FORMAT), amount, currency);
	}

	@SuppressLint("DefaultLocale")
	public String amountAsString() {
		if (NO_DECIMAL_CURRENCIES.contains(currency)) {
			return String.format(NO_DECIMALS_FORMAT, amount);
		}

		return String.format((amount < DECIMAL_DISPLAY_THRESHOLD ? DECIMALS_CURRENCY_FORMAT
				: NO_DECIMALS_FORMAT), amount);
	}

	@Override
	public String toString() {
		return asString();
	}

}
