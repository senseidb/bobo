/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2005-2006  John Wang
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * To contact the project administrators for the bobo-browse project, 
 * please go to https://sourceforge.net/projects/bobo-browse/, or 
 * send mail to owner@browseengine.com.
 */

package com.browseengine.bobo.test;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import com.browseengine.bobo.test.section.TestSectionSearch;


public class UnitTestSuite {

	  public static Test suite(){
          TestSuite suite=new TestSuite();
          suite.addTestSuite(BoboTestCase.class);
          suite.addTestSuite(FacetHandlerTest.class);
          suite.addTestSuite(TestSectionSearch.class);
          suite.addTestSuite(BoboFacetIteratorTest.class);
          suite.addTestSuite(FacetNotValuesTest.class);
          suite.addTestSuite(FacetNameTest.class);
          return suite;
  }
  
  /**
   * @param args
   */
  public static void main(String[] args) {
          TestRunner.run(suite());
  }
}
