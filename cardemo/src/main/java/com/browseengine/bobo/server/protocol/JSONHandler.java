package com.browseengine.bobo.server.protocol;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.browseengine.bobo.serialize.JSONSerializable;
import com.browseengine.bobo.serialize.JSONSerializer;
import com.browseengine.bobo.serialize.JSONSerializable.JSONSerializationException;

public class JSONHandler extends ProtocolHandler {
	private static final String protocol="json";
	@Override
	public Object deserializeRequest(Class reqClass, HttpServletRequest req)
			throws IOException {
		String reqString=req.getParameter("req");
		if (null == reqString) {
			throw new IOException("no 'req' parameter specified on requet for deserialization of class "+reqClass.toString());
		}
		try {
			JSONObject jsonObj=new JSONObject(reqString);
			return JSONSerializer.deSerialize(reqClass, jsonObj);
		} catch (Exception e) {
			throw new IOException("deserialize request with class "+reqClass.toString()+": "+e.toString());
		} 
	}

	@Override
	public String getSupportedProtocol() {
		return protocol;
	}

	@Override
	public byte[] serializeResult(Object result) throws IOException {
		JSONObject jsonObj;
		try {
			jsonObj = JSONSerializer.serializeJSONObject((JSONSerializable)result);
			return jsonObj.toString().getBytes("UTF-8");
		} catch (Exception e) {
			throw new IOException(e.getMessage());
		}
		
	}

	@Override
	public Object deserializeRequest(Class reqClass, byte[] req) throws IOException {
		try {
			JSONObject jsonObj=new JSONObject(new String(req,"UTF-8"));
			return JSONSerializer.deSerialize(reqClass, jsonObj);
		} catch (Exception e) {
			throw new IOException(e.getMessage());
		} 
	}

}
