package com.epam.reportportal.junit5.features.attributes;

import com.epam.reportportal.annotations.attribute.Attribute;
import com.epam.reportportal.annotations.attribute.Attributes;
import com.epam.reportportal.junit5.AttributesTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(AttributesTest.AttributesTestExtension.class)
public class MethodLevelAttributesTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodLevelAttributesTest.class);

	@Test
	@Attributes(attributes = { @Attribute(key = "myKey", value = "myValue") })
	public void testMethodLevelAttributes() {
		LOGGER.info("Inside 'testMethodLevelAttributes' method");
	}
}
