/*
* Copyright (C) 2014 41concepts Aps
*/
/**
* This package contains an indenpendent intermediate representation that is used by string templates when generating output.
*
* Beware that property-style methods on the models may be called from string templates (*.stg files) so be careful with renaming!
*
* All types are fully independent of javax.model.* classes even though {@link com.fortyoneconcepts.valjogen.processor.builders.ModelBuilder} is the primary
* way to create Clazz instances from javax.model.* classes (provided by an annotation processor).
*
* Do also note that the model instances has circular references, so be careful not to get stack overflows or infinite loops if you do a deep search.
*
* @author mmc
**/
package com.fortyoneconcepts.valjogen.model;