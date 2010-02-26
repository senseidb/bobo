package com.browseengine.bobo.server.protocol;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

public abstract class ProtocolHandler {
	abstract public Object deserializeRequest(Class reqClass,HttpServletRequest req) throws IOException;
	
	abstract public Object deserializeRequest(Class reqClass,byte[] req) throws IOException;
	abstract public byte[] serializeResult(Object result) throws IOException;
	
	abstract public String getSupportedProtocol();
	
	private static final HashMap<String,ProtocolHandler> Registry=new HashMap<String,ProtocolHandler>();
	
	static{
		//JSONProtocolHandler handler=new JSONProtocolHandler();
		//Registry.put(handler.getSupportedProtocol(), handler);
	}
	
	public static void registerProtocolHandler(ProtocolHandler handler){
		synchronized(Registry){
			Registry.put(handler.getSupportedProtocol(), handler);
		}
	}
	
	public static ProtocolHandler getProtocolHandler(String protocol){
		return Registry.get(protocol);
	}
}
