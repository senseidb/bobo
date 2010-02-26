package com.browseengine.bobo.service;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

import com.browseengine.bobo.config.FieldConfiguration;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class FieldConfConverter implements Converter
{
  public void marshal(Object obj, HierarchicalStreamWriter writer, MarshallingContext ctx)
  {
  /*  FieldConfiguration fconf=(FieldConfiguration)obj;
    Iterator<FieldPlugin> iter=null;//fconf.getIterator();
    while(iter.hasNext()){
      FieldPlugin fplugin=iter.next();
      writer.startNode("field");
      writer.startNode("name");
      writer.addAttribute("type", fplugin.getTypeString());
      writer.setValue(fplugin.getName());
      writer.endNode();
      Properties props=fplugin.getProperties();
      if (props!=null){
	      Enumeration<?> propIter=props.propertyNames();
	      while(propIter.hasMoreElements()){
	        String name=(String)propIter.nextElement();
	        writer.startNode("param");
	        writer.addAttribute("name", name);
	        writer.addAttribute("value", props.getProperty(name));
	        writer.endNode();
	      }
      }
      writer.endNode();
    }*/
  }

  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext ctx)
  {
    FieldConfiguration fconf=new FieldConfiguration();
    while(reader.hasMoreChildren()){
      reader.moveDown();
      String nodeName=reader.getNodeName();
      if ("field".equals(nodeName)){
        Properties props=new Properties();
        String pluginName=null;
        String pluginType=null;
        while(reader.hasMoreChildren()){
          reader.moveDown();
          String fieldNodeName=reader.getNodeName();
          if ("param".equals(fieldNodeName)){
            String N=reader.getAttribute("name");
            String V=reader.getAttribute("value");
            props.put(N, V);
          }
          else if ("name".equals(fieldNodeName)){
            pluginName=reader.getValue().trim();
            pluginType=reader.getAttribute("type");
          }
          reader.moveUp();
        }
        if (pluginName!=null){
          fconf.addPlugin(pluginName, pluginType, props);
        }
      }
      reader.moveUp();
    }
    return fconf;
  }

  public boolean canConvert(Class cls)
  {
    return FieldConfiguration.class.equals(cls);
  }

  public static void main(String[] args) throws Exception
  {
    File location=new File("/Users/john/project/bobo_dev_1_5_0/bobo/cardata/cartag/field.xml");
    FileReader reader=new FileReader(location);
    XStream xstream=new XStream(new DomDriver());
    xstream.registerConverter(new FieldConfConverter());
    xstream.alias("field-info", FieldConfiguration.class);
    FieldConfiguration fconf=(FieldConfiguration)xstream.fromXML(reader);
    
    System.out.println(xstream.toXML(fconf));
  }
}
