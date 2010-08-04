package com.browseengine.bobo.demo.client;

import com.browseengine.bobo.gwt.svc.BoboSearchService;
import com.browseengine.bobo.gwt.svc.BoboSearchServiceAsync;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.RootPanel;

public class BoboDemo implements EntryPoint{
  /**
   * The message displayed to the user when the server cannot be reached or
   * returns an error.
   */
  private static final String SERVER_ERROR = "An error occurred while "
      + "attempting to contact the server. Please check your network "
      + "connection and try again.";

  private final BoboSearchServiceAsync searchService = GWT.create(BoboSearchService.class);
  
  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
    BoboDemoPanel demoPane = new BoboDemoPanel(searchService);
    RootPanel.get("toptab").add(demoPane);
  }
}
