package com.browseengine.bobo.sort;

import java.io.IOException;
import java.text.Collator;
import java.util.Locale;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.util.BytesRef;

public abstract class DocComparatorSource {

  boolean _reverse = false;

  public DocComparatorSource setReverse(boolean reverse) {
    _reverse = reverse;
    return this;
  }

  public final boolean isReverse() {
    return _reverse;
  }

  public abstract DocComparator getComparator(AtomicReader reader, int docbase) throws IOException;

  public static class IntDocComparatorSource extends DocComparatorSource {
    private final String field;

    public IntDocComparatorSource(String field) {
      this.field = field;
    }

    @Override
    public DocComparator getComparator(AtomicReader reader, int docbase) throws IOException {

      final FieldCache.Ints values = FieldCache.DEFAULT.getInts(reader, field, true);
      return new DocComparator() {
        @Override
        public int compare(ScoreDoc doc1, ScoreDoc doc2) {
          // cannot return v1-v2 because it could overflow
          if (values.get(doc1.doc) < values.get(doc2.doc)) {
            return -1;
          } else if (values.get(doc1.doc) > values.get(doc2.doc)) {
            return 1;
          } else {
            return 0;
          }
        }

        @Override
        public Integer value(ScoreDoc doc) {
          return values.get(doc.doc);
        }
      };
    }
  }

  public static class StringLocaleComparatorSource extends DocComparatorSource {
    private final String field;
    private final Collator _collator;

    public StringLocaleComparatorSource(String field, Locale locale) {
      this.field = field;
      _collator = Collator.getInstance(locale);
    }

    @Override
    public DocComparator getComparator(AtomicReader reader, int docbase) throws IOException {

      final BinaryDocValues values = FieldCache.DEFAULT.getTerms(reader, field);

      return new DocComparator() {
        @Override
        public int compare(ScoreDoc doc1, ScoreDoc doc2) {
          BytesRef result1 = new BytesRef();
          BytesRef result2 = new BytesRef();
          values.get(doc1.doc, result1);
          values.get(doc2.doc, result2);
          if (result1.length == 0) {
            if (result2.length == 0) {
              return 0;
            }
            return -1;
          } else if (result2.length == 0) {
            return 1;
          }
          return _collator.compare(result1.utf8ToString(), result2.utf8ToString());
        }

        @Override
        public String value(ScoreDoc doc) {
          BytesRef result = new BytesRef();
          values.get(doc.doc, result);
          return result.utf8ToString();
        }
      };
    }
  }

  public static class StringValComparatorSource extends DocComparatorSource {
    private final String field;

    public StringValComparatorSource(String field) {
      this.field = field;
    }

    @Override
    public DocComparator getComparator(AtomicReader reader, int docbase) throws IOException {

      final BinaryDocValues values = FieldCache.DEFAULT.getTerms(reader, field);

      return new DocComparator() {
        @Override
        public int compare(ScoreDoc doc1, ScoreDoc doc2) {
          BytesRef result1 = new BytesRef();
          BytesRef result2 = new BytesRef();
          values.get(doc1.doc, result1);
          values.get(doc2.doc, result2);

          if (result1.length == 0) {
            if (result2.length == 0) {
              return 0;
            }
            return -1;
          } else if (result2.length == 0) {
            return 1;
          }
          return result1.utf8ToString().compareTo(result2.utf8ToString());
        }

        @Override
        public String value(ScoreDoc doc) {
          BytesRef result = new BytesRef();
          values.get(doc.doc, result);
          return result.utf8ToString();
        }
      };
    }
  }

  public static class StringOrdComparatorSource extends DocComparatorSource {
    private final String field;

    public StringOrdComparatorSource(String field) {
      this.field = field;
    }

    @Override
    public DocComparator getComparator(AtomicReader reader, int docbase) throws IOException {

      final SortedDocValues values = FieldCache.DEFAULT.getTermsIndex(reader, field);
      return new DocComparator() {
        @Override
        public int compare(ScoreDoc doc1, ScoreDoc doc2) {
          return values.getOrd(doc1.doc) - values.getOrd(doc2.doc);
        }

        @Override
        public String value(ScoreDoc doc) {
          int ord = values.getOrd(doc.doc);
          BytesRef term = new BytesRef();
          values.lookupOrd(ord, term);
          return term.utf8ToString();
        }
      };
    }
  }

  public static class ShortDocComparatorSource extends DocComparatorSource {
    private final String field;

    public ShortDocComparatorSource(String field) {
      this.field = field;
    }

