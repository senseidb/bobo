package org.apache.lucene.index;

import java.util.List;

import org.apache.lucene.index.MergeState.DocMap;

public class DocMapExposer extends DocMap {
    
    private List<Integer> docMap;
    private int numDeletedDocs;

    public DocMapExposer(List<Integer> docMap, int numDeletedDocs) {
        this.docMap = docMap;
        this.numDeletedDocs = numDeletedDocs;
        
    }

    @Override
    public int get(int docID) {
        return docMap.get(docID);
    }

    @Override
    public int maxDoc() {
        return docMap.size();
    }

    @Override
    public int numDeletedDocs() {
        return numDeletedDocs;
    }
    
    public void delete(int index) {
        docMap.set(index, -1);
        
        for (int i = index + 1; i < docMap.size(); i++) {
            int currentValue = docMap.get(i);
            if (currentValue > 0) {
                docMap.set(i, currentValue - 1);
            }
        }
    }
}
