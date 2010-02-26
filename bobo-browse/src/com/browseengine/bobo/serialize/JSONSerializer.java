/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2005-2006  John Wang
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * To contact the project administrators for the bobo-browse project, 
 * please go to https://sourceforge.net/projects/bobo-browse/, or 
 * send mail to owner@browseengine.com.
 */

package com.browseengine.bobo.serialize;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.browseengine.bobo.serialize.JSONSerializable.JSONSerializationException;

public class JSONSerializer {
	private static Logger logger=Logger.getLogger(JSONSerializer.class);
	
	private static void loadObject(Object array,Class type,int index,JSONArray jsonArray) throws JSONSerializationException,JSONException{
		if (type.isPrimitive()){
			if (type == Integer.TYPE){
				Array.setInt(array, index, jsonArray.getInt(index));
			}
			else if (type==Long.TYPE){
				Array.setLong(array, index, jsonArray.getInt(index));
			}
			else if (type==Short.TYPE){
				Array.setShort(array, index, (short)jsonArray.getInt(index));
			}
			else if (type==Boolean.TYPE){
				Array.setBoolean(array, index, jsonArray.getBoolean(index));
			}
			else if (type==Double.TYPE){
				Array.setDouble(array, index, jsonArray.getDouble(index));
			}
			else if (type==Float.TYPE){
				Array.setFloat(array, index, (float)jsonArray.getDouble(index));
			}
			else if (type==Character.TYPE){
				char ch=jsonArray.getString(index).charAt(0);
				Array.setChar(array, index, ch);
			}
			else if (type==Byte.TYPE){
				Array.setByte(array, index, (byte)jsonArray.getInt(index));
			}
			else{
				throw new JSONSerializationException("Unknown primitive: "+type);
			}
		}
		else if (type==String.class){
			Array.set(array, index, jsonArray.getString(index));
		}
		else if (JSONSerializable.class.isAssignableFrom(type)){
			JSONObject jObj=jsonArray.getJSONObject(index);
			JSONSerializable serObj=deSerialize(type,jObj);
			Array.set(array, index, serObj);
		}
		else if (type.isArray()){
			Class componentClass=type.getComponentType();
			JSONArray subArray=jsonArray.getJSONArray(index);
			int len=subArray.length();
			
			Object newArray=Array.newInstance(componentClass, len);
			for (int k=0;k<len;++k){
				loadObject(newArray,componentClass,k,subArray);
			}
		}
	}
	
	private static void loadObject(Object retObj,Field f,JSONObject jsonObj) throws JSONSerializationException{
		String key=f.getName();
		Class type=f.getType();
		try{
			if (type.isPrimitive()){
				if (type == Integer.TYPE){
					f.setInt(retObj, jsonObj.getInt(key));
				}
				else if (type==Long.TYPE){
					f.setLong(retObj, jsonObj.getInt(key));
				}
				else if (type==Short.TYPE){
					f.setShort(retObj, (short)jsonObj.getInt(key));
				}
				else if (type==Boolean.TYPE){
					f.setBoolean(retObj, jsonObj.getBoolean(key));
				}
				else if (type==Double.TYPE){
					f.setDouble(retObj, jsonObj.getDouble(key));
				}
				else if (type==Float.TYPE){
					f.setFloat(retObj, (float)jsonObj.getDouble(key));
				}
				else if (type==Character.TYPE){
					char ch=jsonObj.getString(key).charAt(0);
					f.setChar(retObj, ch);
				}
				else if (type==Byte.TYPE){
					f.setByte(retObj, (byte)jsonObj.getInt(key));
				}
				else{
					throw new JSONSerializationException("Unknown primitive: "+type);
				}
			}
			else if (type==String.class){
				f.set(retObj, jsonObj.getString(key));
			}
			else if (JSONSerializable.class.isAssignableFrom(type)){
				JSONObject jObj=jsonObj.getJSONObject(key);
				JSONSerializable serObj=deSerialize(type,jObj);
				f.set(retObj, serObj);
			}
		}
		catch(Exception e){
			throw new JSONSerializationException(e.getMessage(),e);
		}
	}
	
	public static JSONSerializable deSerialize(Class clz,JSONObject jsonObj) throws JSONSerializationException,JSONException{
		Iterator iter=jsonObj.keys();
		
		if (!JSONSerializable.class.isAssignableFrom(clz)){
			throw new JSONSerializationException(clz+" is not an instance of "+JSONSerializable.class);
		}
		
		JSONSerializable retObj;
		try {
			retObj = (JSONSerializable)clz.newInstance();
		} catch (Exception e1) {
			throw new JSONSerializationException("trouble with no-arg instantiation of "+clz.toString()+
					": "+e1.getMessage(),e1);
		}
		
		if (JSONExternalizable.class.isAssignableFrom(clz)){
			((JSONExternalizable)retObj).fromJSON(jsonObj);
			return retObj;
		}
		
		
		while(iter.hasNext()){
			String key=(String)iter.next();

			try {
				Field f=clz.getDeclaredField(key);
				if (f!=null){
					f.setAccessible(true);
					Class type=f.getType();
					
					if (type.isArray()){
						JSONArray array=jsonObj.getJSONArray(key);
						int len=array.length();
						Class cls=type.getComponentType();
						
						Object newArray=Array.newInstance(cls, len);
						for (int k=0;k<len;++k){
							loadObject(newArray,cls,k,array);
						}
						f.set(retObj, newArray);
					}
					else{
						loadObject(retObj,f,jsonObj);
					}
				}
			} catch (Exception e) {
				throw new JSONSerializationException(e.getMessage(),e);
			}
		}
		
		return retObj;
	}
	
