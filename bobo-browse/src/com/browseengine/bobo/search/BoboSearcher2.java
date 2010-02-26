package com.browseengine.bobo.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.ReaderUtil;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetCountCollectorSource;

public class BoboSearcher2 extends IndexSearcher{
	protected List<FacetHitCollector> _facetCollectors;
	protected BoboIndexReader[] _subReaders;
	protected int[] _docStarts;
	
    public BoboSearcher2(BoboIndexReader reader)
    {
        super(reader);
        _facetCollectors = new LinkedList<FacetHitCollector>();
        List<IndexReader> readerList = new ArrayList<IndexReader>();
        ReaderUtil.gatherSubReaders(readerList, reader);
        _subReaders = (BoboIndexReader[])readerList.toArray(new BoboIndexReader[readerList.size()]);
        _docStarts = new int[_subReaders.length];
        int maxDoc = 0;
        for (int i=0;i<_subReaders.length;++i){
        	_docStarts[i]=maxDoc;
        	maxDoc += _subReaders[i].maxDoc();
        }
    }
    
    public void setFacetHitCollectorList(List<FacetHitCollector> facetHitCollectors){
		if (facetHitCollectors != null){
			_facetCollectors = facetHitCollectors;
		}
	}

    abstract static class FacetValidator
    {
      protected final FacetHitCollector[] _collectors;
      protected final FacetCountCollectorSource[] _countCollectorSources;
      protected final FacetCountCollector[] _countCollectors;
      protected final int _numPostFilters;
      public int _nextTarget;
      
      public FacetValidator(FacetHitCollector[] collectors,FacetCountCollectorSource[] countCollectorSources,int numPostFilters) throws IOException
      {
        _collectors = collectors;
        _countCollectorSources = countCollectorSources;
        _numPostFilters = numPostFilters;
        _countCollectors = new FacetCountCollector[_countCollectorSources.length];
      }
      /**
       * This method validates the doc against any multi-select enabled fields.
       * @param docid
       * @return true if all fields matched
       */
      public abstract boolean validate(final int docid)
        throws IOException;
      
      public void setNextReader(BoboIndexReader reader,int docBase) throws IOException{
    	  for (int i=0;i<_collectors.length;++i){
    		  _collectors[i].setNextReader(reader, docBase);
    		  _countCollectors[i]=_collectors[i]._currentPointers.facetCountCollector;
    	  }
      }
      
    }
    
    private final static class DefaultFacetValidator extends FacetValidator{
    	
    	public DefaultFacetValidator(FacetHitCollector[] collectors,FacetCountCollectorSource[] countCollectors,int numPostFilters) throws IOException{
    		super(collectors,countCollectors,numPostFilters);
    	}
    	 /**
         * This method validates the doc against any multi-select enabled fields.
         * @param docid
         * @return true if all fields matched
         */
    	@Override
        public final boolean validate(final int docid)
          throws IOException
        {
          FacetHitCollector miss = null;
          
          for(int i = 0; i < _numPostFilters; i++)
          {
            FacetHitCollector facetCollector = _collectors[i];
            int sid = facetCollector._currentPointers.doc;
            if(sid!=DocIdSetIterator.NO_MORE_DOCS)
            {
              if(sid == docid) continue; // matched
              
              if(sid < docid)
              {
                DocIdSetIterator iterator = facetCollector._currentPointers.postDocIDSetIterator;
                sid = iterator.advance(docid);
                if(sid!=DocIdSetIterator.NO_MORE_DOCS)
                {
                	facetCollector._currentPointers.doc = sid;
                  if(sid == docid) continue; // matched
                }
                else
                {
                	facetCollector._currentPointers.doc = DocIdSetIterator.NO_MORE_DOCS;
                  
                  // move this to front so that the call can find the failure faster
                  FacetHitCollector tmp = _collectors[0];
                  _collectors[0] = facetCollector;
                  _collectors[i] = tmp;
                }
              }  
            }
            
            if(miss != null)
            {
              // failed because we already have a mismatch
              _nextTarget = (miss._currentPointers.doc < facetCollector._currentPointers.doc ? miss._currentPointers.doc : facetCollector._currentPointers.doc);
              return false;
            }
            miss = facetCollector;
          }
          
          _nextTarget = docid + 1;

          if(miss != null)
          {
            miss._currentPointers.facetCountCollector.collect(docid);
            return false;
          }
          else
          {
            for (FacetCountCollector collector : _countCollectors)
            {
          	 collector.collect(docid);
            }
            return true;
          }
        }
    }
    
    private final static class OnePostFilterFacetValidator extends FacetValidator{
    	private FacetHitCollector _firsttime;
    	OnePostFilterFacetValidator(FacetHitCollector[] collectors,FacetCountCollectorSource[] countCollectors,int numPostFilters) throws IOException{
    		super(collectors,countCollectors,numPostFilters);
    		_firsttime = _collectors[0];
    	}

		@Override
		public final boolean validate(int docid) throws IOException {
			FacetHitCollector miss = null;
			
            RandomAccessDocIdSet set = _firsttime._currentPointers.docidSet;
            if (set!=null && !set.get(docid))
            {
              miss = _firsttime;
            }
            _nextTarget = docid + 1;

            if(miss != null)
            {
              miss._currentPointers.facetCountCollector.collect(docid);
              return false;
            }
            else
            {
              for (FacetCountCollector collector : _countCollectors)
              {
            	 collector.collect(docid);
              }
              return true;
            }
		}
    }
    
