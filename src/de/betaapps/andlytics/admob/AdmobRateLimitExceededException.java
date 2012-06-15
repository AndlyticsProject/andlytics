package de.betaapps.andlytics.admob;

public class AdmobRateLimitExceededException extends Exception {

    private static final long serialVersionUID = 1L;

    public AdmobRateLimitExceededException(String string) {
        super(string);
    }

}
