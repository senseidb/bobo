package com.browseengine.bobo.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.impl.SimpleFacetHandler;
import com.browseengine.bobo.index.BoboIndexer;
import com.browseengine.bobo.index.digest.DataDigester;

import junit.framework.TestCase;

/**
 * This class is to test all kinds of values of FacetSpec.max<0 (which should return all facet values)
 * @author hyan
 *
 */
public class FacetMaxCountTest extends TestCase {
  static Logger log = Logger.getLogger(FacetMaxCountTest.class);
  private List<FacetHandler<?>> _facetHandlers;
  private int _documentSize;
  
  private static class TestDataDigester extends DataDigester {
    private List<FacetHandler<?>> _facetHandlers;
    private Document[] _data;
    
    TestDataDigester(List<FacetHandler<?>> facetHandlers,Document[] data){
      super();
      _facetHandlers = facetHandlers;
      _data=data;
    }
    @Override
    public void digest(DataHandler handler) throws IOException {
      for (int i=0;i<_data.length;++i){
        handler.handleDocument(_data[i]);
      }
    }
  }
  
  public FacetMaxCountTest(String testName){
    super(testName);
    _facetHandlers = createFacetHandlers();
    
    _documentSize = 100;
    String confdir = System.getProperty("conf.dir");
    if (confdir == null) confdir ="./resource";
    org.apache.log4j.PropertyConfigurator.configure(confdir+"/log4j.properties");
  }
  
  public Document[] createData(){
    ArrayList<Document> dataList=new ArrayList<Document>();
    for(int i=0; i<_documentSize; ++i)
    {
      String color = null;
      String make = null;
      String type = null;
      if(i<10) 
      {
        color = "orange";
        make = "camry";
        type = "ce";
      }
      else if(i<20)
      {
        color = "green";
        make = "yaris";
        type = "le";
      }      
      else if(i<30)
      {
        color = "blue";
        make = "yaris";
        type = "le";
      }
      else if(i<40)
      {
        color = "yellow";
        make = "yaris";
        type = "le";
      }
      else if(i<50)
      {
        color = "red";
        make = "yaris";
        type = "le";
      }
      else if(i<60)
      {
        color = "red";
        make = "corrola";
        type = "le";
      }
      else
      {
        color = "red";
        make = "avalon";
        type = "ce";
      }
      String ID = Integer.toString(i);
      Document d=new Document();
      d.add(new Field("id",ID,Field.Store.YES,Index.NOT_ANALYZED_NO_NORMS));
      d.add(new Field("color",color,Field.Store.YES,Index.NOT_ANALYZED_NO_NORMS));
      d.add(new Field("make",make,Field.Store.YES,Index.NOT_ANALYZED_NO_NORMS));
      d.add(new Field("type",type,Field.Store.YES,Index.NOT_ANALYZED_NO_NORMS));
      dataList.add(d);
    }
    return dataList.toArray(new Document[dataList.size()]);
}
  
