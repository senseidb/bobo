package com.browseengine.bobo.geosearch.solo.bo;

public class IndexTooLargeException extends Exception {
    private static final long serialVersionUID = 1L;

    public IndexTooLargeException(String indexName, int indexSize, int maxSize) {
        super("This index is larger than the maximum allowed index size and will not be flushed to disk" +
        		".  IndexName:" + indexName + ";  Actual index size: " + indexSize
        		+ ";  Max index size: " + maxSize);
    }
}
