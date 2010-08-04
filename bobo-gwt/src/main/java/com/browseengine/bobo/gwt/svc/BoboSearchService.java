package com.browseengine.bobo.gwt.svc;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("bobo-search")
public interface BoboSearchService extends RemoteService{
	BoboResult search(BoboRequest req);
}
