package com.browseengine.bobo.tools.ui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.impl.MultiValueFacetHandler;
import com.browseengine.bobo.facets.impl.PathFacetHandler;
import com.browseengine.bobo.facets.impl.RangeFacetHandler;
import com.browseengine.bobo.facets.impl.SimpleFacetHandler;

public class MakeUITask extends Task {
	private String output;
	private String name;
	private String indexDir;
	private String fieldConf;
	private String url;
	
	public MakeUITask(){
		super();
		output=null;
		name=null;
		url = null;
	}
	
	public void setURL(String url){
		this.url = url;
	}
	
	public void setOutput(String output){
		this.output=output;
	}
	
	public void setName(String name){
		this.name=name;
	}
	
	public void setIndexDir(String idxDir){
		indexDir=idxDir;
	}
	
	public void setFieldConf(String fieldConf){
		this.fieldConf=fieldConf;
	}
	
	private void renderFields(String[] fields,PrintWriter pw)
	{
		int size = fields.length;
		if(size == 0)
			return;
		int portion = 100/size;
		pw.print("<table width=\"100%\" border=\"1\" summary=\"\"> <tr valign=\"top\">");
		
		for(String field: fields)
		{
			pw.print("<td width=\"" + portion + "%\">\n");
			pw.print("<div id=\"" + field + "\"></div>\n");
			pw.println("</td>");
		}
		pw.println("</tr></table>");
	}
	
