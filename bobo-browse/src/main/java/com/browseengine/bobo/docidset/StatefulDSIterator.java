package com.browseengine.bobo.docidset;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * NOTICE :  This classes currently does not exist in any release branch for Lucene until  2.3.2,
 *   once they are available in a stable release branch, we will eliminate these classes and depend
 *    on the Lucene release jar directly.
 *    DATE : 07/14/08
 *
 */



/**
 * This abstract class defines methods to iterate over a set of
 * non-decreasing doc ids.
 */
public abstract class StatefulDSIterator extends org.apache.lucene.search.DocIdSetIterator{
  abstract public int getCursor();
}
