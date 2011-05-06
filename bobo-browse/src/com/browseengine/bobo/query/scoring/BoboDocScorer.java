package com.browseengine.bobo.query.scoring;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.search.Explanation;

public abstract class BoboDocScorer {
	protected final FacetTermScoringFunction _function;
	protected final float[] _boostList;
	
	public BoboDocScorer(FacetTermScoringFunction scoreFunction,float[] boostList){
		_function = scoreFunction;
		_boostList = boostList;
	}
	
    public abstract float score(int docid);
    
    abstract public Explanation explain(int docid);
    
    public static float[] buildBoostList(List<String> valArray,Map<String,Float> boostMap){
    	float[] boostList = new float[valArray.size()];
    	Arrays.fill(boostList, 0.0f);
    	if (boostMap!=null && boostMap.size()>0){
    		Iterator<Entry<String,Float>> iter = boostMap.entrySet().iterator();
    		while(iter.hasNext()){
    			Entry<String,Float> entry = iter.next();
    			int index = valArray.indexOf(entry.getKey());
    			if (index>=0){
    				Float fval = entry.getValue();
    				if (fval!=null){
    				  boostList[index] = fval.floatValue();
    				}
    			}
    		}
    	}
    	return boostList;
    }
}
