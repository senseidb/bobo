This directory contains a contrib-area for bobo extensions and enhancements, separate from the core.

To build, do:

  cd ..
  ant
  cd bobo-contrib
  ant

You have to put lucene-patch-0.1.jar ahead of lucene-core-3.3*.jar in your classpath.  Bobo-contrib-0.1.jar contains the rest of the Btree Bitwise Interlace Geosearch.  

The bobo-contrib area is a lucene-3.3* derivative, that is not yet reconciled with bobo "core"s use of lucene-3.0*. 

