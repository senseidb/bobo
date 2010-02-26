package com.browseengine.bobo.facets.data;

import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Class supported:
 * <ul>
 *   <li> {@link Integer} </li>
 *   <li> {@link Float} </li>
 *   <li> {@link Character} </li>
 *   <li> {@link Double} </li>
 *   <li> {@link Short} </li>
 *   <li> {@link Long} </li>
 *   <li> {@link Date} </li>
 * </ul>
 * 
 * Autoboxing: primitive types corresponding classes above are supported.
 */
public class PredefinedTermListFactory implements TermListFactory {
	private static Map<Class<?>,Class<? extends TermValueList>> supportedTypes= new HashMap<Class<?>,Class<? extends TermValueList>>();
	
	static
	{
		supportedTypes.put(int.class,TermIntList.class);
		supportedTypes.put(float.class,TermFloatList.class);
		supportedTypes.put(char.class,TermCharList.class);
		supportedTypes.put(double.class,TermDoubleList.class);
		supportedTypes.put(short.class,TermShortList.class);
		supportedTypes.put(long.class,TermLongList.class);
		supportedTypes.put(Integer.class,TermIntList.class);
		supportedTypes.put(Float.class,TermFloatList.class);
		supportedTypes.put(Character.class,TermCharList.class);
		supportedTypes.put(Double.class,TermDoubleList.class);
		supportedTypes.put(Short.class,TermShortList.class);
		supportedTypes.put(Long.class,TermLongList.class);
		supportedTypes.put(Date.class,TermDateList.class);
	}
	
	private final Class<?> _cls;
	private final String _format;
	public PredefinedTermListFactory(Class<?> cls,String format)
	{
		if (supportedTypes.get(cls)==null)
		{
			throw new IllegalArgumentException("Class "+cls+" not defined.");
		}
		_cls=cls;
		_format=format;
	}
	
	public PredefinedTermListFactory(Class<?> cls)
	{
		this(cls,null);
	}
	
	public TermValueList createTermList() {
		Class<? extends TermValueList> listClass = supportedTypes.get(_cls);
		if (TermCharList.class.equals(listClass))  // we treat char type separate as it does not have a format string
		{
			return new TermCharList();
		}
		else
		{
		    try {
				Constructor<? extends TermValueList> constructor=listClass.getConstructor(String.class);
				return constructor.newInstance(_format);
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage());
			} 
		}
	}
}
