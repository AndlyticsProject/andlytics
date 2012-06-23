/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.github.andlyticsproject.admob;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;

import com.github.andlyticsproject.AdmobAuthenticatorActivity;
import com.github.andlyticsproject.Constants;
import com.github.andlyticsproject.exception.NetworkException;

/**
 * Provides utility methods for communicating with the server.
 */
public class AdmobAuthenticationUtilities {
        
    private static final int REQUEST_AUTHENTICATE = 0;


    private static final String TAG = AdmobAuthenticationUtilities.class.getSimpleName();


    /**
     * Executes the network requests on a separate thread.
     * 
     * @param runnable
     *            The runnable instance containing network mOperations to be
     *            executed.
     */
    public static Thread performOnBackgroundThread(final Runnable runnable) {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {

                }
            }
        };
        t.start();
        return t;
    }

    /**
     * Connects to the Voiper server, authenticates the provided username and
     * password.
     * 
     * @param username
     *            The user's username
     * @param password
     *            The user's password
     * @param handler
     *            The hander instance from the calling UI thread.
     * @param context
     *            The context of the calling Activity.
     * @return boolean The boolean result indicating whether the user was
     *         successfully authenticated.
     */
    public static String authenticate(String username, String password, Handler handler, final Context context) {

        try {
            String token = AdmobRequest.login(username, password);

            sendResult("true", handler, context);
            return token;
        } catch (NetworkException e) {
            Log.e(TAG, "Unsuccessful Admob authentication, network error");
            sendResult(AdmobRequest.ERROR_NETWORK_ERROR, handler, context);
            return AdmobRequest.ERROR_NETWORK_ERROR;
        } catch (AdmobInvalidRequestException e) {
            Log.e(TAG, "Unsuccessful Admob authentication, google accounts are not supported");
            sendResult(AdmobRequest.ERROR_REQUESET_INVALID, handler, context);
            return null;
        } catch (AdmobRateLimitExceededException e) {
            Log.e(TAG, "Unsuccessful Admob authentication, rate limit excceded");
            sendResult(AdmobRequest.ERROR_RATE_LIMIT_EXCEEDED, handler, context);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unsuccessful Admob authentication");
            sendResult("false", handler, context);
            return null;
        }

    }

    /**
     * Sends the authentication response from server back to the caller main UI
     * thread through its handler.
     * 
     * @param result
     *            The boolean holding authentication result
     * @param handler
     *            The main UI thread's handler instance.
     * @param context
     *            The caller Activity's context.
     */
    private static void sendResult(final String result, final Handler handler, final Context context) {
        if (handler == null || context == null) {
            return;
        }
        handler.post(new Runnable() {
            public void run() {
                ((AdmobAuthenticatorActivity) context).onAuthenticationResult(result);
            }
        });
    }

    /**
     * Attempts to authenticate the user credentials on the server.
     * 
     * @param username
     *            The user's username
     * @param password
     *            The user's password to be authenticated
     * @param handler
     *            The main UI thread's handler instance.
     * @param context
     *            The caller Activity's context
     * @return Thread The thread on which the network mOperations are executed.
     */
    public static Thread attemptAuth(final String username, final String password, final Handler handler,
                    final Context context) {
        final Runnable runnable = new Runnable() {
            public void run() {
                authenticate(username, password, handler, context);
            }
        };
        // run on background thread.
        return AdmobAuthenticationUtilities.performOnBackgroundThread(runnable);
    }

    public static String authenticateAccount(String accountName, Context context) {
        
        Account account = null;
        
        String token = null;
        
        AccountManager manager = AccountManager.get(context);
        if (accountName != null) {
            Account[] accounts = manager.getAccountsByType(Constants.ACCOUNT_TYPE_ADMOB);
            int size = accounts.length;
            for (int i = 0; i < size; i++) {
                Account ac = accounts[i];
                if (accountName.equals(ac.name)) {
                    account = ac;
                }
            }
        }
        
        if(account != null) {
            token = authenticateAccount(manager, account, context);
            
        } else {
            token = AdmobRequest.ERROR_ACCOUNT_REMOVED;
        }
        
        return token;
    }

    protected static String authenticateAccount(final AccountManager manager, final Account account, Context context) {
        
        String token = null;

        Bundle bundle;
        try {
            bundle = manager.getAuthToken(account, Constants.AUTHTOKEN_TYPE_ADMOB, true, null, null).getResult();

            if (bundle.containsKey(AccountManager.KEY_INTENT)) {
                
                if(context instanceof Activity) {
                    
                    // ask user for permission - launch account manager intent
                    Intent intent = bundle.getParcelable(AccountManager.KEY_INTENT);
                    int flags = intent.getFlags();
                    flags &= ~Intent.FLAG_ACTIVITY_NEW_TASK;
                    intent.setFlags(flags);
                    ((Activity)context).startActivityForResult(intent, REQUEST_AUTHENTICATE);
                } else {
                    Log.e(TAG, "Got admob KEY_INTENT to ask user for permission but context is not an activity");
                }

                token = AdmobRequest.ERROR_ASK_USER_PASSWORD;
                
            } else if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {

                //  got token form manager - set in application an exit
                final String authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                
             
                return authToken;

            }
        } catch (OperationCanceledException e1) {
            e1.printStackTrace();
        } catch (AuthenticatorException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        
        return token;

    }

    public static void invalidateToken(final String token, Context context) {

        Log.d(TAG, "invalidate admob token");

        AccountManager manager = AccountManager.get(context);
        manager.invalidateAuthToken(Constants.ACCOUNT_TYPE_ADMOB, token);

    }

}
