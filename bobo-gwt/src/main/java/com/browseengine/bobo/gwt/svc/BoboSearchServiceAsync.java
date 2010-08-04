package com.browseengine.bobo.gwt.svc;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface BoboSearchServiceAsync {
	void search(BoboRequest req, AsyncCallback<BoboResult> callback);
}
