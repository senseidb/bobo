package com.browseengine.bobo.util;

import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.io.*;

/**
 * @since 1.2
 */
public class BoboJSONStreamDriver implements HierarchicalStreamDriver {
    public HierarchicalStreamReader createReader(Reader in) {
        throw new UnsupportedOperationException("The BoboJSONStreamDriver can only write JSON");
    }

    public HierarchicalStreamReader createReader(InputStream in) {
        throw new UnsupportedOperationException("The BoboJSONStreamDriver can only write JSON");
    }

    public HierarchicalStreamWriter createWriter(Writer out) {
        return new BoboJSONStreamWriter(out);
    }

    public HierarchicalStreamWriter createWriter(OutputStream out) {
        return createWriter(new OutputStreamWriter(out));
    }

}
