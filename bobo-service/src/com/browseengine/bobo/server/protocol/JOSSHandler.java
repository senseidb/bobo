package com.browseengine.bobo.server.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.servlet.http.HttpServletRequest;

public class JOSSHandler extends ProtocolHandler {

	private static final String protocol="joss";
	
	public JOSSHandler() {
		super();
	}

	@Override
	public Object deserializeRequest(Class reqClass, HttpServletRequest req)
			throws IOException {
		ObjectInputStream oin=new ObjectInputStream(req.getInputStream());
		try {
			return oin.readObject();
		} catch (ClassNotFoundException e) {
			throw new IOException(e.getMessage());
		}
	}

	@Override
	public String getSupportedProtocol() {
		return protocol;
	}

	@Override
	public byte[] serializeResult(Object result) throws IOException {
		ByteArrayOutputStream bout=new ByteArrayOutputStream();
		ObjectOutputStream objOut=new ObjectOutputStream(bout);
		objOut.writeObject(result);
		objOut.flush();
		return bout.toByteArray();
	}

	@Override
	public Object deserializeRequest(Class reqClass, byte[] req) throws IOException {
		ObjectInputStream oin=new ObjectInputStream(new ByteArrayInputStream(req));
		try {
			return oin.readObject();
		} catch (ClassNotFoundException e) {
			throw new IOException(e.getMessage());
		}
	}

}
