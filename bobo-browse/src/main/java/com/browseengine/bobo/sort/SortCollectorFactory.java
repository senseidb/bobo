/*
 * Copyright 2013 jonathan.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.browseengine.bobo.sort;

import com.browseengine.bobo.api.Browsable;
import java.util.Set;
import org.apache.lucene.search.SortField;

public interface SortCollectorFactory {

    SortCollector create(DocComparatorSource compSource,
        SortField[] sortFields,
        Browsable boboBrowser,
        int offset,
        int count,
        boolean doScoring,
        boolean fetchStoredFields,
        Set<String> termVectorsToFetch,
        String[] groupBy,
        int maxPerGroup,
        boolean collectDocIdCache);
}