	public void writeHTML(File location,List<FacetHandler> fconf) throws IOException{
		File webXml=new File(location,"index.html");
		FileOutputStream fout=null;
		try{
			fout=new FileOutputStream(webXml);
			BufferedWriter bwriter=new BufferedWriter(new OutputStreamWriter(fout,"UTF-8"));
			PrintWriter out=new PrintWriter(bwriter);
			
			out.println("<html>");
			// write header
			String header = "<head><title></title>\n"
				+ "<link rel=StyleSheet href=\"style.css\" type=\"text/css\">\n"
				+ "<script type=\"text/javascript\" src=\"browse.js\">	</script>\n"
				+ "<script type=\"text/javascript\" src=\"bobobase.js\"></script>\n"
				+ "<script type=\"text/javascript\" src=\"json.js\"></script>\n"
				+ "<script type=\"text/javascript\" src=\"widgets.js\"></script>\n"
				+ "<script type=\"text/javascript\" src=\"remote.js\"></script>\n"
				+ "<script type=\"text/javascript\" src=\"application.js\"></script>\n"
				+ "</head>";
			out.println(header);

			//start body
			out.println("<body onload=\"javascript:loadBody()\">");
			
			String searchTxt = "<table width=\"100%\" border=\"0\" summary=\"\">\n"
	               + "<tr>\n"
	               + "<td align=\"left\"> <span class=\"text\">Hits: </span><span id=\"hitcount\" class=\"hitstat\"> </span> </td>\n"
	               + "<td align=\"right\"> <span class=\"text\"></span> <input id=\"search\"></input> <button onclick=\"javascript:handleSearch()\">Search</button> </td>\n"
	               + "</tr>\n"
	               + "<tr>\n"
	               + "<td colspan=\"2\" align=\"right\"> <a href=\"javascript:reset()\">reset all</a> </td>\n"
	               + "</tr>\n"
	               + "</table>\n";
			out.print(searchTxt);
			
			ArrayList<String> simpleFields=new ArrayList<String>();
			ArrayList<String> pathFields=new ArrayList<String>();
			ArrayList<String> tagFields=new ArrayList<String>();
			
			for (FacetHandler fPlugin : fconf){
				String name = fPlugin.getName();
				if (fPlugin instanceof SimpleFacetHandler || fPlugin instanceof RangeFacetHandler){
					simpleFields.add(name);
				}
				else if (fPlugin instanceof PathFacetHandler){
					pathFields.add(name);
				}
				else if (fPlugin instanceof MultiValueFacetHandler){
					tagFields.add(name);
				}
				else{
					System.out.println("field not rendered: "+name);
				}
			}
			
			renderFields(pathFields.toArray(new String[pathFields.size()]), out);
			renderFields(tagFields.toArray(new String[tagFields.size()]), out);
			renderFields(simpleFields.toArray(new String[simpleFields.size()]), out);

			out.println("<div id=\"resultset\"></div>");
			out.println("<script type=\"text/javascript\">\n"
					+ "app_setup();\n"
					+ "</script>");
			out.println("</body>");
			out.println("</html>");
			out.flush();
		}
		finally{
			if (fout!=null){
				try {
					fout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void writeWebXML(File location,String idxDir) throws IOException{
		File webXml=new File(location,"web.xml");
		FileOutputStream fout=null;
		try{
			fout=new FileOutputStream(webXml);
			BufferedWriter bwriter=new BufferedWriter(new OutputStreamWriter(fout,"UTF-8"));
			PrintWriter out=new PrintWriter(bwriter);
			
			out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

			out.write("<!DOCTYPE web-app PUBLIC \"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN\" \"http://java.sun.com/dtd/web-app_2_3.dtd\">\n");

			out.write("<web-app> \n<display-name>Bobo Browse - App</display-name>\n <description></description>\n");
			out.write("<servlet> \n<servlet-name>BrowseServlet</servlet-name>\n<servlet-class>com.browseengine.bobo.servlet.BrowseServlet</servlet-class>\n");
			out.write("<init-param><param-name>config.dir</param-name>\n");
			out.write("<init-param><param-name>config.dir</param-name><param-value>/home/javasoze/data/cardemo</param-value></init-param>\n");	
			out.write("</servlet>\n<servlet-mapping>\n<servlet-name>BrowseServlet</servlet-name>\n<url-pattern>/bobo/*</url-pattern>\n");
			out.write("</servlet-mapping>\n</web-app>\n");
			out.flush();
		}
		finally{
			if (fout!=null){
				try {
					fout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
		
	
	//var sortnames=new Array("color","category","price","year","mileage");
	
	public void writeApplicationJS(File location,List<FacetHandler> fConf) throws IOException{
		File appJS=new File(location,"application.js");
		
		ArrayList<FacetHandler> sortableList=new ArrayList<FacetHandler>();
		for (FacetHandler plugin : fConf){
			if (plugin instanceof SimpleFacetHandler || plugin instanceof RangeFacetHandler) {
				sortableList.add(plugin);
			}
		}
		
		FileOutputStream fout=null;
		try{
			fout=new FileOutputStream(appJS);
			BufferedWriter bwriter=new BufferedWriter(new OutputStreamWriter(fout,"UTF-8"));
			PrintWriter out=new PrintWriter(bwriter);
			
			// globals
			out.write("var url=\"/"+name+url+"?\";\n");
			out.write("var api=new BoboAPI(url);\n");
			out.write("var sortnames=new Array(");
			
			int i=0;
			for (FacetHandler sortHandler : sortableList){
				if (i!=0){
					out.write(",");
				}
				out.write("\""+sortHandler.getName()+"\"");
				++i;
			}
			out.write(");\n\n\n");
			
			out.write("function app_setup(){\n");
			for (FacetHandler plugin : fConf){
				String name = plugin.getName();
				if (plugin instanceof SimpleFacetHandler) {
					// setup the widget
					out.write("var "+name+"Section=document.getElementById(\""+name+"\");\n");
					out.write("var "+name+"Widget=new SelectList("+name+"Section,handleSelection);\n");
					out.write("widgets.unshift("+name+"Widget);\n");
					
					// setup for the request
					out.write("var "+name+"Selection=new Selection(Selection_Type_Simple);\n");
					out.write("request.selections[\""+name+"\"]="+name+"Selection;\n");
					
					// setup the outputspec
					out.write("var "+name+"OutputSpec=new OutputSpec();\n");	
					out.write(name+"OutputSpec.expandSelection=true;\n");
					out.write(name+"OutputSpec.max=10;\n");
					out.write("request.ospecs[\""+name+"\"]="+name+"OutputSpec;\n");
				}
				else if (plugin instanceof RangeFacetHandler){
					//	setup the widget
					out.write("var "+name+"Section=document.getElementById(\""+name+"\");\n");
					out.write("var "+name+"Widget=new SelectList("+name+"Section,handleSelection);\n");
					//out.write(fields[i]+"Widget.displayFn=format;");
					out.write("widgets.unshift("+name+"Widget);\n");
					
					// setup for the request
					out.write("var "+name+"Selection=new Selection(Selection_Type_Range);\n");
					out.write("request.selections[\""+name+"\"]="+name+"Selection;\n");
					
					// setup the outputspec

					out.write("var "+name+"OutputSpec=new OutputSpec();\n");
					
					out.write(name+"OutputSpec.expandSelection=true;\n");
					out.write(name+"OutputSpec.max=5;\n");
					out.write("request.ospecs[\""+name+"\"]="+name+"OutputSpec;\n");
				}
				else if (plugin instanceof PathFacetHandler){
//					setup the widget
					out.write("var "+name+"Section=document.getElementById(\""+name+"\");\n");
					out.write("var "+name+"Widget=new SelectPath("+name+"Section,handleSelection,\"/\",4,5);\n");
					out.write("widgets.unshift("+name+"Widget);\n");
					
					// setup for the request
					out.write("var "+name+"Selection=new Selection(Selection_Type_Path);\n");
					out.write(name+"Selection.depth=1;\n");
					
					out.write("request.selections[\""+name+"\"]="+name+"Selection;\n");
					
					// setup the outputspec
					out.write("var "+name+"OutputSpec=new OutputSpec();\n");						
					out.write("request.ospecs[\""+name+"\"]="+name+"OutputSpec;\n");
				}
				else if (plugin instanceof MultiValueFacetHandler)
				{
//                setup the widget
                  out.write("var "+name+"Section=document.getElementById(\""+name+"\");\n");
                  out.write("var "+name+"Widget=new TagCloud("+name+"Section,tagselected);\n");
                  out.write("widgets.unshift("+name+"Widget);\n");
                  
                  // setup for the request
                  out.write("var "+name+"Selection=new Selection(Selection_Type_Simple);\n");
                  out.write("request.selections[\""+name+"\"]="+name+"Selection;\n");
                  out.write(name+"Selection.operation=Value_Operation_AND;\n");
                  
                  // setup the outputspec
                  out.write("var "+name+"OutputSpec=new OutputSpec();\n");   
                  out.write(name+"OutputSpec.max=10;\n");
                  out.write(name+"OutputSpec.order=OutputSpec_Order_Hits;\n");
                  out.write("request.ospecs[\""+name+"\"]="+name+"OutputSpec;\n");
				}
				else{
					System.out.println("skipping field: "+name+", not a renderable field.");
				}
				out.write("\n");
			}
			out.write("resultslist=new ResultsTable(document.getElementById(\"resultset\"),sortnames);\n");
			out.write("}\n");
			out.flush();
		}
		finally{
			if (fout!=null){
				try {
					fout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void copyFile(File in, File outDir) throws IOException {
	     FileChannel sourceChannel = null;
	     FileChannel destinationChannel=null;
	     try{
		     sourceChannel= new
		          FileInputStream(in).getChannel();
		     
		     File out=new File(outDir,in.getName());
		     destinationChannel = new
		          FileOutputStream(out).getChannel();
		     sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
	     }
	     finally{
	    	 try{
		    	 if (sourceChannel!=null){
		    		 sourceChannel.close();
		    	 }
	    	 }
	    	 finally{
	    		 if (destinationChannel!=null){
	    			 destinationChannel.close();
	    		 }
	    	 }
	     }
	}
	
	@Override
	public void execute() throws BuildException {
		
		if (name==null){
			throw new BuildException("name not specified.");
		}
		if (output==null){
			throw new BuildException("output directory is not specified.");
		}
		
		File fConfFile=null;
		
		if (indexDir!=null){
			File tempF=new File(indexDir,"bobo.spring");
			if (tempF.exists() && tempF.isFile()){
				fConfFile=tempF;
			}
		}
		
		if (fConfFile==null && fieldConf!=null){
			File tempF=new File(fieldConf);
			if (tempF.exists() && tempF.isFile()){
				fConfFile=tempF;
			}
		}
		
		if (fConfFile==null){
			throw new BuildException("Please provide field.xml location by either setting the indexDir or the fieldConf property.");
		}
		
		
		List<FacetHandler> fConf=null;
		
		Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
		ApplicationContext appCtx=null;
		try{
			appCtx=new FileSystemXmlApplicationContext("file:"+fConfFile.getAbsolutePath());
			fConf =  (List<FacetHandler>)appCtx.getBean("handlers");
		}
		catch(Exception e){
			throw new BuildException(e.getMessage(),e);
		}
		
		File baseDir=getProject().getBaseDir();
		
		ArrayList<File> copyList=new ArrayList<File>(10);
		copyList.add(new File(new File(baseDir,"webapp"),"bobobase.js"));
		copyList.add(new File(new File(baseDir,"webapp"),"browse.js"));
		copyList.add(new File(new File(baseDir,"webapp"),"style.css"));
		copyList.add(new File(new File(baseDir,"webapp"),"json.js"));
		copyList.add(new File(new File(baseDir,"webapp"),"widgets.js"));
		copyList.add(new File(new File(baseDir,"webapp"),"remote.js"));
		
		File outputDir=new File(new File(baseDir,output),name);
		outputDir.mkdirs();
		try{
			Iterator<File> iter=copyList.iterator();
			while(iter.hasNext()){
				copyFile(iter.next(),outputDir);
			}
		}
		catch(IOException e){
			throw new BuildException(e.getMessage(),e);
		}
		
		try {
			//writeWebXML(outputDir, indexDir);
			writeApplicationJS(outputDir, fConf);
			writeHTML(outputDir, fConf);
		} catch (IOException e) {
			throw new BuildException(e.getMessage(),e);
		}
	}

	@Override
	public String getTaskName() {
		return "bobo-ui";
	}
	
}
