package com.browseengine.bobo.geosearch.solo.impl;

import java.util.Comparator;

import com.browseengine.bobo.geosearch.solo.bo.IDGeoRecord;

/**
 * 
 * @author gcooney
 *
 */
public class IDGeoRecordComparator implements Comparator<IDGeoRecord> {

    @Override
    public int compare(IDGeoRecord idGeoRecord1, IDGeoRecord idGeoRecord2) {
        long diff = idGeoRecord1.highOrder - idGeoRecord2.highOrder;
        if (diff > 0) {
            return 1;
        }
        if (diff < 0) {
            return -1;
        }
        int idiff = idGeoRecord1.lowOrder - idGeoRecord2.lowOrder;
        if(idiff > 0) {
            return 1;
        }
        if (idiff < 0) {
            return -1;
        }
        
        for (int i = 0; i < idGeoRecord1.id.length && i < idGeoRecord2.id.length; i++) {
            if (idGeoRecord1.id[i] > idGeoRecord2.id[i]) {
                return 1;
            } else if (idGeoRecord1.id[i] < idGeoRecord2.id[i]) {
                return -1;
            }
        }

        if (idGeoRecord1.id.length > idGeoRecord2.id.length) {
            return 1;
        } else if (idGeoRecord1.id.length < idGeoRecord2.id.length) {
            return -1;
        }
        
        return 0;
    }
 
}
