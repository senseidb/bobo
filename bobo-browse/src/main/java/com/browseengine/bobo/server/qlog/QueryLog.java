package com.browseengine.bobo.server.qlog;

import org.apache.log4j.Logger;


public class QueryLog {
	private static Logger logger=Logger.getLogger(QueryLog.class);
	
	public static class LogLine{
		String protocol;
		String method;
		String request;
		
		private LogLine(){
			
		}
		public String getMethod() {
			return method;
		}
		
		public String getProtocol() {
			return protocol;
		}
		
		public String getRequest() {
			return request;
		}
		
		
	}
	
	public static void logQuery(String request){
		logger.info(request);
	}
	
	public static LogLine readLog(String line){
		
		LogLine log=new LogLine();
		int index=line.indexOf('#');
		if (index!=-1){
			String header=line.substring(0, index);
			log.request=line.substring(index+1,line.length());
			
			String[] parts=header.split("/");
			log.protocol=parts[0];
			log.method=parts[1];
			
		}
		return log;
	}
}
