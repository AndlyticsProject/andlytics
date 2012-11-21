package com.github.andlyticsproject.io;

import android.app.backup.FileBackupHelper;
import android.content.Context;

public class DbBackupHelper extends FileBackupHelper {

	public DbBackupHelper(Context ctx, String dbName) {
		super(ctx, "../databases/" + dbName);
	}
}
