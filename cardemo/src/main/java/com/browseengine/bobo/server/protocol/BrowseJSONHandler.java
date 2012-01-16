package com.browseengine.bobo.server.protocol;

import java.io.IOException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;

import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.serialize.JSONSerializable;
import com.browseengine.bobo.serialize.JSONSerializer;
import com.browseengine.bobo.serialize.JSONSerializable.JSONSerializationException;

public class BrowseJSONHandler extends ProtocolHandler {

	private static String protocol="browsejson";
	
	public BrowseJSONHandler() {
		super();
	}

	@Override
	public Object deserializeRequest(Class reqClass, HttpServletRequest req)
			throws IOException {
		
		String reqString = req.getParameter("reqstring");
		reqString=URLDecoder.decode(reqString, "UTF-8");
		
		BrowseRequest browseReq;
		return null;
		/*try {
			//browseReq = BrowseJSONSerializer.buildBrowseRequest(reqString);
			
		} catch (ParseException e) {
			throw new IOException(e.getMessage());
		}*/
		
	}

	@Override
	public String getSupportedProtocol() {
		return protocol;
	}

	@Override
	public byte[] serializeResult(Object result) throws IOException {
		String resultString=null;
		if (result instanceof BrowseResult){
			//resultString=BrowseJSONSerializer.serialize((BrowseResult)result);
		}
		else if (result instanceof JSONSerializable){
			try {
				resultString=JSONSerializer.serializeJSONObject((JSONSerializable)result).toString();
			} catch (Exception e) {
				throw new IOException(e.getMessage());
			}
		}
		else if (result instanceof String){
			resultString=(String)result;
		}
		else if (result.getClass().isPrimitive()){
			resultString=String.valueOf(result);
		}
		else{
			resultString=null;
		}
		if (resultString!=null){
			return resultString.getBytes("UTF-8");
		}
		else{
			return null;
		}
	}

	@Override
	public Object deserializeRequest(Class reqClass, byte[] req) throws IOException {
		BrowseRequest browseReq;
		/*try {
			browseReq = BrowseJSONSerializer.buildBrowseRequest(new String(req,"UTF-8"));
			return browseReq;
		} catch (ParseException e) {
			throw new IOException(e.getMessage());
		}*/
		return null;
	}

}