    @Override
    public DocComparator getComparator(AtomicReader reader, int docbase) throws IOException {

      final FieldCache.Shorts values = FieldCache.DEFAULT.getShorts(reader, field, true);

      return new DocComparator() {
        @Override
        public int compare(ScoreDoc doc1, ScoreDoc doc2) {
          return values.get(doc1.doc) - values.get(doc2.doc);
        }

        @Override
        public Short value(ScoreDoc doc) {
          return values.get(doc.doc);
        }
      };
    }
  }

  public static class LongDocComparatorSource extends DocComparatorSource {
    private final String field;

    public LongDocComparatorSource(String field) {
      this.field = field;
    }

    @Override
    public DocComparator getComparator(AtomicReader reader, int docbase) throws IOException {

      final FieldCache.Longs values = FieldCache.DEFAULT.getLongs(reader, field, true);

      return new DocComparator() {
        @Override
        public int compare(ScoreDoc doc1, ScoreDoc doc2) {
          // cannot return v1-v2 because it could overflow
          if (values.get(doc1.doc) < values.get(doc2.doc)) {
            return -1;
          } else if (values.get(doc1.doc) > values.get(doc2.doc)) {
            return 1;
          } else {
            return 0;
          }
        }

        @Override
        public Long value(ScoreDoc doc) {
          return values.get(doc.doc);
        }
      };
    }
  }

  public static class FloatDocComparatorSource extends DocComparatorSource {
    private final String field;

    public FloatDocComparatorSource(String field) {
      this.field = field;
    }

    @Override
    public DocComparator getComparator(AtomicReader reader, int docbase) throws IOException {

      final FieldCache.Floats values = FieldCache.DEFAULT.getFloats(reader, field, true);

      return new DocComparator() {
        @Override
        public int compare(ScoreDoc doc1, ScoreDoc doc2) {
          // cannot return v1-v2 because it could overflow
          if (values.get(doc1.doc) < values.get(doc2.doc)) {
            return -1;
          } else if (values.get(doc1.doc) > values.get(doc2.doc)) {
            return 1;
          } else {
            return 0;
          }
        }

        @Override
        public Float value(ScoreDoc doc) {
          return values.get(doc.doc);
        }
      };
    }
  }

  public static class DoubleDocComparatorSource extends DocComparatorSource {
    private final String field;

    public DoubleDocComparatorSource(String field) {
      this.field = field;
    }

    @Override
    public DocComparator getComparator(AtomicReader reader, int docbase) throws IOException {

      final FieldCache.Doubles values = FieldCache.DEFAULT.getDoubles(reader, field, true);

      return new DocComparator() {
        @Override
        public int compare(ScoreDoc doc1, ScoreDoc doc2) {
          // cannot return v1-v2 because it could overflow
          if (values.get(doc1.doc) < values.get(doc2.doc)) {
            return -1;
          } else if (values.get(doc1.doc) > values.get(doc2.doc)) {
            return 1;
          } else {
            return 0;
          }
        }

        @Override
        public Double value(ScoreDoc doc) {
          return values.get(doc.doc);
        }
      };
    }
  }

  public static class RelevanceDocComparatorSource extends DocComparatorSource {
    public RelevanceDocComparatorSource() {
    }

    @Override
    public DocComparator getComparator(AtomicReader reader, int docbase) throws IOException {

      return new DocComparator() {
        @Override
        public int compare(ScoreDoc doc1, ScoreDoc doc2) {
          // cannot return v1-v2 because it could overflow
          if (doc1.score < doc2.score) {
            return -1;
          } else if (doc1.score > doc2.score) {
            return 1;
          } else {
            return 0;
          }
        }

        @Override
        public Float value(ScoreDoc doc) {
          return Float.valueOf(doc.score);
        }
      };
    }

  }

  public static class DocIdDocComparatorSource extends DocComparatorSource {
    public DocIdDocComparatorSource() {
    }

    @Override
    public DocComparator getComparator(AtomicReader reader, int docbase) throws IOException {

      final int _docbase = docbase;

      return new DocComparator() {
        @Override
        public int compare(ScoreDoc doc1, ScoreDoc doc2) {
          return doc1.doc - doc2.doc;
        }

        @Override
        public Integer value(ScoreDoc doc) {
          return Integer.valueOf(doc.doc + _docbase);
        }
      };
    }
  }

  public static class ByteDocComparatorSource extends DocComparatorSource {
    private final String field;

    public ByteDocComparatorSource(String field) {
      this.field = field;
    }

    @Override
    public DocComparator getComparator(AtomicReader reader, int docbase) throws IOException {

      final FieldCache.Bytes values = FieldCache.DEFAULT.getBytes(reader, field, true);

      return new DocComparator() {
        @Override
        public int compare(ScoreDoc doc1, ScoreDoc doc2) {
          return values.get(doc1.doc) - values.get(doc2.doc);
        }

        @Override
        public Byte value(ScoreDoc doc) {
          return values.get(doc.doc);
        }
      };
    }
  }

}
