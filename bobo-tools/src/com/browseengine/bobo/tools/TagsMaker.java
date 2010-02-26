package com.browseengine.bobo.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Random;

public class TagsMaker {
	
	private static class Clause{
		String field;
		String val;
		
		boolean isTrue(Properties prop){
			if (val.equals("*")){
				return true;
			}
			boolean retval=false;
			Enumeration iter=prop.keys();
			while(iter.hasMoreElements()){
				String fname=(String)iter.nextElement();
				if (field.equalsIgnoreCase(fname)){
					if (val.equalsIgnoreCase((String)prop.getProperty(fname))){
						retval=true;
						break;
					}
					else{
						retval=false;
						break;
					}
				}
			}
			return retval;
		}
	}
	
	private static class Action{
		String tag;
		int percent;
	}
	
	private static class Rule{
		LinkedList<Clause> clauses=new LinkedList<Clause>();
		LinkedList<Action> actions=new LinkedList<Action>();
		
		
		String getTag(Properties prop){
			String tag=null;
			Random rand=new Random();
			if (actions.size()>0){
				boolean trigger=false;
				if (clauses.size()==0){
					trigger=true;
				}
				else{
					Iterator<Clause> iter=clauses.iterator();
					while(iter.hasNext()){
						Clause c=iter.next();
						trigger=c.isTrue(prop);
						if (trigger) break;
					}
				}
				
				if (trigger){
					int n=rand.nextInt(100)+1;
					Iterator<Action> iter=actions.iterator();
					int start=0;
					while(iter.hasNext()){
						Action action=iter.next();
						if (n>start && n<=action.percent+start){
							tag=action.tag;
							break;
						}
						else{
							start+=action.percent;
						}
					}
				}
			}
			return tag;
		}
	}
	
	ArrayList<Rule> rules;
	private TagsMaker(){
		rules=new ArrayList<Rule>();
	}
	
	public String[] getTags(Properties prop){
		Iterator<Rule> iter=rules.iterator();
		ArrayList<String> tags=new ArrayList<String>();
		while(iter.hasNext()){
			Rule r=iter.next();
			String tag=r.getTag(prop);
			if (tag!=null){
				tags.add(tag);
			}
		}
		return tags.toArray(new String[tags.size()]);
	}
	
	public static TagsMaker loadFile(File file) throws IOException{
		FileInputStream fin=null;
		TagsMaker tagsMaker=new TagsMaker();
		try{
			Rule rule=null;
			fin=new FileInputStream(file);
			BufferedReader reader=new BufferedReader(new InputStreamReader(fin,"UTF-8"));
			while(true){
				String line=reader.readLine();
				if (line==null) break;
				line=line.trim();
				if (line.length()==0) continue;
				if (line.startsWith("?")){
					rule=new Rule();
					tagsMaker.rules.add(rule);
					line=line.substring(1);
					if (line.length()>0){					
						String[] clauseLines=line.split(";");
						for (int i=0;i<clauseLines.length;++i){
							Clause c=new Clause();
							String[] parts=clauseLines[i].split(":");
							c.field=parts[0];
							c.val=parts[1];
							rule.clauses.add(c);
						}
					}
				}
				else{
					if (rule!=null){
						String[] parts=line.split(":");
						Action a=new Action();
						a.tag=parts[0];
						a.percent=Integer.parseInt(parts[1]);
						rule.actions.add(a);
					}
				}
			}
		}
		finally{
			if (fin!=null){
				try{
					fin.close();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		
		return tagsMaker;
	}
}