	private static void dumpObject(JSONObject jsonObj,Field f,JSONSerializable srcObj) throws JSONSerializationException,JSONException{
		Object value;
		try {
			value = f.get(srcObj);
		} catch (Exception e) {
			throw new JSONSerializationException(e.getMessage(),e);
		} 
		
		Class type=f.getType();
		
		String name=f.getName();
		
		if (type.isPrimitive()){
			jsonObj.put(name, String.valueOf(value));
		}
		else if (type==String.class){
			if (value!=null){
				jsonObj.put(name,String.valueOf(value));
			}
		}
		else if (JSONSerializable.class.isInstance(value)){
			jsonObj.put(name,serializeJSONObject((JSONSerializable)value));
		}
	}
	
	private static void dumpObject(JSONArray jsonArray,Class type,Object array,int index) throws JSONSerializationException,JSONException{
		if (type.isPrimitive()){
			jsonArray.put(String.valueOf(Array.get(array, index)));
		}
		else if (type==String.class){
			String val=(String)Array.get(array,index);
			if (val!=null){
				jsonArray.put(val);
			}
		}
		else if (JSONSerializable.class.isAssignableFrom(type)){
			JSONSerializable o=(JSONSerializable)Array.get(array, index);
			JSONObject jobj=serializeJSONObject(o);
			jsonArray.put(jobj);
		}
		else if (type.isArray() && array!=null){
			Class compType=type.getComponentType();
			Object subArray=Array.get(array, index);
			int len=Array.getLength(subArray);
			JSONArray arr=new JSONArray();
			for (int k=0;k<len;++k){
				dumpObject(arr,compType,subArray,k);
			}
			jsonArray.put(arr);
		}
	}
	
	public static JSONObject serializeJSONObject(JSONSerializable obj) throws JSONSerializationException,JSONException{
		if (obj instanceof JSONExternalizable){
			return ((JSONExternalizable)obj).toJSON();
		}
		
		JSONObject jsonObj=new JSONObject();
		Class clz=obj.getClass();
		Field[] fields=clz.getDeclaredFields();
		
		for (int i=0;i<fields.length;++i){
			fields[i].setAccessible(true);
			int modifiers=fields[i].getModifiers();
			if (!Modifier.isTransient(modifiers) && !Modifier.isStatic(modifiers)){
				String name=fields[i].getName();
				Class type=fields[i].getType();
				
				try {
					Object value=fields[i].get(obj);
					
					if (type.isArray() && value!=null){
						int len=Array.getLength(value);
						
						Class cls=type.getComponentType();
						
						JSONArray array=new JSONArray();
						
						for (int k=0;k<len;++k){
							dumpObject(array,cls,value,k);
						}
						jsonObj.put(name, array);
					}
					else{
						dumpObject(jsonObj,fields[i],obj);
					}
				} catch (Exception e) {
					throw new JSONSerializationException(e.getMessage(),e); 
				}
			}
		}
		
		return jsonObj;
	}
	
	public static void main(String[] args) throws Exception{
		class B implements JSONSerializable{
			transient int tIntVal=6;
			String s="bstring";
			float[] fArray=new float[]{1.3f,1.2f,2.5f};
		}
		class C implements JSONExternalizable{
			private HashMap<String,String> map=new HashMap<String,String>();
			
			
			
			public void fromJSON(JSONObject obj) throws JSONSerializationException,JSONException {
				map.clear();
				Iterator iter=obj.keys();
				while(iter.hasNext()){
					String key=(String)iter.next();
					String val=obj.getString(key);
					map.put(key, val);
				}
			}

			public JSONObject toJSON() throws JSONSerializationException,JSONException {
				JSONObject retVal=new JSONObject();
				Iterator<String> iter=map.keySet().iterator();
				while(iter.hasNext()){
					String name=iter.next();
					String val=map.get(name);
					retVal.put(name,val);
				}
				return retVal;
			}
			
			public void set(String name,String val){
				map.put(name, val);
			}
		}
		class A implements JSONSerializable{
			int intVal=4;
			double doubleVal=1.2;
			short shortVal=12;
			HashMap hash=new HashMap();
			int[] intArray=new int[]{1,3};
			String[] strArray=new String[]{"john","wang"};
			B[] b=new B[]{new B(),new B()};
			B b2=new B();
			C c=new C();
			
			A(){
				c.set("city","san jose");
				c.set("country","usa");
			}
		}
		
		JSONObject jsonObj=JSONSerializer.serializeJSONObject(new A());
		
		String s1=jsonObj.toString();
		
		System.out.println(s1);
		
		A a=(A)deSerialize(A.class,jsonObj);
		jsonObj=JSONSerializer.serializeJSONObject(a);
		String s2=jsonObj.toString();
		
		System.out.println(s1.equals(s2));
	}
}
