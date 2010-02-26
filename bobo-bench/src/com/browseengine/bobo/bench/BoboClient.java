package com.browseengine.bobo.bench;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;

import com.browseengine.bobo.api.Browsable;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;

public class BoboClient
{

  private static class RequestBuilder
  {
    private final File _reqFile;
    RequestBuilder(File reqFile)
    {
      _reqFile = reqFile;
    }
    
    public BrowseRequest[] buildRequests() throws IOException
    {
      List<BrowseRequest> reqList = new LinkedList<BrowseRequest>();
      FileInputStream fin = new FileInputStream(_reqFile);
      BufferedReader reader = new BufferedReader(new InputStreamReader(fin,"UTF-8"));
      String line = reader.readLine();
      String[] facets = line.split(",");
      
      while(true)
      {
        line = reader.readLine();
        if (line==null) break;
        if (line.startsWith("//")) continue;
        String[] parts1 = line.split(":");
        if(parts1.length==2)
        {
          String[] parts2 = parts1[1].split(",");
          if (parts2.length>0)
          {
            BrowseSelection sel = new BrowseSelection(parts1[0]);
            sel.setValues(parts2);
            BrowseRequest req = new BrowseRequest();
            req.addSelection(sel);
            for (String facet : facets)
            {
              FacetSpec fspec = new FacetSpec();
              fspec.setOrderBy(FacetSortSpec.OrderHitsDesc);
              fspec.setMaxCount(10);
              req.setFacetSpec(facet, fspec);
            }
            reqList.add(req);
          }
        }
      }
      reader.close();
      return reqList.toArray(new BrowseRequest[reqList.size()]);
    }
  }
  
  /**
   * @param args
   */
  public static void main(String[] args) throws Exception
  {
    File dataFile = new File("/Users/jwang/proj/bobo-trunk/cardata/facetvals.txt");
    RequestBuilder reqBuilder = new RequestBuilder(dataFile);
    final BrowseRequest[] reqs = reqBuilder.buildRequests();
    
    final HttpInvokerProxyFactoryBean factoryBean = new HttpInvokerProxyFactoryBean();

    factoryBean.setServiceInterface(Browsable.class);
    factoryBean.setServiceUrl("http://localhost:8888/bobo-service/services/BrowseService");
    factoryBean.afterPropertiesSet();

    final Browsable svc = (Browsable) (factoryBean.getObject());

    int numThreads = 1;
    
    Thread[] threads = new Thread[numThreads];
    for (int i = 0; i < threads.length; ++i)
    {
      threads[i] = new Thread()
      {
        public void run()
        {
          for (BrowseRequest req : reqs)
          {
            try
            {
              long start = System.currentTimeMillis();
              BrowseResult result = svc.browse(req);
              long end = System.currentTimeMillis();
  
              long time = result.getTime();
              System.out.println("took: (c):" + (end - start) + " / (s):" + time);
              Thread.sleep(200);
            }
            catch(Exception e)
            {
              e.printStackTrace();
            }
          }
        }
        public void run2()
        {
          while (true)
          {
            try
            {
              BrowseRequest req = new BrowseRequest();
              BrowseSelection sel = new BrowseSelection("color");
              sel.addValue("red");
              req.addSelection(sel);

              FacetSpec fspec = new FacetSpec();
              fspec.setExpandSelection(true);
              fspec.setMaxCount(10);
              fspec.setOrderBy(FacetSortSpec.OrderHitsDesc);

              req.setFacetSpec("color", fspec);
              req.setFacetSpec("category", fspec);
              req.setFacetSpec("makemodel", fspec);
              req.setFacetSpec("city", fspec);
              req.setFacetSpec("price", fspec);
              req.setFacetSpec("year", fspec);

              long start = System.currentTimeMillis();
              BrowseResult result = svc.browse(req);
              long end = System.currentTimeMillis();

              long time = result.getTime();
              System.out.println("took: (c):" + (end - start) + " / (s):" + time);
              Thread.sleep(200);
            }
            catch (Exception e)
            {
              System.out.println("error: " + e.getMessage());
            }
          }
        }
      };
    }

    for (Thread t : threads)
    {
      t.start();
    }
    for (Thread t : threads)
    {
      t.join();
    }
  }

}
