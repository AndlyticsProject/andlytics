package com.github.andlyticsproject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.andlyticsproject.db.AndlyticsDb;
import com.github.andlyticsproject.model.DeveloperAccount;
import com.github.andlyticsproject.sync.AutosyncHandler;

/**
 * Used for initial login and managing accounts Because of this original legacy as the launcher
 * activity, navigation is a little odd.
 * On first startup: LoginActivity -> Main
 * When managing
 * accounts: Main -> LoginActivity <- Main
 * or
 * Main -> LoginActivity -> Main
 */
public class LoginActivity extends SherlockActivity {

	private static final String TAG = LoginActivity.class.getSimpleName();

	protected static final int CREATE_ACCOUNT_REQUEST = 1;

	private List<DeveloperAccount> developerAccounts;

	private boolean manageAccountsMode = false;
	private boolean blockGoingBack = false;
	private DeveloperAccount selectedAccount = null;
	private View okButton;
	private LinearLayout accountList;

	private AccountManager accountManager;
	private AndlyticsDb andlyticsDb;

	// TODO Clean this code and res/layout/login.xml up e.g. using a ListView
	// instead of a LinearLayout
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		accountManager = AccountManager.get(this);
		andlyticsDb = AndlyticsDb.getInstance(getApplicationContext());

		// When called from accounts action item in Main, this flag is passed to
		// indicate
		// that LoginActivity should not auto login as we are managing the
		// accounts,
		// rather than performing the initial login
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			manageAccountsMode = extras.getBoolean(Constants.MANAGE_ACCOUNTS_MODE);
		}

		if (manageAccountsMode) {
			getSupportActionBar().setTitle(R.string.manage_accounts);
		}

		selectedAccount = andlyticsDb.getSelectedDeveloperAccount();

		setContentView(R.layout.login);
		accountList = (LinearLayout) findViewById(R.id.login_input);

		okButton = findViewById(R.id.login_ok_button);
		okButton.setClickable(true);
		okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (selectedAccount != null) {
					redirectToMain(selectedAccount.getName());
				} else {
					// Go to the first non hidden account
					for (DeveloperAccount account : developerAccounts) {
						if (!account.isHidden()) {
							redirectToMain(account.getName());
							break;
						}
					}
				}

			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		boolean skipAutologin = Preferences.getSkipAutologin(this);

		if (!manageAccountsMode & !skipAutologin & selectedAccount != null) {
			redirectToMain(selectedAccount.getName());
		} else {
			showAccountList();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getSupportMenuInflater().inflate(R.menu.login_menu, menu);
		return true;
	}

	/**
	 * Called if item in option menu is selected.
	 * 
	 * @param item
	 * The chosen menu item
	 * @return boolean true/false
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.itemLoginmenuAdd:
			addNewGoogleAccount();
			break;
		case android.R.id.home:
			if (!blockGoingBack) {
				setResult(RESULT_OK);
				finish();
			}
			break;
		default:
			return false;
		}
		return true;
	}

	@Override
	public void onBackPressed() {
		setResult(blockGoingBack ? RESULT_CANCELED : RESULT_OK);
		super.onBackPressed();
	}

	protected void showAccountList() {
		Account[] googleAccounts = accountManager.getAccountsByType(Constants.ACCOUNT_TYPE_GOOGLE);
		developerAccounts = new ArrayList<DeveloperAccount>();

		accountList.removeAllViews();
		for (int i = 0; i < googleAccounts.length; i++) {
			DeveloperAccount developerAccount = andlyticsDb
					.findDeveloperAccountByName(googleAccounts[i].name);
			if (developerAccount == null) {
				developerAccount = new DeveloperAccount(googleAccounts[i].name,
						DeveloperAccount.State.HIDDEN);
			}
			developerAccounts.add(developerAccount);


			// Setup auto sync
			final AutosyncHandler syncHandler = new AutosyncHandler();
			// only do this when managing accounts, otherwise sync may start
			// in the background before accounts are actually configured
			if (manageAccountsMode) {
				// Ensure it matches the sync period (excluding disabled state)
				syncHandler.setAutosyncPeriod(googleAccounts[i].name,
						Preferences.getLastNonZeroAutosyncPeriod(this));
				// Now make it match the master sync (including disabled state)
				syncHandler.setAutosyncPeriod(googleAccounts[i].name,
						Preferences.getAutosyncPeriod(this));
			}

			View accountItem = getLayoutInflater().inflate(R.layout.login_list_item, null);
			TextView accountName = (TextView) accountItem.findViewById(R.id.login_list_item_text);
			accountName.setText(googleAccounts[i].name);
			accountItem.setTag(developerAccount);
			CheckBox enabled = (CheckBox) accountItem.findViewById(R.id.login_list_item_enabled);
			enabled.setChecked(!developerAccount.isHidden());
			enabled.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					DeveloperAccount account = (DeveloperAccount) ((View) buttonView.getParent())
							.getTag();
					if (isChecked) {
						account.activate();
					} else {
						account.hide();
					}

					if (account.isHidden()) {
						// They are removing the account from Andlytics, disable
						// syncing
						syncHandler.setAutosyncEnabled(account.getName(), false);
					} else {
						// Make it match the master sync period (including
						// disabled state)
						syncHandler.setAutosyncPeriod(account.getName(),
								Preferences.getAutosyncPeriod(LoginActivity.this));
					}
					andlyticsDb.addOrUpdateDeveloperAccount(account);

					if (manageAccountsMode && (account.getName()).equals(selectedAccount)) {
						// If they remove the current account, then stop them
						// going back
						blockGoingBack = account.isHidden();
					}

					okButton.setEnabled(isAtLeastOneAccountEnabled());
				}
			});
			accountList.addView(accountItem);
		}

		// Update ok button
		okButton.setEnabled(isAtLeastOneAccountEnabled());
	}

	private boolean isAtLeastOneAccountEnabled() {
		for (DeveloperAccount acc : developerAccounts) {
			if (!acc.isHidden()) {
				return true;
			}
		}

		return false;
	}

	private void addNewGoogleAccount() {
		AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
			public void run(AccountManagerFuture<Bundle> future) {
				try {
					Bundle bundle = future.getResult();
					bundle.keySet();
					Log.d(TAG, "account added: " + bundle);

					showAccountList();

				} catch (OperationCanceledException e) {
					Log.d(TAG, "addAccount was canceled");
				} catch (IOException e) {
					Log.d(TAG, "addAccount failed: " + e);
				} catch (AuthenticatorException e) {
					Log.d(TAG, "addAccount failed: " + e);
				}
				// gotAccount(false);
			}
		};

		accountManager.addAccount(Constants.ACCOUNT_TYPE_GOOGLE,
				Constants.AUTH_TOKEN_TYPE_ANDROID_DEVELOPER, null, null /* options */,
				LoginActivity.this, callback, null /* handler */);
	}

	private void redirectToMain(String selectedAccount) {
		Preferences.saveSkipAutoLogin(this, false);
		Intent intent = new Intent(LoginActivity.this, Main.class);
		intent.putExtra(Constants.AUTH_ACCOUNT_NAME, selectedAccount);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_out);
		finish();
	}

}
