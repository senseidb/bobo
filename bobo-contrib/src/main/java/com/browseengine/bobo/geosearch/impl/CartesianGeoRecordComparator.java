package com.browseengine.bobo.geosearch.impl;

import java.util.Comparator;

import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;

/**
 * 
 * Basic comparator for CartesianGeoRecords
 * 
 * @author gcooney
 *
 */
public class CartesianGeoRecordComparator implements Comparator<CartesianGeoRecord> {
    @Override
    public int compare(CartesianGeoRecord recordFirst, CartesianGeoRecord recordSecond) {
        long highdiff = recordFirst.highOrder - recordSecond.highOrder;
        if(highdiff > 0) {
            return 1;
        }
        if (highdiff < 0) {
            return -1;
        }
        long lowdiff = recordFirst.lowOrder - recordSecond.lowOrder;
        if(lowdiff > 0) {
            return 1;
        }
        if (lowdiff < 0) {
            return -1;
        }
        
        if (recordFirst.filterByte > recordSecond.filterByte) {
            return 1; 
        } else if (recordFirst.filterByte < recordSecond.filterByte) {
            return -1;
        }
        
        return 0;
    }

}
