package com.browseengine.bobo.server.protocol;

import java.util.Iterator;

public abstract class BoboParams {
	abstract public String get(String name);
	abstract public String[] getStrings(String name);
	abstract public Iterator<String> getParamNames();
	 
	public String getString(String name,boolean required){
		String retVal=get(name);
		if (retVal==null && required){
			throw new IllegalArgumentException("parameter "+name+" does not exist");
		}
		return retVal;
	}
	public boolean getBool(String name,boolean defaultVal){
		String retVal=getString(name,false);
		if (retVal!=null){
			return Boolean.parseBoolean(retVal);
		}
		else{
			return defaultVal;
		}
	}
	
	public String getString(String name){
		return getString(name,false);
	}
	
	public String getString(String name,String defaultVal){
		String retVal=get(name);
		if (retVal==null) return defaultVal;
		return retVal;
	}
	
	public int getInt(String name,boolean required){
		String retVal=getString(name,required);
		return Integer.parseInt(retVal);
	}
	
	public int getInt(String name,int defaultVal){
		String retVal=getString(name,false);
		try{
			return Integer.parseInt(retVal);
		}
		catch(Exception e){
			return defaultVal;
		}
	}
}
