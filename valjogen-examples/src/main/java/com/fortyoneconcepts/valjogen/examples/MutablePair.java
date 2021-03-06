/*
* Copyright (C) 2014 41concepts Aps
*/
package com.fortyoneconcepts.valjogen.examples;

import com.fortyoneconcepts.valjogen.annotations.*;
import com.fortyoneconcepts.valjogen.annotations.types.Mutability;

/**
* Example that shows how to generate a simple mutable pair class.
* Note the tiny difference compared to the immutable pair class example.
*
* @param <TLeft> First object type in pair.
* @param <TRight> Second object type in pair.
*/
@VALJOGenerate(comment="Example 3")
@VALJOConfigure(mutability=Mutability.Mutable)
public interface MutablePair<TLeft, TRight>
{
	public TLeft getLeft();
	public void setLeft(TLeft left);

	public TRight getRight();
	public void setRight(TRight right);
}
