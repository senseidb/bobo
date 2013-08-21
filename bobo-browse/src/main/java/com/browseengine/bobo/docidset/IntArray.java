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

package com.browseengine.bobo.docidset;

import java.io.IOException;
import java.io.Serializable;

/**
 *
 */
public class IntArray extends PrimitiveArray<Integer> implements Serializable {

  private static final long serialVersionUID = 1L;

  public IntArray(int len) {
    super(len);
  }

  public IntArray() {
    super();
  }

  public void add(int val) {
    ensureCapacity(_count + 1);
    int[] array = (int[]) _array;
    array[_count] = val;
    _count++;
  }

  public void set(int index, int val) {
    ensureCapacity(index);
    int[] array = (int[]) _array;
    array[index] = val;
    _count = Math.max(_count, index + 1);
  }

  public int get(int index) {
    int[] array = (int[]) _array;
    return array[index];
  }

  public boolean contains(int elem) {
    int size = this.size();
    for (int i = 0; i < size; ++i) {
      if (get(i) == elem) return true;
    }
    return false;
  }

  @Override
  protected Object buildArray(int len) {
    return new int[len];
  }

  public static int getSerialIntNum(IntArray instance) {
    int num = 3 + instance._count; // _len, _count, _growth
    return num;
  }

  public static int convertToBytes(IntArray instance, byte[] out, int offset) {
    int numInt = 0;
    Conversion.intToByteArray(instance._len, out, offset);
    offset += Conversion.BYTES_PER_INT;
    numInt++;

    Conversion.intToByteArray(instance._count, out, offset);
    offset += Conversion.BYTES_PER_INT;
    numInt++;

    Conversion.intToByteArray(instance._growth, out, offset);
    offset += Conversion.BYTES_PER_INT;
    numInt++;

    for (int i = 0; i < instance.size(); i++) {
      int data = instance.get(i);
      Conversion.intToByteArray(data, out, offset);
      offset += Conversion.BYTES_PER_INT;
    }
    numInt += instance.size();
    return numInt;
  }

  public static IntArray newInstanceFromBytes(byte[] inData, int offset) throws IOException {
    int len = Conversion.byteArrayToInt(inData, offset);
    offset += Conversion.BYTES_PER_INT;

    IntArray instance = new IntArray(len);

    int count = Conversion.byteArrayToInt(inData, offset);
    offset += Conversion.BYTES_PER_INT;

    int growth = Conversion.byteArrayToInt(inData, offset);
    offset += Conversion.BYTES_PER_INT;

    for (int i = 0; i < count; i++) {
      int data = Conversion.byteArrayToInt(inData, offset);
      offset += Conversion.BYTES_PER_INT;
      instance.add(data);
    }

    instance._growth = growth;
    if (instance._count != count) throw new IOException("cannot build IntArray from byte[]");

    return instance;
  }
}
