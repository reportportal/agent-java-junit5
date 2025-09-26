package com.epam.reportportal.junit5.features.description;

import com.epam.reportportal.annotations.Description;
import com.epam.reportportal.junit5.DescriptionTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DescriptionTest.TestExtension.class)
public class DescriptionAnnotatedMethodTest {
	public static final String TEST_DESCRIPTION_METHOD = "My test description on the method";

	@Test
	@Description(TEST_DESCRIPTION_METHOD)
	public void testDescriptionTest() {
	}
}
