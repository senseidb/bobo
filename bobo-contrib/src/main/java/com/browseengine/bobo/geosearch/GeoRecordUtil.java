package com.browseengine.bobo.geosearch;

/**
 * 
 * @author gcooney
 *
 */
public class GeoRecordUtil {
    public static final int MAX_DIGITS_INT = ndigits(Integer.MAX_VALUE);
    public static final int MAX_DIGITS_LONG = ndigits(Long.MAX_VALUE);
    
    public static String lpad(int maxDigits, long val) {
        int ndigits = ndigits(val);
        int pad = maxDigits - ndigits;
        StringBuilder buf = new StringBuilder();
        while (pad > 0) {
            buf.append('0');
            pad--;
        }
        buf.append(val);
        return buf.toString();
    }
    
    private static int ndigits(long val) {
        val = Long.highestOneBit(val);
        int i = 0;
        while (val > 0) {
            i++;
            val /= 10;
        }
        return i;
    }
}
