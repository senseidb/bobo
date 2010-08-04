package com.browseengine.solr;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.IndexReaderFactory;

import com.browseengine.bobo.api.BoboIndexReader;

public class BoboIndexReaderFactory extends IndexReaderFactory {
	public void init(NamedList conf) {
	}

	@Override
	public IndexReader newReader(Directory indexDir, boolean readOnly)
			throws IOException {
		IndexReader reader=IndexReader.open(indexDir,readOnly);
		BoboIndexReader boboReader=null;
		try{
			boboReader=BoboIndexReader.getInstance(reader);
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
