package com.browseengine.bobo.mapred;

import com.browseengine.bobo.api.BoboIndexReader;

public interface BoboMapFunctionWrapper {
	public void mapFullIndexReader(BoboIndexReader reader);
	public void mapSingleDocument(int docId, BoboIndexReader reader);
	public void finalizeSegment(BoboIndexReader reader);
	public void finalizePartition();	
	public MapReduceResult getResult();
}
