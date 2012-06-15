package de.betaapps.andlytics;


public abstract class DeveloperConsoleRemoteCall<Params, Result> {

    public DeveloperConsoleRemoteCall() {
       
    }

    public abstract DeveloperConsoleRemoteCall<Params, Result> execute(Params... params);
    
}
