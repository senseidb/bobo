package com.browseengine.bobo.perf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class LogCleaner {
  public static void main(String[] args) throws IOException{
	File srcLogFile = new File(args[0]);
	File outLogFile = new File(args[1]);
	
	FileInputStream fin = new FileInputStream(srcLogFile);
	FileOutputStream fout = new FileOutputStream(outLogFile);
	
	BufferedReader r = new BufferedReader(new InputStreamReader(fin,"UTF-8"));
	
	BufferedWriter w = new BufferedWriter(new OutputStreamWriter(fout,"UTF-8"));
	
	while(true){
		String line = r.readLine();
		
		if (line == null){
			break;
		}
		
		int idx = line.indexOf("REQ: ");
		if (idx>=0){
			line = line.substring(idx+5);
			w.write(line);
			w.write('\n');
			w.flush();
		}
	}
	r.close();
	w.close();
  }
}
