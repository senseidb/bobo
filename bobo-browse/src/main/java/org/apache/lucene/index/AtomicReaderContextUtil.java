package org.apache.lucene.index;

public class AtomicReaderContextUtil {

  private AtomicReaderContextUtil() {
  }

  public static AtomicReaderContext updateDocBase(AtomicReaderContext ctx, int docBase) {
    return new AtomicReaderContext(null, ctx.reader(), 0, 0, 0, docBase);
  }
}
