package com.browseengine.bobo.service;

import java.util.Comparator;

import com.browseengine.bobo.api.BrowseHit;

public class HitCompareMulti implements Comparator<BrowseHit>
{
	protected Comparator<BrowseHit>[] m_hcmp;

	public HitCompareMulti(Comparator<BrowseHit>[] hcmp)
	{
		m_hcmp = hcmp;
	}

	// HitCompare
	public int compare(BrowseHit h1, BrowseHit h2)
	{
		int retVal=0;
		for (int i=0;i<m_hcmp.length;++i){
			retVal=m_hcmp[i].compare(h1, h2);
			if (retVal!=0) break;
		}
		return retVal;
	}
}