    private final static class NoNeedFacetValidator extends FacetValidator{
    	private FacetCountCollector[] collList=null;
    	NoNeedFacetValidator(FacetHitCollector[] collectors,FacetCountCollectorSource[] countCollectors,int numPostFilters) throws IOException{
    		super(collectors,countCollectors,numPostFilters);
    	}
    	
		@Override
		public void setNextReader(BoboIndexReader reader, int docBase)
				throws IOException {
			super.setNextReader(reader, docBase);
			ArrayList<FacetCountCollector> filteredCollectorList = new ArrayList<FacetCountCollector>();
			for (FacetCountCollector collector : _countCollectors){
				if (collector!=null){
					filteredCollectorList.add(collector);
				}
            }
			collList = filteredCollectorList.toArray(new FacetCountCollector[filteredCollectorList.size()]);
		}



		@Override
		public final boolean validate(int docid) throws IOException {
			for (FacetCountCollector collector : collList){
              collector.collect(docid);
            }
            return true;
		}
    	
    }
    
    protected FacetValidator createFacetValidator() throws IOException
    {
    	
    	FacetHitCollector[] collectors = new FacetHitCollector[_facetCollectors.size()];
    	FacetCountCollectorSource[] countCollectors = new FacetCountCollectorSource[collectors.length];
        int numPostFilters;
        int i = 0;
        int j = collectors.length;
        
        for (FacetHitCollector facetCollector : _facetCollectors)
        {
          if (facetCollector._filter != null) 
          {
            collectors[i] = facetCollector;
            countCollectors[i]=facetCollector._facetCountCollectorSource;
            i++;
          }
          else
          {
            j--;
            collectors[j] = facetCollector;
            countCollectors[j] = facetCollector._facetCountCollectorSource;
          }
        }
        numPostFilters = i;

      if(numPostFilters == 0){
        return new NoNeedFacetValidator(collectors,countCollectors,numPostFilters);
      }
      else if (numPostFilters==1){
    	return new OnePostFilterFacetValidator(collectors,countCollectors,numPostFilters);  
      }
      else{
        return new DefaultFacetValidator(collectors,countCollectors,numPostFilters);
      }
    }
    
    @Override
    public void search(Weight weight, Filter filter, Collector collector) throws IOException
    {
      search(weight, filter, collector, 0);
    }
    
    public void search(Weight weight, Filter filter, Collector collector, int start) throws IOException
    {
        final FacetValidator validator = createFacetValidator();
        int target = 0;
        
        if (filter == null)
        {
          for (int i = 0; i < _subReaders.length; i++) { // search each subreader
        	int docStart = start + _docStarts[i];
            collector.setNextReader(_subReaders[i], docStart);
            validator.setNextReader(_subReaders[i], docStart);
            Scorer scorer = weight.scorer(_subReaders[i], true, true);
            if (scorer != null) {
            	collector.setScorer(scorer);
            	target = scorer.nextDoc();
                while(target!=DocIdSetIterator.NO_MORE_DOCS)
                {
                  if(validator.validate(target))
                  {
                	collector.collect(target);
                    target = scorer.nextDoc();
                  }
                  else
                  {
                    target = validator._nextTarget;
                    target = scorer.advance(target);
                  }
                }
            }
          }
          return;
        }

        for (int i = 0; i < _subReaders.length; i++) {
        	DocIdSet filterDocIdSet = filter.getDocIdSet(_subReaders[i]);
        	if (filterDocIdSet == null) return;
        	int docStart = start + _docStarts[i];
        	collector.setNextReader(_subReaders[i], docStart);
        	validator.setNextReader(_subReaders[i], docStart);
            Scorer scorer = weight.scorer(_subReaders[i], true, false);
            if (scorer!=null){
            	collector.setScorer(scorer);
	        	DocIdSetIterator filterDocIdIterator = filterDocIdSet.iterator(); // CHECKME: use ConjunctionScorer here?
	        	
	        	target = filterDocIdIterator.nextDoc();
	            if(target == DocIdSetIterator.NO_MORE_DOCS) return;
	            
	            int doc = -1;
	            while(true)
	            {
	              if(doc < target)
	              {
	            	doc = scorer.advance(target);
	                if(doc == DocIdSetIterator.NO_MORE_DOCS) break;
	              }
	              
	              if(doc == target) // permitted by filter
	              {
	                if(validator.validate(doc))
	                {
	                  collector.collect(doc);
	                  
	                  target = filterDocIdIterator.nextDoc();
	                  if(target == DocIdSetIterator.NO_MORE_DOCS) break;
	                  continue;
	                }
	                else
	                {
	                  // skip to the next possible docid
	                  target = validator._nextTarget;
	                }
	              }
	              else // doc > target
	              {
	                target = doc;
	              }
	              
	              target = filterDocIdIterator.advance(target);
	              if(target == DocIdSetIterator.NO_MORE_DOCS) break;
                }
            }
        }
        
    }
}
