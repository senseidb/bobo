package com.browseengine.bobo.service.dataprovider;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;

import proj.zoie.api.indexing.ZoieIndexable;
import proj.zoie.api.indexing.ZoieIndexableInterpreter;

public class PropertiesIndexDataInterpreter implements ZoieIndexableInterpreter<PropertiesData>
{

  public ZoieIndexable interpret(PropertiesData props)
  {
    return new PropertiesIndexable(props);
  }
  
  private static class PropertiesIndexable implements ZoieIndexable
  {
    private final PropertiesData _prop;
    private static final String CONTENTS_FIELD_NAME = "contents";
    public PropertiesIndexable(PropertiesData prop)
    {
      _prop = prop;
    }
    
    public Document buildDocument()
    {
      HashMap<String,String> data = _prop.getData();
      if (data!=null)
      {
        Document doc = new Document();
        Set<String> keySet = data.keySet();
        Iterator<String> keyIter = keySet.iterator();
        StringBuffer contentBuffer = new StringBuffer();
        while(keyIter.hasNext())
        {
          String propName = keyIter.next();
          String propVal = data.get(propName);
          contentBuffer.append(propVal).append(" ");
          String[] valList = propVal.split(",");
          for (String val : valList)
          {
            Field f = new Field(propName,val,Store.NO,Index.NOT_ANALYZED_NO_NORMS);
            f.setOmitTermFreqAndPositions(true);
            doc.add(f);
          }
          doc.add(new Field(CONTENTS_FIELD_NAME,contentBuffer.toString(),Store.NO,Index.ANALYZED));
        }
        return doc;
      }
      else
      {
        return null;
      }
      
    }

    public long getUID()
    {
      return _prop.getID();
    }

    public boolean isDeleted()
    {
      return _prop.getData() == null;
    }

    public boolean isSkip()
    {
      return _prop.isSkip();
    }

	public IndexingReq[] buildIndexingReqs() {
		Document doc = buildDocument();
		IndexingReq req = new IndexingReq(doc);
		return new IndexingReq[]{req};
	}

	public Document[] buildDocuments() {
		return new Document[]{buildDocument()};
	}
    
  }

  public ZoieIndexable convertAndInterpret(PropertiesData props) {
	return new PropertiesIndexable(props);
  }

}
