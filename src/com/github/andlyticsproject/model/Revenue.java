package com.github.andlyticsproject.model;

import java.util.Arrays;
import java.util.Currency;
import java.util.List;

import android.annotation.SuppressLint;

public class Revenue {

	private static final String DECIMALS_FORMAT = "%.2f";
	private static final String NO_DECIMALS_FORMAT = "%.0f";
	private static final String DECIMAL_CURRENCY_FORMAT = "%s%.2f";
	private static final String NO_DECIMALS_CURRENCY_FORMAT = "%s%.0f";
	private static final int DECIMAL_DISPLAY_THRESHOLD = 1000;

	private static final double DEVELOPER_CUT_PERCENTAGE = 0.7;

	public enum Type {
		TOTAL, APP_SALES, IN_APP, SUBSCRIPTIONS
	}

	// TODO add others
	public static final String[] NO_DECIMAL_CURRENCIES_ARR = { "JPY" };
	public static final List<String> NO_DECIMAL_CURRENCIES = Arrays
			.asList(NO_DECIMAL_CURRENCIES_ARR);

	private Type type;
	private String currencyCode;
	private Currency currency;
	private double amount;
	private double developerCut;

	public Revenue(Type type, double amount, String currencyCode) {
		this.type = type;
		this.amount = amount;
		// XXX make this smarter, round up,etc.
		this.developerCut = DEVELOPER_CUT_PERCENTAGE * amount;
		this.currencyCode = currencyCode;
		this.currency = Currency.getInstance(currencyCode);
	}

	public Type getType() {
		return type;
	}

	public String getCurrencyCode() {
		return currencyCode;
	}

	public Currency getCurrency() {
		return currency;
	}

	public double getAmount() {
		return amount;
	}

	public double getDeveloperCut() {
		return developerCut;
	}

	@SuppressLint("DefaultLocale")
	public String asString() {
		if (NO_DECIMAL_CURRENCIES.contains(currencyCode)) {
			return String.format(NO_DECIMALS_CURRENCY_FORMAT, currency.getSymbol(), amount);
		}

		return String.format((amount < DECIMAL_DISPLAY_THRESHOLD ? DECIMAL_CURRENCY_FORMAT
				: NO_DECIMALS_CURRENCY_FORMAT), currency.getSymbol(), amount);
	}

	@SuppressLint("DefaultLocale")
	public String amountAsString() {
		if (NO_DECIMAL_CURRENCIES.contains(currencyCode)) {
			return String.format(NO_DECIMALS_FORMAT, amount);
		}

		return String.format((amount < DECIMAL_DISPLAY_THRESHOLD ? DECIMALS_FORMAT
				: NO_DECIMALS_FORMAT), amount);
	}

	@SuppressLint("DefaultLocale")
	public String developerCutAsString() {
		if (NO_DECIMAL_CURRENCIES.contains(currencyCode)) {
			return String.format(NO_DECIMALS_CURRENCY_FORMAT, currency.getSymbol(), developerCut);
		}

		return String.format((amount < DECIMAL_DISPLAY_THRESHOLD ? DECIMAL_CURRENCY_FORMAT
				: DECIMAL_CURRENCY_FORMAT), currency.getSymbol(), developerCut);
	}

	@Override
	public String toString() {
		return asString();
	}

}
