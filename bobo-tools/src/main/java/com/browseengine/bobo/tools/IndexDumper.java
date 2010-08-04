package com.browseengine.bobo.tools;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.facets.FacetHandler;


public class IndexDumper {
	private BoboIndexReader _reader;
	private int curr_docid=0;
	private int maxdoc;
	public IndexDumper(File idxDir) throws IOException{
		IndexReader idxReader=IndexReader.open(FSDirectory.open(idxDir),true);
		if (idxReader!=null){
			try{
				_reader=BoboIndexReader.getInstance(idxReader);
				maxdoc=_reader.maxDoc();
			}
			catch(Exception e){
				e.printStackTrace();
				maxdoc=0;
				idxReader.close();
			}
		}
	}
	
	public int numDocs(){
		return _reader.numDocs();
	}
	
	public Document next() throws IOException{
		while(_reader.isDeleted(curr_docid) && curr_docid<maxdoc){
			curr_docid++;
		}
		if (curr_docid<maxdoc){
			Document doc=_reader.document(curr_docid);
			curr_docid++;
			return doc;
		}
		else{
			return null;
		}
	}
	
	public void close(){
		if (_reader!=null){
			try {
				_reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			finally{
				_reader=null;
			}
		}
	}
	
	protected void finalize(){
		close();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		File idxDir=new File(args[0]);
		File outDir=new File(args[1]);
		
		FileWriter writer=null;
		IndexDumper dumper=new IndexDumper(idxDir);

		BoboIndexReader idxReader=dumper._reader;
		try{
			outDir.mkdirs();
			File outFile=new File(outDir,"dataout.txt");
			if (!outFile.exists()) outFile.createNewFile();
			
			writer=new FileWriter(outFile);
			BufferedWriter bwriter=new BufferedWriter(writer);
			dumper=new IndexDumper(idxDir);
			Set<String> fields=idxReader.getFacetNames();
			
			for (int k=0;k<idxReader.maxDoc();++k){
				for (String field : fields){
					FacetHandler facetHandler = idxReader.getFacetHandler(field);
					if (facetHandler!=null){
						String[] f=facetHandler.getFieldValues(idxReader,k);
						StringBuilder buffer=new StringBuilder();
						buffer.append(field).append(':');
						for (int l=0;l<f.length;++l){
							if (l>0){
								buffer.append(',');
							}
							buffer.append(f[l]);
						}
						buffer.append('\n');
						bwriter.write(buffer.toString());
					}
					
				}

				bwriter.write("<EOD>\n");
				bwriter.flush();
			}
		}
		finally{
			try{
				if (dumper!=null){
					dumper.close();
				}
			}
			finally{
				if (writer!=null){
					writer.close();
				}
			}
		}
	}

}
