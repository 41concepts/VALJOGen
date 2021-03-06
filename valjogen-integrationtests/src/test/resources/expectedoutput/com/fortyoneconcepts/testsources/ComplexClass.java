package com.fortyoneconcepts.valjogen.testsources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Generated;

@Generated(value = "com.fortyoneconcepts.valjogen", date="2014-11-16T17:41Z", comments="Generated by ValjoGen code generator (ValjoGen.41concepts.com) from com.fortyoneconcepts.valjogen.testsources.ComplexInterfaceWithAllTypes")
public final class ComplexClass implements ComplexInterfaceWithAllTypes
{
  private final ComplexInterfaceWithAllTypes other;
  private final Object object;
  private final String string;
  private final java.util.Date date;
  private final Object[] objectArray;
  private final Object[][] objectMultiArray;
  private final byte _byte;
  private final int _int;
  private final long _long;
  private final char _char;
  private final boolean _boolean;
  private final float _float;
  private final double _double;
  private final byte[] byteArray;
  private final int[] intArray;
  private final long[] longArray;
  private final char[] charArray;
  private final boolean[] booleanArray;
  private final float[] floatArray;
  private final double[] doubleArray;
  private final byte[][] byteMultiArray;
  private final int[][] intMultiArray;
  private final long[][] longMultiArray;
  private final char[][] charMultiArray;
  private final boolean[][] booleanMultiArray;
  private final float[][] floatMultiArray;
  private final double[][] doubleMultiArray;

  @JsonCreator
  public static ComplexClass valueOf(@JsonProperty("other") final ComplexInterfaceWithAllTypes other, @JsonProperty("object") final Object object, @JsonProperty("string") final String string, @JsonProperty("date") final java.util.Date date, @JsonProperty("objectArray") final Object[] objectArray, @JsonProperty("objectMultiArray") final Object[][] objectMultiArray, @JsonProperty("_byte") final byte _byte, @JsonProperty("_int") final int _int, @JsonProperty("_long") final long _long, @JsonProperty("_char") final char _char, @JsonProperty("_boolean") final boolean _boolean, @JsonProperty("_float") final float _float, @JsonProperty("_double") final double _double, @JsonProperty("byteArray") final byte[] byteArray, @JsonProperty("intArray") final int[] intArray, @JsonProperty("longArray") final long[] longArray, @JsonProperty("charArray") final char[] charArray, @JsonProperty("booleanArray") final boolean[] booleanArray, @JsonProperty("floatArray") final float[] floatArray, @JsonProperty("doubleArray") final double[] doubleArray, @JsonProperty("byteMultiArray") final byte[][] byteMultiArray, @JsonProperty("intMultiArray") final int[][] intMultiArray, @JsonProperty("longMultiArray") final long[][] longMultiArray, @JsonProperty("charMultiArray") final char[][] charMultiArray, @JsonProperty("booleanMultiArray") final boolean[][] booleanMultiArray, @JsonProperty("floatMultiArray") final float[][] floatMultiArray, @JsonProperty("doubleMultiArray") final double[][] doubleMultiArray)
  {
    ComplexClass _instance = new ComplexClass(other, object, string, date, objectArray, objectMultiArray, _byte, _int, _long, _char, _boolean, _float, _double, byteArray, intArray, longArray, charArray, booleanArray, floatArray, doubleArray, byteMultiArray, intMultiArray, longMultiArray, charMultiArray, booleanMultiArray, floatMultiArray, doubleMultiArray);
    return _instance;
  }

