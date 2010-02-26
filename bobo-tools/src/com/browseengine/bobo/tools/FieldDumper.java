package com.browseengine.bobo.tools;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.browseengine.bobo.api.BoboIndexReader;

public class FieldDumper
{
  private final Directory _idxDir;
  private final File _outDir;
  
  public FieldDumper(Directory idxDir,File outDir)
  {
    _idxDir = idxDir;
    _outDir = outDir;
  }
  
  public void doit() throws IOException
  {
    _outDir.mkdirs();
    
    File outfile = new File(_outDir,"facetvals.txt");
    BufferedOutputStream ostream = new BufferedOutputStream(new FileOutputStream(outfile));
    PrintWriter writer = new PrintWriter(new OutputStreamWriter(ostream,"UTF-8"));
    
    IndexReader reader=IndexReader.open(_idxDir);
    BoboIndexReader boboReader = BoboIndexReader.getInstance(reader);
    
    Set<String> fieldNames = boboReader.getFacetNames();
    for (String fieldName : fieldNames)
    {
      TermEnum te = reader.terms(new Term(fieldName,""));
      while(te.next())
      {
        Term t = te.term();
        if (!fieldName.equals(t.field())) break;
        writer.println(t.field()+":"+t.text());
      }
      te.close();
    }
    writer.flush();
    writer.close();
    
    boboReader.close();
  }
  
  /**
   * @param args
   */
  public static void main(String[] args) throws Exception
  {
    Directory idxDir = FSDirectory.open(new File("/Users/jwang/proj/bobo-trunk/cardata/cartag"));
    File outDir = new File("/Users/jwang/proj/bobo-trunk/cardata");
    FieldDumper dumper = new FieldDumper(idxDir,outDir);
    dumper.doit();
  }

}
