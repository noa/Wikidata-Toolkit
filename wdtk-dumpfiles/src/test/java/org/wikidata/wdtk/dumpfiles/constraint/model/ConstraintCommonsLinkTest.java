package org.wikidata.wdtk.dumpfiles.constraint.model;

/*
 * #%L
 * Wikidata Toolkit Dump File Handling
 * %%
 * Copyright (C) 2014 Wikidata Toolkit Developers
 * %%
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
 * #L%
 */

import org.junit.Assert;
import org.junit.Test;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

/**
 * Test class for {@link ConstraintCommonsLink}
 *
 * @author Julian Mendez
 *
 */
public class ConstraintCommonsLinkTest {

	public static final String TEMPLATE_STR = "{{Constraint:Commons link}}";

	@Test
	public void testParameters() {
		PropertyIdValue constrainedProperty = ConstraintTestHelper
				.getPropertyIdValue("P41");
		ConstraintCommonsLink constraint = new ConstraintCommonsLink(
				constrainedProperty);
		Assert.assertEquals(constrainedProperty,
				constraint.getConstrainedProperty());
	}

	@Test
	public void testToStringAndVisit() {
		String propertyName = "P41";
		String string = propertyName + " " + TEMPLATE_STR;
		PropertyIdValue constrainedProperty = ConstraintTestHelper
				.getPropertyIdValue(propertyName);
		ConstraintCommonsLink constraint = new ConstraintCommonsLink(
				constrainedProperty);
		Assert.assertEquals(TEMPLATE_STR, constraint.getTemplate());
		Assert.assertEquals(string, constraint.toString());

		ConstraintTestHelper.testVisit(constraint);
	}

	@Test
	public void testEquals() {
		PropertyIdValue constrainedProperty0 = ConstraintTestHelper
				.getPropertyIdValue("P41");
		PropertyIdValue constrainedProperty1 = ConstraintTestHelper
				.getPropertyIdValue("P94");

		ConstraintTestHelper.testEquals(new ConstraintCommonsLink(
				constrainedProperty0), new ConstraintCommonsLink(
				constrainedProperty0), new ConstraintCommonsLink(
				constrainedProperty1));
	}

}