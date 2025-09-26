package com.epam.reportportal.junit5.features.attributes;

import com.epam.reportportal.annotations.attribute.Attribute;
import com.epam.reportportal.annotations.attribute.Attributes;
import com.epam.reportportal.junit5.AttributesTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(AttributesTest.AttributesTestExtension.class)
@Attributes(attributes = { @Attribute(key = ClassLevelAttributesTest.KEY, value = ClassLevelAttributesTest.VALUE) })
public class ClassLevelAttributesTest {
	public static final String KEY = "attribute_test_key";
	public static final String VALUE = "attribute_test_value";

	private static final Logger LOGGER = LoggerFactory.getLogger(ClassLevelAttributesTest.class);

	@Test
	public void testClassLevelAttributes() {
		LOGGER.info("Inside 'testClassLevelAttributes' method");
	}
}
