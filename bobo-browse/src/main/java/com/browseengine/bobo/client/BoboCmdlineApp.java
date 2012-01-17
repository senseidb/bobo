package com.browseengine.bobo.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.Version;

import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.impl.BrowseServiceImpl;
import com.browseengine.bobo.service.BrowseService;

public class BoboCmdlineApp {

	private final BrowseRequestBuilder _reqBuilder;
	
	private final BrowseService _svc;
	
	public BoboCmdlineApp(BrowseService svc){
	  _svc = svc;
	  _reqBuilder = new BrowseRequestBuilder();
	}
	
	private void shutdown() throws BrowseException{
      if (_svc!=null){
        _svc.close();
      }
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
	    File idxDir = new File(args[0]);
	    System.out.println("index: "+idxDir.getAbsolutePath());
	    
		BrowseService svc = new BrowseServiceImpl(idxDir);
		
		final BoboCmdlineApp app = new BoboCmdlineApp(svc);
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				try {
				  app.shutdown();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		BufferedReader cmdLineReader = new BufferedReader(new InputStreamReader(System.in));
		while(true){
			try{
				System.out.print("> ");
				System.out.flush();
				String line = cmdLineReader.readLine();
				while(true){
					try{
					  app.processCommand(line);
					}
					catch(BrowseException ne){
						ne.printStackTrace();
					}
					System.out.print("> ");
					System.out.flush();
					line = cmdLineReader.readLine();
				}
				
			}
			catch(InterruptedException ie){
				throw new Exception(ie.getMessage(),ie);
			}
		}
	}
	
	void processCommand(String line) throws BrowseException, InterruptedException, ExecutionException{
		if (line == null || line.length() == 0) return;
		String[] parsed = line.split(" ");
		if (parsed.length == 0) return;
		
		String cmd = parsed[0];
		
		String[] args = new String[parsed.length -1 ];
		if (args.length > 0){
			System.arraycopy(parsed, 1, args, 0, args.length);
		}
		
		if ("exit".equalsIgnoreCase(cmd)){
			System.exit(0);
		}
		else if ("help".equalsIgnoreCase(cmd)){
			System.out.println("help - prints this message");
			System.out.println("exit - quits");
			System.out.println("query <query string> - sets query text");
			System.out.println("facetspec <name>:<minHitCount>:<maxCount>:<sort> - add facet spec");
			System.out.println("page <offset>:<count> - set paging parameters");
			System.out.println("select <name>:<value1>,<value2>... - add selection, with ! in front of value indicates a not");
			System.out.println("sort <name>:<dir>,... - set sort specs");
			System.out.println("showReq: shows current request");
			System.out.println("clear: clears current request");
			System.out.println("clearSelections: clears all selections");
			System.out.println("clearSelection <name>: clear selection specified");
			System.out.println("clearFacetSpecs: clears all facet specs");
			System.out.println("clearFacetSpec <name>: clears specified facetspec");
			System.out.println("browse - executes a search");
		}
		else if ("query".equalsIgnoreCase(cmd)){
			if (parsed.length<2){
				System.out.println("query not defined.");
			}
			else{
				String qString = parsed[1];
				String queryString = qString;
	            if (queryString!=null)
	            {
	              QueryParser qparser = new QueryParser(Version.LUCENE_CURRENT,"contents",new StandardAnalyzer(Version.LUCENE_CURRENT));
	              Query q;
	              try
	              {
	                q = qparser.parse(queryString);
	                _reqBuilder.getRequest().setQuery(q);
	              }
	              catch (Exception e)
	              {
	                e.printStackTrace();
	              }
	            }
			}
		}
		else if ("facetspec".equalsIgnoreCase(cmd)){
			if (parsed.length<2){
				System.out.println("facetspec not defined.");
			}
			else{
				try{
					String fspecString = parsed[1];
					String[] parts = fspecString.split(":");
					String name = parts[0];
					String fvalue=parts[1];
					String[] valParts = fvalue.split(",");
					if (valParts.length != 4){
						System.out.println("spec must of of the form <minhitcount>,<maxcount>,<isExpand>,<orderby>");
					}
					else{
						int minHitCount = 1;
						int maxCount = 5;
						boolean expand=false;
						FacetSortSpec sort = FacetSortSpec.OrderHitsDesc;
						try{
						   	minHitCount = Integer.parseInt(valParts[0]);
						}
						catch(Exception e){
							System.out.println("default min hitcount = 1 is applied.");
						}
						try{
							maxCount = Integer.parseInt(valParts[1]);
						}
						catch(Exception e){
							System.out.println("default maxCount = 5 is applied.");
						}
						try{
							expand =Boolean.parseBoolean(valParts[2]);
						}
						catch(Exception e){
							System.out.println("default expand=false is applied.");
						}
						
						if ("hits".equals(valParts[3])){
							sort = FacetSortSpec.OrderHitsDesc;
						}
						else{
							sort = FacetSortSpec.OrderValueAsc;
						}
						
						_reqBuilder.applyFacetSpec(name, minHitCount, maxCount, expand, sort);
					}
					
				}
				catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		else if ("select".equalsIgnoreCase(cmd)){
			if (parsed.length<2){
				System.out.println("selection not defined.");
			}
			else{
				try{
					String selString = parsed[1];
					String[] parts = selString.split(":");
					String name = parts[0];
					String selList = parts[1];
					String[] sels = selList.split(",");
					for (String sel : sels){
						boolean isNot=false;
						String val = sel;
						if (sel.startsWith("!")){
							isNot=true;
							val = sel.substring(1);
						}
						if (val!=null && val.length() > 0){
							_reqBuilder.addSelection(name, val, isNot);
						}
					}
				}
				catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		else if ("page".equalsIgnoreCase(cmd)){
			try{
				String pageString = parsed[1];
				String[] parts = pageString.split(":");
				_reqBuilder.setOffset(Integer.parseInt(parts[0]));
				_reqBuilder.setCount(Integer.parseInt(parts[1]));
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		else if ("clearFacetSpec".equalsIgnoreCase(cmd)){
			if (parsed.length<2){
				System.out.println("facet spec not defined.");
			}
			else{
				String name = parsed[1];
				_reqBuilder.clearFacetSpec(name);
			}
		}
		else if ("clearSelection".equalsIgnoreCase(cmd)){
			if (parsed.length<2){
				System.out.println("selection name not defined.");
			}
			else{
				String name = parsed[1];
				_reqBuilder.clearSelection(name);
			}
		}
		else if ("clearSelections".equalsIgnoreCase(cmd)){
			_reqBuilder.clearSelections();
		}
		else if ("clearFacetSpecs".equalsIgnoreCase(cmd)){
			_reqBuilder.clearFacetSpecs();
		}
		else if ("clear".equalsIgnoreCase(cmd)){
			_reqBuilder.clear();
		}
		else if ("showReq".equalsIgnoreCase(cmd)){
			BrowseRequest req = _reqBuilder.getRequest();
			System.out.println(req.toString());
		}
		else if ("sort".equalsIgnoreCase(cmd)){
			if (parsed.length == 2){
				String sortString = parsed[1];
				String[] sorts = sortString.split(",");
				ArrayList<SortField> sortList = new ArrayList<SortField>();
				for (String sort : sorts){
					String[] sortParams = sort.split(":");
					boolean rev = false;
					if (sortParams.length>0){
					  String sortName = sortParams[0];
					  if (sortParams.length>1){
						try{
						  rev = Boolean.parseBoolean(sortParams[1]);
						}
						catch(Exception e){
							System.out.println(e.getMessage()+", default rev to false");
						}
					  }
					  sortList.add(new SortField(sortName,SortField.CUSTOM,rev));
					}
				}
				_reqBuilder.applySort(sortList.toArray(new SortField[sortList.size()]));
			}
			else{
				_reqBuilder.applySort(null);
			}
		}
		else if ("browse".equalsIgnoreCase(cmd)){
			BrowseRequest req = _reqBuilder.getRequest();
			
			BrowseResult res = _svc.browse(req);
			String output = BrowseResultFormatter.formatResults(res);
			System.out.println(output);
		}
		else{
			System.out.println("Unknown command: "+cmd+", do help for list of supported commands");
		}
	}

}
