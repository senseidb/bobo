package com.browseengine.bobo.geosearch.index.bo;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.EmptyTokenStream;
import org.apache.lucene.index.FieldInfo.DocValuesType;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.util.BytesRef;

/**
 * 
 * @author Geoff Cooney
 * @author Shane Detsch
 *
 */
public class GeoCoordinateField implements IndexableField {
    
    private final GeoCoordinateFieldType fieldType = new GeoCoordinateFieldType();
    private final String fieldName;
    private GeoCoordinate geoCoordinate;
    private float boost = 1.0f;
    
    public GeoCoordinateField(String fieldName, GeoCoordinate geoCoordinate) {
        this.fieldName = fieldName;
        this.geoCoordinate = geoCoordinate;
    }
    
    public GeoCoordinate getGeoCoordinate() {
        return geoCoordinate;
    }
    
    public void setGeoCoordinate(GeoCoordinate geoCoordinate) {
        this.geoCoordinate = geoCoordinate;
    }
    
    @Override
    public String stringValue() {
        return geoCoordinate.getLatitude() + ", " + geoCoordinate.getLongitude();
    }

    /** 
     *  Returns always <code>null</code> for GeoCoordinate fields 
     *  Use getGeoCoordinate to retrieve results instead. 
     */
    @Override
    public Reader readerValue() {
        return null;
    }

    @Override
    public String name() {
        return fieldName;
    }

    @Override
    public IndexableFieldType fieldType() {
        return fieldType;
    }

    @Override
    public float boost() {
        return boost;
    }

    @Override
    public BytesRef binaryValue() {
        return null;
    }

    @Override
    public Number numericValue() {
        return null;
    }

    @Override
    public TokenStream tokenStream(Analyzer analyzer) throws IOException {
        return new EmptyTokenStream();
    }

    private class GeoCoordinateFieldType implements IndexableFieldType {

        @Override
        public boolean indexed() {
            return true;
        }

        @Override
        public boolean stored() {
            return false;
        }

        @Override
        public boolean tokenized() {
            return false;
        }

        @Override
        public boolean storeTermVectors() {
            return false;
        }

        @Override
        public boolean storeTermVectorOffsets() {
            return false;
        }

        @Override
        public boolean storeTermVectorPositions() {
            return false;
        }

        @Override
        public boolean storeTermVectorPayloads() {
            return false;
        }

        @Override
        public boolean omitNorms() {
            return true;
        }

        @Override
        public IndexOptions indexOptions() {
            return IndexOptions.DOCS_ONLY;
        }

        @Override
        public DocValuesType docValueType() {
            return null;
        }

    }
    
}
