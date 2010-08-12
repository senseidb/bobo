package com.browseengine.solr;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.IndexReaderFactory;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BoboIndexReader.WorkArea;

public class BoboIndexReaderFactory extends IndexReaderFactory {
	public void init(NamedList conf) {
	}

	@Override
	public IndexReader newReader(Directory indexDir, boolean readOnly)
			throws IOException {
		IndexReader reader=IndexReader.open(indexDir,null,readOnly,termInfosIndexDivisor);
		BoboIndexReader boboReader=null;
		try{
			WorkArea workArea = new WorkArea();
			workArea.put(getClass().getClassLoader());
			boboReader=BoboIndexReader.getInstance(reader,workArea);
			return boboReader;
		}
		catch(IOException e){
			if (reader!=null){
				reader.close();
			}
			throw e;
		}
	}
}
