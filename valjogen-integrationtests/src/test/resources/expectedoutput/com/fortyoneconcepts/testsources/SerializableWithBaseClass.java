package com.fortyoneconcepts.valjogen.testsources;

import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Generated;

@Generated(value = "com.fortyoneconcepts.valjogen", date="2014-10-17T19:58Z", comments="Generated by ValjoGen code generator (ValjoGen.41concepts.com) from com.fortyoneconcepts.valjogen.testsources.ImmutableSerializableInterfaceWithBaseClass") 
public final class SerializableWithBaseClass extends BaseClass implements ImmutableSerializableInterfaceWithBaseClass
{
  private static final long serialVersionUID = 43;

  private final String value;

  public static SerializableWithBaseClass valueOf(final int baseValue, final String value)
  {
    SerializableWithBaseClass _instance = new SerializableWithBaseClass(baseValue, value);
    return _instance;
  }

  private SerializableWithBaseClass(final int baseValue, final String value)
  {
    super(baseValue);
    this.value=Objects.requireNonNull(value);
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public String getValue()
  {
   return value;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public int hashCode()
  {
    int _result = 31 * super.hashCode() + Objects.hash(value);
    return _result;
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public boolean equals(final Object obj)
  {
    if (this == obj)
      return true;

    if (obj == null)
      return false;

    if (!super.equals(obj))
      return false;

    if (getClass() != obj.getClass())
      return false;

    @SuppressWarnings("unchecked")
    SerializableWithBaseClass _other = (SerializableWithBaseClass) obj;

    return (Objects.equals(value, _other.value));
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public String toString()
  {
    final StringBuilder _sb = new StringBuilder();
    _sb.append("SerializableWithBaseClass [");
    _sb.append("value=");
    _sb.append(value); 
    _sb.append(']');
    return _sb.toString();
  }
}