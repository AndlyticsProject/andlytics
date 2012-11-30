package com.github.andlyticsproject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.github.andlyticsproject.db.AndlyticsDb;
import com.github.andlyticsproject.db.DeveloperAccountsTable;
import com.github.andlyticsproject.model.DeveloperAccount;

public class DeveloperAccountManager {

	private static final String TAG = DeveloperAccountManager.class.getSimpleName();

	private static DeveloperAccountManager instance;

	private AndlyticsDb andlyticsDb;

	public static synchronized DeveloperAccountManager getInstance(Context context) {
		if (instance == null) {
			instance = new DeveloperAccountManager(context);
		}

		return instance;
	}

	private DeveloperAccountManager(Context context) {
		andlyticsDb = AndlyticsDb.getInstance(context);
	}

	public synchronized long addDeveloperAccount(DeveloperAccount account) {
		return andlyticsDb.addDeveloperAccount(account);
	}

	public synchronized long addOrUpdateDeveloperAccount(DeveloperAccount account) {
		if (account.getId() != null) {
			updateDeveloperAccount(account);
			return account.getId();
		}

		long id = andlyticsDb.addDeveloperAccount(account);
		account.setId(id);

		return id;
	}

	public List<DeveloperAccount> getAllDeveloperAccounts() {
		List<DeveloperAccount> result = new ArrayList<DeveloperAccount>();

		SQLiteDatabase db = andlyticsDb.getReadableDatabase();
		Cursor c = null;
		try {
			c = db.query(DeveloperAccountsTable.DATABASE_TABLE_NAME,
					DeveloperAccountsTable.ALL_COLUMNS, null, null, null, null, "_id asc", null);
			while (c.moveToNext()) {
				DeveloperAccount account = createAcount(c);
				result.add(account);
			}

			return result;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public List<DeveloperAccount> getActiveDeveloperAccounts() {
		List<DeveloperAccount> result = new ArrayList<DeveloperAccount>();

		SQLiteDatabase db = andlyticsDb.getReadableDatabase();
		Cursor c = null;
		try {
			c = db.query(DeveloperAccountsTable.DATABASE_TABLE_NAME,
					DeveloperAccountsTable.ALL_COLUMNS, "state = ? or state = ?",
					new String[] { Integer.toString(DeveloperAccount.State.ACTIVE.ordinal()),
							Integer.toString(DeveloperAccount.State.SELECTED.ordinal()) }, null,
					null, "_id asc", null);
			while (c.moveToNext()) {
				DeveloperAccount account = createAcount(c);
				result.add(account);
			}

			return result;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public List<DeveloperAccount> getHiddenDeveloperAccounts() {
		return getDeveloperAccountsByState(DeveloperAccount.State.HIDDEN);
	}

	public DeveloperAccount getSelectedDeveloperAccount() {
		List<DeveloperAccount> accounts = getDeveloperAccountsByState(DeveloperAccount.State.SELECTED);
		if (accounts.isEmpty()) {
			return null;
		}

		if (accounts.size() > 1) {
			throw new IllegalStateException("More than one selected developer account: " + accounts);
		}

		return accounts.get(0);
	}

	public List<DeveloperAccount> getDeveloperAccountsByState(DeveloperAccount.State state) {
		List<DeveloperAccount> result = new ArrayList<DeveloperAccount>();

		SQLiteDatabase db = andlyticsDb.getReadableDatabase();
		Cursor c = null;
		try {
			c = db.query(DeveloperAccountsTable.DATABASE_TABLE_NAME,
					DeveloperAccountsTable.ALL_COLUMNS, "state = ?",
					new String[] { Integer.toString(state.ordinal()) }, null, null, "_id asc", null);
			while (c.moveToNext()) {
				DeveloperAccount account = createAcount(c);
				result.add(account);
			}

			return result;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public DeveloperAccount findDeveloperAccountById(long id) {
		SQLiteDatabase db = andlyticsDb.getReadableDatabase();
		Cursor c = null;
		try {
			c = db.query(DeveloperAccountsTable.DATABASE_TABLE_NAME,
					DeveloperAccountsTable.ALL_COLUMNS, "_id = ?",
					new String[] { Long.toString(id) }, null, null, "_id asc", null);
			if (!c.moveToNext()) {
				return null;
			}

			return createAcount(c);
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public DeveloperAccount findDeveloperAccountByName(String name) {
		SQLiteDatabase db = andlyticsDb.getReadableDatabase();
		Cursor c = null;
		try {
			c = db.query(DeveloperAccountsTable.DATABASE_TABLE_NAME,
					DeveloperAccountsTable.ALL_COLUMNS, "name = ?", new String[] { name }, null,
					null, "_id asc", null);
			if (!c.moveToNext()) {
				return null;
			}

			return createAcount(c);
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public synchronized void updateDeveloperAccount(DeveloperAccount account) {
		SQLiteDatabase db = andlyticsDb.getWritableDatabase();
		ContentValues values = AndlyticsDb.toValues(account);
		values.put(DeveloperAccountsTable.ROWID, account.getId());

		db.update(DeveloperAccountsTable.DATABASE_TABLE_NAME, values, "_id = ?",
				new String[] { Long.toString(account.getId()) });
	}

	public synchronized void deleteDeveloperAccount(DeveloperAccount account) {
		SQLiteDatabase db = andlyticsDb.getWritableDatabase();

		db.delete(DeveloperAccountsTable.DATABASE_TABLE_NAME, "name = ?",
				new String[] { account.getName() });
	}

	public synchronized void selectDeveloperAccount(String name) {
		SQLiteDatabase db = andlyticsDb.getWritableDatabase();
		db.beginTransaction();
		try {
			List<DeveloperAccount> currentlySelected = getDeveloperAccountsByState(DeveloperAccount.State.SELECTED);
			if (currentlySelected.size() > 1) {
				throw new IllegalStateException("More than one selected account: "
						+ currentlySelected);
			}

			if (!currentlySelected.isEmpty()) {
				DeveloperAccount selected = currentlySelected.get(0);
				if (selected.getName().equals(name)) {
					return;
				}

				selected.deselect();
				updateDeveloperAccount(selected);
				Log.d(TAG, "Set to ACTIVE: " + selected);
			}

			DeveloperAccount toSelect = findDeveloperAccountByName(name);
			if (toSelect == null) {
				throw new IllegalStateException("Account not found: " + name);
			}
			toSelect.select();
			updateDeveloperAccount(toSelect);
			Log.d(TAG, "Set to SELECTED: " + toSelect);

			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public synchronized void unselectDeveloperAccount() {
		SQLiteDatabase db = andlyticsDb.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(DeveloperAccountsTable.STATE, DeveloperAccount.State.ACTIVE.ordinal());
		db.update(DeveloperAccountsTable.DATABASE_TABLE_NAME, values, DeveloperAccountsTable.STATE
				+ " = ?",
				new String[] { Integer.toString(DeveloperAccount.State.SELECTED.ordinal()) });
	}

	public synchronized void activateDeveloperAccount(String accountName) {
		setAccountState(accountName, DeveloperAccount.State.ACTIVE);
	}

	public synchronized void hideDeveloperAccount(String accountName) {
		setAccountState(accountName, DeveloperAccount.State.HIDDEN);
	}

	private void setAccountState(String accountName, DeveloperAccount.State state) {
		SQLiteDatabase db = andlyticsDb.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(DeveloperAccountsTable.STATE, state.ordinal());
		db.update(DeveloperAccountsTable.DATABASE_TABLE_NAME, values, DeveloperAccountsTable.NAME
				+ " = ?", new String[] { accountName });
	}

	private DeveloperAccount createAcount(Cursor c) {
		long id = c.getLong(c.getColumnIndex(DeveloperAccountsTable.ROWID));
		String name = c.getString(c.getColumnIndex(DeveloperAccountsTable.NAME));
		DeveloperAccount.State currentState = DeveloperAccount.State.values()[c.getInt(c
				.getColumnIndex(DeveloperAccountsTable.STATE))];
		Date lastStatsUpdate = new Date(c.getLong(c
				.getColumnIndex(DeveloperAccountsTable.LAST_STATS_UPDATE)));

		DeveloperAccount account = new DeveloperAccount(name, currentState);
		account.setId(id);
		account.setLastStatsUpdate(lastStatsUpdate);
		return account;
	}

	public long getLastStatsRemoteUpdateTime(String accountName) {
		DeveloperAccount account = findDeveloperAccountByName(accountName);
		if (account == null) {
			throw new IllegalStateException("Account not found: " + accountName);
		}

		return account.getLastStatsUpdate().getTime();
	}

	public synchronized void saveLastStatsRemoteUpdateTime(String accountName, long timestamp) {
		DeveloperAccount account = findDeveloperAccountByName(accountName);
		if (account == null) {
			throw new IllegalStateException("Account not found: " + accountName);
		}

		account.setLastStatsUpdate(new Date(timestamp));
		updateDeveloperAccount(account);
	}

}
