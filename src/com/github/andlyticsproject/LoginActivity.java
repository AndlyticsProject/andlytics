
package com.github.andlyticsproject;

import java.io.IOException;

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
import com.github.andlyticsproject.sync.AutosyncHandler;
import com.github.andlyticsproject.sync.AutosyncHandlerFactory;

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

	private static final String TAG = "Andlytics";
	protected static final int CREATE_ACCOUNT_REQUEST = 1;
	
	private AccountStatus[] accountStatuses;

	private boolean manageAccountsMode = false;
	private boolean blockGoingBack = false;
	private String selectedAccount = null;
	private View okButton;
	private LinearLayout accountList;

	// TODO Clean this code and res/layout/login.xml up e.g. using a ListView instead of a LinearLayout
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// When called from accounts action item in Main, this flag is passed to indicate
		// that LoginActivity should not auto login as we are managing the accounts,
		// rather than performing the initial login
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			manageAccountsMode = extras.getBoolean(Constants.MANAGE_ACCOUNTS_MODE);
		}

		if (manageAccountsMode) {
			getSupportActionBar().setTitle(R.string.manage_accounts);
		}

		selectedAccount = Preferences.getAccountName(this);

		setContentView(R.layout.login);
		accountList = (LinearLayout) findViewById(R.id.login_input);
		
		
		okButton = findViewById(R.id.login_ok_button);
		okButton.setClickable(true);
		okButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				if (selectedAccount != null) {
					redirectToMain(selectedAccount);
				} else {
					// Go to the first non hidden account
					for (AccountStatus account : accountStatuses) {
						if (!account.hidden) {
							redirectToMain(account.name);
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
			redirectToMain(selectedAccount);
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
	 * @param item The chosen menu item
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
		final AccountManager manager = AccountManager.get(this);
		final Account[] accounts = manager.getAccountsByType(Constants.ACCOUNT_TYPE_GOOGLE);
		final int size = accounts.length;
		accountStatuses = new AccountStatus[size];
		accountList.removeAllViews();
		for (int i = 0; i < size; i++) {
			accountStatuses[i] = new AccountStatus();
			accountStatuses[i].name = accounts[i].name;
			Boolean hiddenAccount = Preferences.getIsHiddenAccount(this, accountStatuses[i].name);
			accountStatuses[i].hidden = hiddenAccount;
			View inflate = getLayoutInflater().inflate(R.layout.login_list_item, null);
			TextView accountName = (TextView) inflate.findViewById(R.id.login_list_item_text);
			accountName.setText(accounts[i].name);
			inflate.setTag(accountStatuses[i]);
			CheckBox enabled = (CheckBox) inflate.findViewById(R.id.login_list_item_enabled);
			enabled.setChecked(!hiddenAccount);
			enabled.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					AccountStatus account = (AccountStatus) ((View) buttonView.getParent()).getTag();
					Preferences.saveIsHiddenAccount(getApplicationContext(), account.name,
							!isChecked);
					// Enable disable sync
					AutosyncHandler syncHandler = AutosyncHandlerFactory
							.getInstance(getApplicationContext());
					account.hidden = !isChecked;
					if (!isChecked) {
						syncHandler.setAutosyncPeriod(account.name, 0);
					} else {
						// If auto sync was on for the account, enable it again
						syncHandler.setAutosyncPeriod(account.name,
								Preferences.isAutoSyncEnabled(LoginActivity.this, account.name) ? Preferences
										.getAutoSyncPeriod(LoginActivity.this) : 0);
					}

					if (manageAccountsMode && (account.name).equals(selectedAccount)) {
						// If they remove the current account, then stop them going back
						blockGoingBack = !isChecked;
					}
					
					// Update ok button
					boolean atLeastOneAccountEnabled = false;
					for (AccountStatus acc : accountStatuses) {
						if (!acc.hidden) {
							atLeastOneAccountEnabled = true;
							break;
						}
					}
					okButton.setEnabled(atLeastOneAccountEnabled);
				}
			});
			accountList.addView(inflate);
		}
		
		// Update ok button
		boolean atLeastOneAccountEnabled = false;
		for (AccountStatus acc : accountStatuses) {
			if (!acc.hidden) {
				atLeastOneAccountEnabled = true;
				break;
			}
		}
		okButton.setEnabled(atLeastOneAccountEnabled);
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

		AccountManager.get(LoginActivity.this).addAccount(Constants.ACCOUNT_TYPE_GOOGLE,
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
	
	private static class AccountStatus {
		public String name;
		public boolean hidden;
	}

}
