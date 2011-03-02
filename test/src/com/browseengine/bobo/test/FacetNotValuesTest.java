package com.browseengine.bobo.test;


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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

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
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.impl.SimpleFacetHandler;
import com.browseengine.bobo.index.BoboIndexer;
import com.browseengine.bobo.index.digest.DataDigester;

public class FacetNotValuesTest extends TestCase {
  static Logger log = Logger.getLogger(BoboTestCase.class);
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
  
  public FacetNotValuesTest(String testName){
    super(testName);
    _facetHandlers = createFacetHandlers();
    
    _documentSize = 10;
    String confdir = System.getProperty("conf.dir");
    if (confdir == null) confdir ="./resource";
    org.apache.log4j.PropertyConfigurator.configure(confdir+"/log4j.properties");
  }
  
  public Document[] createData(){
      ArrayList<Document> dataList=new ArrayList<Document>();
      for(int i=0; i<_documentSize; i++)
      {
        String color = (i%2 == 0) ? "red" : "green";
        String ID = Integer.toString(i);
        Document d=new Document();
        d.add(new Field("id",ID,Field.Store.YES,Index.NOT_ANALYZED_NO_NORMS));
        d.add(new Field("color",color,Field.Store.YES,Index.NOT_ANALYZED_NO_NORMS));
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
    facetHandlers.add(new SimpleFacetHandler("color"));
    return facetHandlers;
  }
  
  
  public void testNotValuesForSimpleFacetHandler() throws Exception{
    BrowseRequest br=new BrowseRequest();
    br.setCount(20);
    br.setOffset(0);

    BrowseSelection colorSel=new BrowseSelection("color");
    colorSel.addValue("red");
    br.addSelection(colorSel); 

    BrowseSelection idSel=new BrowseSelection("id");
    idSel.addNotValue("0");
    br.addSelection(idSel);

    BrowseResult result = null;
    BoboBrowser boboBrowser=null;
    int expectedHitNum = (_documentSize/2) - 1;
    try {
      Directory ramIndexDir = createIndex();
      IndexReader srcReader=IndexReader.open(ramIndexDir,true);
      boboBrowser = new BoboBrowser(BoboIndexReader.getInstance(srcReader,_facetHandlers, null));
      result = boboBrowser.browse(br);
      
      assertEquals(expectedHitNum,result.getNumHits());

      StringBuilder buffer=new StringBuilder();
      BrowseHit[] hits=result.getHits();
     
      for (int i=0;i<hits.length;++i)
      {
        int expectedID = (i+1)*2;
        assertEquals(expectedID, Integer.parseInt(hits[i].getField("id")));
        if (i!=0){
          buffer.append('\n');
        }
        buffer.append("id=" + hits[i].getField("id") + "," + "color=" + hits[i].getField("color"));
      }
      log.info(buffer.toString());

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
   
  public static void main(String[] args)throws Exception {
    FacetNotValuesTest test=new FacetNotValuesTest("testNotValues");
    test.setUp();
    test.testNotValuesForSimpleFacetHandler();
    test.tearDown();
  }
}

