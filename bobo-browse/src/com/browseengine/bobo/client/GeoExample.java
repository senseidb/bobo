/**
 * 
 */
package com.browseengine.bobo.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.browseengine.bobo.facets.impl.GeoSimpleFacetHandler;

/**
 * @author nnarkhed
 *
 */
public class GeoExample extends GeoSimpleFacetHandler{
	private static Logger logger = Logger.getLogger(GeoSimpleFacetHandler.class);

	public GeoExample(String name, String latFacetName, String longFacetName) {
		super(name, latFacetName, longFacetName);
	}

	@Override
	protected String buildLatRangeString(String val) {
		// val string looks like <latitude, longitude, radius>
		int index = val.indexOf('<');
		int index2 = val.indexOf('>');
		
		float latitude, radius, latStart, latEnd;
		try{
			String range = val.substring(index+1, index2).trim();
			String[] values = range.split(",");

			// values[0] is latitude, values[1] is longitude, values[2] is radius
			latitude = Float.parseFloat(values[0]);
			radius = Float.parseFloat(values[2]);
			
			latStart = latitude - radius;
			latEnd = latitude + radius;
		}
		catch(RuntimeException re){
			logger.error("problem parsing range string: " + val + ":" + re.getMessage(),re);
			throw re;
		}		
		return "[" + String.valueOf(latStart) + " TO " + String.valueOf(latEnd) + "]";
	}
	
	@Override
	protected String buildLongRangeString(String val) {
		// val string looks like <latitude, longitude, radius>
		int index = val.indexOf('<');
		int index2 = val.indexOf('>');
		
		float longitude, radius, longStart, longEnd;
		try{
			String range = val.substring(index+1, index2).trim();
			String[] values = range.split(",");
			// values[0] is latitude, values[1] is longitude, values[2] is radius
			longitude = Float.parseFloat(values[1]);
			radius = Float.parseFloat(values[2]);
			
			longStart = longitude - radius;
			longEnd = longitude + radius;			
		}
		catch(RuntimeException re){
			logger.error("problem parsing range string: " + val + ":" + re.getMessage(),re);
			throw re;
		}		
		return "[" + String.valueOf(longStart) + " TO " + String.valueOf(longEnd) + "]";
	}

	@Override
	protected List<String> buildAllRangeStrings(String[] values) {
		if(values == null)  return Collections.EMPTY_LIST;
		List<String> ranges = new ArrayList<String>(values.length);
		String[] range = null;
		for(String value: values) {
			ranges.add(value);
		}
		return ranges;
	}

	@Override
	protected String getValueFromRangeString(String rangeString) {
		return null;
	}

	@Override
	public GeoSimpleFacetHandler newInstance() {
		return null;
	}

}
