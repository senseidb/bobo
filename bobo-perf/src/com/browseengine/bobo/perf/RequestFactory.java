package com.browseengine.bobo.perf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.protobuf.BrowseProtobufConverter;

public class RequestFactory{
	
	private BrowseRequest[] reqs;
	private RequestFactory(BrowseRequest[] r){
		reqs = r;
	}
	
	public static RequestFactory load(File qlogFile,int numReq) throws IOException{
		return new RequestFactory(init(qlogFile, numReq));
	}
	
	private static BrowseRequest[] init(File qlogFile,int numReq) throws IOException{
		BrowseRequest[] reqs;
		System.out.println("loading requests...");
		  ArrayList<BrowseRequest> reqList = new ArrayList<BrowseRequest>(numReq);
		  FileInputStream fin = null;
		  try{
			fin = new FileInputStream(qlogFile);
			BufferedReader buf = new BufferedReader(new InputStreamReader(fin,"UTF-8"));
			while(reqList.size()<numReq){
				String line = buf.readLine();
				if (line!=null){
					reqList.add(BrowseProtobufConverter.fromProtoBufString(line,null));
				}
				else{
					break;
				}
			}
		  }
		  finally{
			  if (fin!=null){
				  fin.close();
			  }
		  }
		  
		  BrowseRequest[] reqFromFile = reqList.toArray(new BrowseRequest[reqList.size()]);
		  if (reqFromFile.length == numReq){
			  reqs= reqFromFile;
		  }
		  else{
			  reqs = new BrowseRequest[numReq];
			  int chunks = numReq/reqFromFile.length;
			  int leftover = numReq%reqFromFile.length;
			  for (int i=0;i<chunks;++i){
				  System.arraycopy(reqFromFile, 0, reqs, i*reqFromFile.length, reqFromFile.length);
			  }
			  if (leftover > 0){
				  System.arraycopy(reqFromFile, 0, reqs,chunks*reqFromFile.length, leftover);
			  }
		  }
		  
		  return reqs;
	}
	
	public ReqIterator iterator(){
		return new ReqIterator(reqs);
	}
	
	public static class ReqIterator{
		private BrowseRequest[] req;
		private int idx = 0;
		private int len;
		private ReqIterator(BrowseRequest[] r){
			req = r;
			len = r.length;
		}
		
		public synchronized BrowseRequest next() {
			if (idx<len){
			  BrowseRequest br = req[idx];
			  idx++;
			  return br;
			}
			else{
			  return null;
			}
		}
	}
}
