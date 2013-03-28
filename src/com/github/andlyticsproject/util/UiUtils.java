package com.github.andlyticsproject.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.preference.TwoStatePreference;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class UiUtils {

	private static final boolean IS_ICS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;

	private UiUtils() {
	}

	public static boolean isChecked(Preference pref) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return ((TwoStatePreference) pref).isChecked();
		}

		return ((CheckBoxPreference) pref).isChecked();
	}

	public static void setChecked(Preference pref, boolean checked) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			((TwoStatePreference) pref).setChecked(checked);
		} else {
			((CheckBoxPreference) pref).setChecked(checked);
		}
	}

	public static Preference createTwoStatePreference(Context ctx) {
		return IS_ICS ? new SwitchPreference(ctx) : new CheckBoxPreference(ctx);
	}
}
