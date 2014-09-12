/*
* Copyright (C) 2014 41concepts Aps
*/
package com.fortyoneconcepts.valjogen.test;

import org.junit.Test;

import com.fortyoneconcepts.valjogen.model.*;
import com.fortyoneconcepts.valjogen.test.input.*;
import com.fortyoneconcepts.valjogen.test.util.TemplateTestBase;

import static com.fortyoneconcepts.valjogen.test.util.TestSupport.*;

/**
 * Stubbed integration test of StringTemplate generation of special code related to a custom string template. See {@link TemplateTestBase} for general
 * words about template tests.
 *
 * @author mmc
 */
public class TemplateCustomTest extends TemplateTestBase
{
	@Test
	public void testCustomTemplate() throws Exception
	{
		Output output = produceOutput(CustomTemplateInterface.class, configureAnnotationBuilder.add(ConfigurationOptionKeys.comparableEnabled, true)
				                                                     .add(ConfigurationOptionKeys.extraInterfaceNames, new String[] {"java.lang.Comparable<$(This)>"})
				                                                     .add(ConfigurationOptionKeys.customTemplateFileName, "custom_template.stg").build());

		String[] searchStrings = { "generated", "class annotation", "import",
				                   "class javadoc", "property javadoc", "equals javadoc", "hashCode javadoc", "toString javadoc", "compareTo javadoc",
				                   "before static members", "after static members",
				                   "before instance members", "after instance members",
				                   "before static methods", "after static methods",
				                   "before instance methods", "after instance methods",
				                   "member _object annotation",
				                   "constructor annotation", "factory annotation",
				                   "equals annotation", "hashCode annotation", "toString annotation", "compareTo annotation",
				                   "constructor preamble", "factory preamble",
				                   "equals preamble", "hashCode preamble", "toString preamble", "compareTo preamble",
				                   "property getObject preamble"
		};

		for (String searchString : searchStrings)
		  assertContainsWithWildcards("Inserted "+searchString+" stuff here.", output.code);
	}
}