  private ComplexClass(final ComplexInterfaceWithAllTypes other, final Object object, final String string, final java.util.Date date, final Object[] objectArray, final Object[][] objectMultiArray, final byte _byte, final int _int, final long _long, final char _char, final boolean _boolean, final float _float, final double _double, final byte[] byteArray, final int[] intArray, final long[] longArray, final char[] charArray, final boolean[] booleanArray, final float[] floatArray, final double[] doubleArray, final byte[][] byteMultiArray, final int[][] intMultiArray, final long[][] longMultiArray, final char[][] charMultiArray, final boolean[][] booleanMultiArray, final float[][] floatMultiArray, final double[][] doubleMultiArray)
  {
    super();
    this.other=Objects.requireNonNull(other);
    this.object=Objects.requireNonNull(object);
    this.string=Objects.requireNonNull(string);
    this.date=Objects.requireNonNull(date);
    this.objectArray=Objects.requireNonNull(objectArray);
    this.objectMultiArray=Objects.requireNonNull(objectMultiArray);
    this._byte=_byte;
    this._int=_int;
    this._long=_long;
    this._char=_char;
    this._boolean=_boolean;
    this._float=_float;
    this._double=_double;
    this.byteArray=Objects.requireNonNull(byteArray);
    this.intArray=Objects.requireNonNull(intArray);
    this.longArray=Objects.requireNonNull(longArray);
    this.charArray=Objects.requireNonNull(charArray);
    this.booleanArray=Objects.requireNonNull(booleanArray);
    this.floatArray=Objects.requireNonNull(floatArray);
    this.doubleArray=Objects.requireNonNull(doubleArray);
    this.byteMultiArray=Objects.requireNonNull(byteMultiArray);
    this.intMultiArray=Objects.requireNonNull(intMultiArray);
    this.longMultiArray=Objects.requireNonNull(longMultiArray);
    this.charMultiArray=Objects.requireNonNull(charMultiArray);
    this.booleanMultiArray=Objects.requireNonNull(booleanMultiArray);
    this.floatMultiArray=Objects.requireNonNull(floatMultiArray);
    this.doubleMultiArray=Objects.requireNonNull(doubleMultiArray);
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public ComplexInterfaceWithAllTypes getOther()
  {
   return other;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public Object getObject()
  {
   return object;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public String getString()
  {
   return string;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public java.util.Date getDate()
  {
   return date;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public Object[] getObjectArray()
  {
   return objectArray;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public Object[][] getObjectMultiArray()
  {
   return objectMultiArray;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public byte getByte()
  {
   return _byte;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public int getInt()
  {
   return _int;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public long getLong()
  {
   return _long;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public char getChar()
  {
   return _char;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public boolean isBoolean()
  {
   return _boolean;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public float getFloat()
  {
   return _float;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public double getDouble()
  {
   return _double;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public byte[] getByteArray()
  {
   return byteArray;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public int[] getIntArray()
  {
   return intArray;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public long[] getLongArray()
  {
   return longArray;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public char[] getCharArray()
  {
   return charArray;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public boolean[] getBooleanArray()
  {
   return booleanArray;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public float[] getFloatArray()
  {
   return floatArray;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public double[] getDoubleArray()
  {
   return doubleArray;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public byte[][] getByteMultiArray()
  {
   return byteMultiArray;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public int[][] getIntMultiArray()
  {
   return intMultiArray;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public long[][] getLongMultiArray()
  {
   return longMultiArray;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public char[][] getCharMultiArray()
  {
   return charMultiArray;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public boolean[][] getBooleanMultiArray()
  {
   return booleanMultiArray;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public float[][] getFloatMultiArray()
  {
   return floatMultiArray;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public double[][] getDoubleMultiArray()
  {
   return doubleMultiArray;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public int hashCode()
  {
    final int _prime = 31;
    int _result = 1;
    _result = _prime * _result + Objects.hashCode(other); 
    _result = _prime * _result + Objects.hashCode(object); 
    _result = _prime * _result + Objects.hashCode(string); 
    _result = _prime * _result + Objects.hashCode(date); 
    _result = _prime * _result + Arrays.hashCode(objectArray); 
    _result = _prime * _result + Arrays.deepHashCode(objectMultiArray); 
    _result = _prime * _result + Byte.hashCode(_byte); 
    _result = _prime * _result + Integer.hashCode(_int); 
    _result = _prime * _result + Long.hashCode(_long); 
    _result = _prime * _result + Character.hashCode(_char); 
    _result = _prime * _result + Boolean.hashCode(_boolean); 
    _result = _prime * _result + Float.hashCode(_float); 
    _result = _prime * _result + Double.hashCode(_double); 
    _result = _prime * _result + Arrays.hashCode(byteArray); 
    _result = _prime * _result + Arrays.hashCode(intArray); 
    _result = _prime * _result + Arrays.hashCode(longArray); 
    _result = _prime * _result + Arrays.hashCode(charArray); 
    _result = _prime * _result + Arrays.hashCode(booleanArray); 
    _result = _prime * _result + Arrays.hashCode(floatArray); 
    _result = _prime * _result + Arrays.hashCode(doubleArray); 
    _result = _prime * _result + Arrays.deepHashCode(byteMultiArray); 
    _result = _prime * _result + Arrays.deepHashCode(intMultiArray); 
    _result = _prime * _result + Arrays.deepHashCode(longMultiArray); 
    _result = _prime * _result + Arrays.deepHashCode(charMultiArray); 
    _result = _prime * _result + Arrays.deepHashCode(booleanMultiArray); 
    _result = _prime * _result + Arrays.deepHashCode(floatMultiArray); 
    _result = _prime * _result + Arrays.deepHashCode(doubleMultiArray); 
    return _result;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public boolean equals(final Object arg0)
  {
    if (this == arg0)
      return true;

    if (arg0 == null)
      return false;

    if (getClass() != arg0.getClass())
      return false;

    @SuppressWarnings("unchecked")
    ComplexClass _other = (ComplexClass) arg0;

    return (Objects.equals(other, _other.other) && Objects.equals(object, _other.object) && Objects.equals(string, _other.string) && Objects.equals(date, _other.date) && Arrays.equals(objectArray, _other.objectArray) && Arrays.deepEquals(objectMultiArray, _other.objectMultiArray) && (_byte == _other._byte) && (_int == _other._int) && (_long == _other._long) && (_char == _other._char) && (_boolean == _other._boolean) && (Float.floatToIntBits(_float) == Float.floatToIntBits(_other._float)) && (Double.doubleToLongBits(_double) == Double.doubleToLongBits(_other._double)) && Arrays.equals(byteArray, _other.byteArray) && Arrays.equals(intArray, _other.intArray) && Arrays.equals(longArray, _other.longArray) && Arrays.equals(charArray, _other.charArray) && Arrays.equals(booleanArray, _other.booleanArray) && Arrays.equals(floatArray, _other.floatArray) && Arrays.equals(doubleArray, _other.doubleArray) && Arrays.deepEquals(byteMultiArray, _other.byteMultiArray) && Arrays.deepEquals(intMultiArray, _other.intMultiArray) && Arrays.deepEquals(longMultiArray, _other.longMultiArray) && Arrays.deepEquals(charMultiArray, _other.charMultiArray) && Arrays.deepEquals(booleanMultiArray, _other.booleanMultiArray) && Arrays.deepEquals(floatMultiArray, _other.floatMultiArray) && Arrays.deepEquals(doubleMultiArray, _other.doubleMultiArray));
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public String toString()
  {
    final StringBuilder _sb = new StringBuilder();
    _sb.append("ComplexClass [");
    _sb.append("other=");
    _sb.append(other); 
    _sb.append(", ");
    _sb.append("object=");
    _sb.append(object); 
    _sb.append(", ");
    _sb.append("string=");
    _sb.append(string); 
    _sb.append(", ");
    _sb.append("date=");
    _sb.append(date); 
    _sb.append(", ");
    _sb.append("objectArray=");
    _sb.append(Arrays.toString(objectArray)); 
    _sb.append(", ");
    _sb.append("objectMultiArray=");
    _sb.append(Arrays.toString(objectMultiArray)); 
    _sb.append(", ");
    _sb.append("_byte=");
    _sb.append(_byte); 
    _sb.append(", ");
    _sb.append("_int=");
    _sb.append(_int); 
    _sb.append(", ");
    _sb.append("_long=");
    _sb.append(_long); 
    _sb.append(", ");
    _sb.append("_char=");
    _sb.append(_char); 
    _sb.append(", ");
    _sb.append("_boolean=");
    _sb.append(_boolean); 
    _sb.append(", ");
    _sb.append("_float=");
    _sb.append(_float); 
    _sb.append(", ");
    _sb.append("_double=");
    _sb.append(_double); 
    _sb.append(", ");
    _sb.append("byteArray=");
    _sb.append(Arrays.toString(byteArray)); 
    _sb.append(", ");
    _sb.append("intArray=");
    _sb.append(Arrays.toString(intArray)); 
    _sb.append(", ");
    _sb.append("longArray=");
    _sb.append(Arrays.toString(longArray)); 
    _sb.append(", ");
    _sb.append("charArray=");
    _sb.append(Arrays.toString(charArray)); 
    _sb.append(", ");
    _sb.append("booleanArray=");
    _sb.append(Arrays.toString(booleanArray)); 
    _sb.append(", ");
    _sb.append("floatArray=");
    _sb.append(Arrays.toString(floatArray)); 
    _sb.append(", ");
    _sb.append("doubleArray=");
    _sb.append(Arrays.toString(doubleArray)); 
    _sb.append(", ");
    _sb.append("byteMultiArray=");
    _sb.append(Arrays.toString(byteMultiArray)); 
    _sb.append(", ");
    _sb.append("intMultiArray=");
    _sb.append(Arrays.toString(intMultiArray)); 
    _sb.append(", ");
    _sb.append("longMultiArray=");
    _sb.append(Arrays.toString(longMultiArray)); 
    _sb.append(", ");
    _sb.append("charMultiArray=");
    _sb.append(Arrays.toString(charMultiArray)); 
    _sb.append(", ");
    _sb.append("booleanMultiArray=");
    _sb.append(Arrays.toString(booleanMultiArray)); 
    _sb.append(", ");
    _sb.append("floatMultiArray=");
    _sb.append(Arrays.toString(floatMultiArray)); 
    _sb.append(", ");
    _sb.append("doubleMultiArray=");
    _sb.append(Arrays.toString(doubleMultiArray)); 
    _sb.append(']');
    return _sb.toString();
  }
}