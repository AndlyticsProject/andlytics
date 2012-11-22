
package com.github.andlyticsproject;

// XX unused?
public abstract class DevConsoleRemoteCall<Params, Result> {

	public DevConsoleRemoteCall() {

	}

	public abstract DevConsoleRemoteCall<Params, Result> execute(Params... params);

}
