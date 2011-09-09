/**
 * 
 */
package com.browseengine.bobo.geosearch.impl;

import com.browseengine.bobo.geosearch.IDeletedDocs;

/**
 * Retrieves isDeleted for relative space.
 * Retrieves isDeleted for relative <tt>docidWithinSegment</tt>s by using the 
 * global <tt>wholeIndexDeletedDocs</tt> and <tt>firstDocIdInSegment</tt> 
 * offsets.
 * 
 * @author Ken McCracken
 *
 */
public class DeletedDocs implements IDeletedDocs {
    
    private IDeletedDocs wholeIndexDeletedDocs;
    
    private int firstDocIdInSegment;
    
    public DeletedDocs(IDeletedDocs wholeIndexDeletedDocs, int firstDocIdInSegment) {
        this.wholeIndexDeletedDocs = wholeIndexDeletedDocs;
        this.firstDocIdInSegment =  firstDocIdInSegment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDeleted(int docidWithinSegment) {
        int globalDocId = docidWithinSegment + firstDocIdInSegment;
        return wholeIndexDeletedDocs.isDeleted(globalDocId);
    }
    

}
