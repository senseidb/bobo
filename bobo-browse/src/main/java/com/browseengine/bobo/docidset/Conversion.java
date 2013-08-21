package com.browseengine.bobo.docidset;

public class Conversion {
  public static final int BYTES_PER_INT = 4;
  public static final int BYTES_PER_LONG = 8;

  public static final void intToByteArray(int value, byte[] bytes, int offset) {
    bytes[offset] = (byte) (value >>> 24);
    bytes[offset + 1] = (byte) (value >>> 16);
    bytes[offset + 2] = (byte) (value >>> 8);
    bytes[offset + 3] = (byte) value;
  }

  public static final int byteArrayToInt(byte[] b, int offset) {
    return (b[offset] << 24) + ((b[offset + 1] & 0xFF) << 16) + ((b[offset + 2] & 0xFF) << 8)
        + (b[offset + 3] & 0xFF);
  }

  public static final void longToByteArray(long value, byte[] bytes, int offset) {
    bytes[offset] = (byte) (value >>> 56);
    bytes[offset + 1] = (byte) (value >>> 48);
    bytes[offset + 2] = (byte) (value >>> 40);
    bytes[offset + 3] = (byte) (value >>> 32);
    bytes[offset + 4] = (byte) (value >>> 24);
    bytes[offset + 5] = (byte) (value >>> 16);
    bytes[offset + 6] = (byte) (value >>> 8);
    bytes[offset + 7] = (byte) value;
  }

  public static final long byteArrayToLong(byte[] b, int offset) {
    return (b[offset] << 56) + ((b[offset + 1] & 0xFF) << 48) + ((b[offset + 2] & 0xFF) << 40)
        + ((b[offset + 3] & 0xFF) << 32) + ((b[offset + 4] & 0xFF) << 24)
        + ((b[offset + 5] & 0xFF) << 16) + ((b[offset + 6] & 0xFF) << 8) + (b[offset + 7] & 0xFF);
  }

}