  private Directory createIndex(){
    Directory dir = new RAMDirectory();
    try {
      Document[] data= createData();
      
      TestDataDigester testDigester=new TestDataDigester(_facetHandlers,data);
      BoboIndexer indexer=new BoboIndexer(testDigester,dir);
      indexer.index();
      IndexReader r = IndexReader.open(dir,false);
      r.close();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return dir;
  }
  
  public static List<FacetHandler<?>> createFacetHandlers(){
    List<FacetHandler<?>> facetHandlers = new ArrayList<FacetHandler<?>>();
    facetHandlers.add(new SimpleFacetHandler("id"));
    facetHandlers.add(new SimpleFacetHandler("make"));
    facetHandlers.add(new SimpleFacetHandler("color"));
    facetHandlers.add(new SimpleFacetHandler("type"));
    return facetHandlers;
  }
  
  
  public void testFacetSpecMaxCountGreaterThanFacetValueNum() throws Exception{
    BrowseRequest br=new BrowseRequest();
    br.setCount(10);
    br.setOffset(0);

    BrowseSelection colorSel=new BrowseSelection("color");
    colorSel.addValue("red");
    br.addSelection(colorSel); 
    
    FacetSpec spec=new FacetSpec();
    spec.setExpandSelection(true);
    spec.setOrderBy(FacetSortSpec.OrderHitsDesc);
    spec.setMaxCount(15);
    
    br.setFacetSpec("color", spec);
    br.setFacetSpec("id", spec);
    br.setFacetSpec("make", spec);
    br.setFacetSpec("type", spec);
    
    BrowseResult result = null;
    BoboBrowser boboBrowser=null;
    int expectedHitNum = 60;
    int expectedPagedHitNum = 10;
    int expectedFacetNum = 4; 
    try {
      Directory ramIndexDir = createIndex();
      IndexReader srcReader=IndexReader.open(ramIndexDir,true);
      boboBrowser = new BoboBrowser(BoboIndexReader.getInstance(srcReader,_facetHandlers, null));
      result = boboBrowser.browse(br);
      
      assertEquals(expectedHitNum,result.getNumHits());
      assertEquals(expectedPagedHitNum,result.getHits().length);
      int facetNum = result.getFacetMap().entrySet().size();
      assertEquals(expectedFacetNum,facetNum);
      
    } catch (BrowseException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    catch(IOException ioe){
      fail(ioe.getMessage());
    }
    finally{
      if (boboBrowser!=null){
        try {
          if(result!=null) result.close();
          boboBrowser.close();
        } catch (IOException e) {
          fail(e.getMessage());
        }
      }
    }

  }  
  
  public void testFacetSpecMaxCountLessThanFacetNum() throws Exception{
    BrowseRequest br=new BrowseRequest();
    br.setCount(10);
    br.setOffset(0);

    FacetSpec spec=new FacetSpec();
    spec.setExpandSelection(true);
    spec.setOrderBy(FacetSortSpec.OrderValueAsc);
    spec.setMinHitCount(1);
    spec.setMaxCount(2);
    
    br.setFacetSpec("color", spec);
    br.setFacetSpec("id", spec);
    br.setFacetSpec("make", spec);
    br.setFacetSpec("type", spec);
    
    BrowseResult result = null;
    BoboBrowser boboBrowser=null;
    int expectedHitNum = 100;
    int expectedPagedHitNum = 10;
    int expectedFacetNum = 4; 
    int expectedFacetValueNum_id = 2; // since MaxCount==2
    int expectedFacetValueNum_make = 2; 
    int expectedFacetValueNum_color = 2;
    int expectedFacetValueNum_type = 2;
    try {
      Directory ramIndexDir = createIndex();
      IndexReader srcReader=IndexReader.open(ramIndexDir,true);
      boboBrowser = new BoboBrowser(BoboIndexReader.getInstance(srcReader,_facetHandlers, null));
      result = boboBrowser.browse(br);
      
      assertEquals(expectedHitNum,result.getNumHits());
      assertEquals(expectedPagedHitNum,result.getHits().length);
      int facetNum = result.getFacetMap().entrySet().size();
      assertEquals(expectedFacetNum,facetNum);
     
      int facetValueNum;
      for(Entry<String, FacetAccessible> entry: result.getFacetMap().entrySet())
      {
        if(entry.getKey().equals("id"))
        {
          facetValueNum = entry.getValue().getFacets().size();
          assertEquals(facetValueNum, expectedFacetValueNum_id);
        }
        else if(entry.getKey().equals("make"))
        {
           facetValueNum = entry.getValue().getFacets().size();
          assertEquals(entry.getValue().getFacets().size(), expectedFacetValueNum_make);
        }
        else if(entry.getKey().equals("color"))
        {
           facetValueNum = entry.getValue().getFacets().size();
          assertEquals(entry.getValue().getFacets().size(), expectedFacetValueNum_color);
        }
        else //type
        {
           facetValueNum = entry.getValue().getFacets().size();
          assertEquals(entry.getValue().getFacets().size(), expectedFacetValueNum_type);
        }
      }
      
    } catch (BrowseException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    catch(IOException ioe){
      fail(ioe.getMessage());
    }
    finally{
      if (boboBrowser!=null){
        try {
          if(result!=null) result.close();
          boboBrowser.close();
        } catch (IOException e) {
          fail(e.getMessage());
        }
      }
    }

  }  
  
  
  public void testFacetSpecMaxLessThanZero() throws Exception{
    BrowseRequest br=new BrowseRequest();
    br.setCount(10);
    br.setOffset(0);

    FacetSpec spec=new FacetSpec();
    spec.setExpandSelection(true);
    spec.setOrderBy(FacetSortSpec.OrderValueAsc);
    spec.setMinHitCount(1);
    spec.setMaxCount(-1);
    
    br.setFacetSpec("color", spec);
    br.setFacetSpec("id", spec);
    br.setFacetSpec("make", spec);
    br.setFacetSpec("type", spec);
    
    BrowseResult result = null;
    BoboBrowser boboBrowser=null;
    int expectedHitNum = 100;
    int expectedPagedHitNum = 10;
    int expectedFacetNum = 4; 
    int expectedFacetValueNum_id = 100; 
    int expectedFacetValueNum_make = 4;
    int expectedFacetValueNum_color = 5;
    int expectedFacetValueNum_type = 2;
    
    try {
      Directory ramIndexDir = createIndex();
      IndexReader srcReader=IndexReader.open(ramIndexDir,true);
      boboBrowser = new BoboBrowser(BoboIndexReader.getInstance(srcReader,_facetHandlers, null));
      result = boboBrowser.browse(br);
      
      assertEquals(expectedHitNum,result.getNumHits());
      assertEquals(expectedPagedHitNum,result.getHits().length);
      int facetNum = result.getFacetMap().entrySet().size();
      assertEquals(expectedFacetNum,facetNum);
     
      int facetValueNum;
      for(Entry<String, FacetAccessible> entry: result.getFacetMap().entrySet())
      {
        if(entry.getKey().equals("id"))
        {
          facetValueNum = entry.getValue().getFacets().size();
          assertEquals(facetValueNum, expectedFacetValueNum_id);
        }
        else if(entry.getKey().equals("make"))
        {
           facetValueNum = entry.getValue().getFacets().size();
          assertEquals(entry.getValue().getFacets().size(), expectedFacetValueNum_make);
        }
        else if(entry.getKey().equals("color"))
        {
           facetValueNum = entry.getValue().getFacets().size();
          assertEquals(entry.getValue().getFacets().size(), expectedFacetValueNum_color);
        }
        else //type
        {
           facetValueNum = entry.getValue().getFacets().size();
           assertEquals(entry.getValue().getFacets().size(), expectedFacetValueNum_type);
        }
      }
      
    } catch (BrowseException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    catch(IOException ioe){
      fail(ioe.getMessage());
    }
    finally{
      if (boboBrowser!=null){
        try {
          if(result!=null) result.close();
          boboBrowser.close();
        } catch (IOException e) {
          fail(e.getMessage());
        }
      }
    }

  }  
  
  public void testFacetSpecMaxLessThanZeroWithSingleSelection() throws Exception{
    BrowseRequest br=new BrowseRequest();
    br.setCount(10);
    br.setOffset(0);

    BrowseSelection colorSel=new BrowseSelection("color");
    colorSel.addValue("red");
    br.addSelection(colorSel); 
    
    FacetSpec spec=new FacetSpec();
    spec.setExpandSelection(true);
    spec.setOrderBy(FacetSortSpec.OrderValueAsc);
    spec.setMaxCount(-1);
    spec.setMinHitCount(1);
    
    br.setFacetSpec("color", spec);
    br.setFacetSpec("id", spec);
    br.setFacetSpec("make", spec);
    br.setFacetSpec("type", spec);
    
    BrowseResult result = null;
    BoboBrowser boboBrowser=null;
    int expectedHitNum = 60;
    int expectedPagedHitNum = 10;
    int expectedFacetNum = 4; 
    int expectedFacetValueNum_id = 60; 
    int expectedFacetValueNum_make = 3;
    int expectedFacetValueNum_color = 5;
    int expectedFacetValueNum_type = 2;
    
    try {
      Directory ramIndexDir = createIndex();
      IndexReader srcReader=IndexReader.open(ramIndexDir,true);
      boboBrowser = new BoboBrowser(BoboIndexReader.getInstance(srcReader,_facetHandlers, null));
      result = boboBrowser.browse(br);
      
      assertEquals(expectedHitNum,result.getNumHits());
      assertEquals(expectedPagedHitNum,result.getHits().length);
      int facetNum = result.getFacetMap().entrySet().size();
      assertEquals(expectedFacetNum,facetNum);
     
      int facetValueNum;
      for(Entry<String, FacetAccessible> entry: result.getFacetMap().entrySet())
      {
        if(entry.getKey().equals("id"))
        {
          facetValueNum = entry.getValue().getFacets().size();
          assertEquals(facetValueNum, expectedFacetValueNum_id);
        }
        else if(entry.getKey().equals("make"))
        {
           facetValueNum = entry.getValue().getFacets().size();
          assertEquals(entry.getValue().getFacets().size(), expectedFacetValueNum_make);
        }
        else if(entry.getKey().equals("color"))
        {
           facetValueNum = entry.getValue().getFacets().size();
          assertEquals(entry.getValue().getFacets().size(), expectedFacetValueNum_color);
        }
        else //type
        {
           facetValueNum = entry.getValue().getFacets().size();
           assertEquals(entry.getValue().getFacets().size(), expectedFacetValueNum_type);
        }
      }
      
    } catch (BrowseException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    catch(IOException ioe){
      fail(ioe.getMessage());
    }
    finally{
      if (boboBrowser!=null){
        try {
          if(result!=null) result.close();
          boboBrowser.close();
        } catch (IOException e) {
          fail(e.getMessage());
        }
      }
    }
  }  
  
  public void testFacetSpecMaxLessThanZeroWithMultipleSelection() throws Exception{
    BrowseRequest br=new BrowseRequest();
    br.setCount(10);
    br.setOffset(0);

    BrowseSelection makeSel=new BrowseSelection("type");
    makeSel.addValue("ce");
    br.addSelection(makeSel); 
    
    BrowseSelection colorSel=new BrowseSelection("color");
    colorSel.addValue("red");
    br.addSelection(colorSel); 
    
    FacetSpec spec=new FacetSpec();
    spec.setExpandSelection(true);
    spec.setOrderBy(FacetSortSpec.OrderValueAsc);
    spec.setMaxCount(-1);
    spec.setMinHitCount(1);
    
    br.setFacetSpec("make", spec);
    br.setFacetSpec("color", spec);
    br.setFacetSpec("id", spec);
    br.setFacetSpec("type", spec);
    
    BrowseResult result = null;
    BoboBrowser boboBrowser=null;
    int expectedHitNum = 40;
    int expectedPagedHitNum = 10;
    int expectedFacetNum = 4; 
    int expectedFacetValueNum_id = 40; 
    int expectedFacetValueNum_make = 1; // the number of make when "color=red" && "type=ce" is 1: avalon
    int expectedFacetValueNum_color = 2;  // the number of color when "type=ce" is 2: orange and red
    int expectedFacetValueNum_type = 2;// the number of types when "color=red" is 2: le , ce
    
    try {
      Directory ramIndexDir = createIndex();
      IndexReader srcReader=IndexReader.open(ramIndexDir,true);
      boboBrowser = new BoboBrowser(BoboIndexReader.getInstance(srcReader,_facetHandlers, null));
      result = boboBrowser.browse(br);
      
      assertEquals(expectedHitNum,result.getNumHits());
      assertEquals(expectedPagedHitNum,result.getHits().length);
      int facetNum = result.getFacetMap().entrySet().size();
      assertEquals(expectedFacetNum,facetNum);
     
      int facetValueNum;
      for(Entry<String, FacetAccessible> entry: result.getFacetMap().entrySet())
      {
        if(entry.getKey().equals("id"))
        {
          facetValueNum = entry.getValue().getFacets().size();
          assertEquals(facetValueNum, expectedFacetValueNum_id);
        }
        else if(entry.getKey().equals("make"))
        {
           facetValueNum = entry.getValue().getFacets().size();
           assertEquals(entry.getValue().getFacets().size(), expectedFacetValueNum_make);
        }
        else if(entry.getKey().equals("color"))
        {
           facetValueNum = entry.getValue().getFacets().size();
           assertEquals(entry.getValue().getFacets().size(), expectedFacetValueNum_color);
        }
        else //type
        {
           facetValueNum = entry.getValue().getFacets().size();
           assertEquals(entry.getValue().getFacets().size(), expectedFacetValueNum_type);
        }
        
      }
      
    } catch (BrowseException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    catch(IOException ioe){
      fail(ioe.getMessage());
    }
    finally{
      if (boboBrowser!=null){
        try {
          if(result!=null) result.close();
          boboBrowser.close();
        } catch (IOException e) {
          fail(e.getMessage());
        }
      }
    }

  }  
}