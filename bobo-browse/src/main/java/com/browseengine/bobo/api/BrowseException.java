package com.browseengine.bobo.api;

public class BrowseException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public BrowseException(String msg){
		this(msg,null);
	}
	
	public BrowseException(String msg,Throwable cause){
		super(msg,cause);
	}
}
