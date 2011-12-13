This directory contains a contrib-area for bobo extensions and enhancements, separate from the core.

Current functionality list
  BTree-Bitwise-Interlace-Geosearch

To build, do:

  cd ..
  ant
  cd bobo-contrib
  ant

To test, do:

  cd ..
  ant 
  cd bobo-contrib
  ant clean test

You have to put lucene-patch-0.1.jar ahead of lucene-core-3.5*.jar in your classpath.  bobo-contrib-0.1.jar contains the rest of the BTree-Bitwise-Interlace-Geosearch.  

The bobo-contrib area is a lucene-3.5* derivative, that is not yet reconciled with bobo-browse's use of lucene-3.0*. 


