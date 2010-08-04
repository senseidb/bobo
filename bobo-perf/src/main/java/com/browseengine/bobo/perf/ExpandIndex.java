package com.browseengine.bobo.perf;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class ExpandIndex {
  public static void main(String[] args) throws IOException {
	System.out.println("Usage: <srcidx> <targetidx> <numReplications> <numSegments>");
	File srcIdx = new File(args[0]);
	File targetIndx = new File(args[1]);
	
	int numReps = Integer.parseInt(args[2]);
	int numsegs = Integer.parseInt(args[3]);
	
	
	Directory srcDir = FSDirectory.open(srcIdx);
	Directory targetDir = FSDirectory.open(targetIndx);
	int timesReplicatePerSeg = numReps/numsegs;
	IndexWriter writer = new IndexWriter(targetDir,new StandardAnalyzer(Version.LUCENE_CURRENT),MaxFieldLength.UNLIMITED);
	writer.setMaxMergeDocs(Integer.MAX_VALUE);
	writer.setMergeFactor(Integer.MAX_VALUE);
	
	System.out.println("num segments: "+numsegs);
	System.out.println("num reps per segment: "+timesReplicatePerSeg);
	for (int i=0;i<numsegs;++i){
		RAMDirectory ramDir = new RAMDirectory();
		IndexWriter subWriter = new IndexWriter(ramDir,new StandardAnalyzer(Version.LUCENE_CURRENT),MaxFieldLength.UNLIMITED);
		Directory[] multiplier = new Directory[timesReplicatePerSeg];
		for (int k=0;k<timesReplicatePerSeg;++k){
			multiplier[k]=new RAMDirectory(srcDir);
		}
		subWriter.addIndexesNoOptimize(multiplier);
		subWriter.optimize();
		subWriter.close();
	
		writer.addIndexesNoOptimize(new Directory[]{ramDir});
		System.out.println("segment: "+i+" created");
	}
	
	writer.close();
	
	File boboSpring = new File(srcIdx,"bobo.spring");
	if (boboSpring.exists()){
		String cmd = "cp "+boboSpring.getAbsolutePath()+" "+targetIndx.getAbsolutePath();
		System.out.println("executing: "+cmd);
		Runtime.getRuntime().exec(cmd);
	}
	
	SegmentInfos infos=new SegmentInfos();
	infos.read(targetDir);
	System.out.println("result segment count: "+infos.size());
	
	IndexReader r = IndexReader.open(targetDir,true);
	System.out.println("num docs: "+r.numDocs());
	r.close();
  }
}
