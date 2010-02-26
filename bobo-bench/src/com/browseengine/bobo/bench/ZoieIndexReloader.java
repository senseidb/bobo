package com.browseengine.bobo.bench;

import javax.management.ObjectName;

import sun.tools.jconsole.ProxyClient;

public class ZoieIndexReloader
{

  /**
   * @param args
   */
  public static void main(String[] args) throws Exception
  {
    ProxyClient proxyClient = ProxyClient.getProxyClient("localhost", 9999,null,null);
    while(true)
    {
      long start=System.currentTimeMillis();
      proxyClient.invoke(new ObjectName("bobo-service:name=bobo-zoie-system"),"refreshDiskReader", new String[0], new String[0]);
      long end=System.currentTimeMillis();
      System.out.println("index reloaded in "+(end-start)+" ms");
      Thread.sleep(60000);
    }
  }

}
