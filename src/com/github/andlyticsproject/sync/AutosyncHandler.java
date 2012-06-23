package com.github.andlyticsproject.sync;


public interface AutosyncHandler {

    int DEFAULT_PERIOD = 60 * 60 * 3; // 3 hours

    boolean isAutosyncEnabled(String accountname);

    int getAutosyncPeriod(String accountname);

    void setAutosyncPeriod(String accountName, Integer integer);


}
