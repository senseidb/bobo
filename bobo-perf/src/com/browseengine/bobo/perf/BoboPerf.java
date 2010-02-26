package com.browseengine.bobo.perf;

import java.io.File;
import java.io.IOException;

import org.apache.commons.configuration.PropertiesConfiguration;

import com.browseengine.bobo.perf.RequestFactory.ReqIterator;

public class BoboPerf extends AbstractPerfTest{
  
  
  public static final String QUERY_LOG_FILE="query.log.file";
  
  private File qlogFile;
  
  private RequestFactory _reqFactory;
  private ReqIterator _reqIter;
  
  public BoboPerf(PropertiesConfiguration propConf) throws IOException{
	  super(propConf);
  }
  
  @Override
  protected void init() throws IOException{ 
	  super.init();
	  
	  String qlogFileName=_propConf.getString(QUERY_LOG_FILE);
	  qlogFile = new File(qlogFileName);
	  if (!qlogFile.isAbsolute()){
		  qlogFile = new File(new File("conf"),qlogFileName);
	  }
	   
	  System.out.println("query log file: "+qlogFile.getAbsolutePath());
	  
	  _reqFactory = RequestFactory.load(qlogFile, numReq);

	  _reqIter = _reqFactory.iterator();
  }
  
  
  
  @Override
  public Thread buildWorkThread() {
	  return new BrowseThread(boboReader,_reqIter,throttleWait,this);
  }
  
  public static void main(String[] args) throws Exception{
	  File propFile = new File(args[0]);
	  BoboPerf perf = new BoboPerf(new PropertiesConfiguration(propFile));
	  perf.start();
  }
}
