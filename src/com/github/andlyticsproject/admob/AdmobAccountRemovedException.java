package com.github.andlyticsproject.admob;

public class AdmobAccountRemovedException extends Exception {

    private static final long serialVersionUID = 1L;
    
    private String accountName;

    public AdmobAccountRemovedException(String string, String accountName) {
        super(string);
        this.setAccountName(accountName);
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountName() {
        return accountName;
    }

}
