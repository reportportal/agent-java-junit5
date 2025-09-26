package com.epam.reportportal.junit5.features.description;

import com.epam.reportportal.annotations.Description;
import com.epam.reportportal.junit5.DescriptionTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Description(DescriptionAnnotatedClassTest.TEST_DESCRIPTION_CLASS)
@ExtendWith(DescriptionTest.TestExtension.class)
public class DescriptionAnnotatedClassTest {
	public static final String TEST_DESCRIPTION_CLASS = "My test description on the class";

	@Test
	public void testDescriptionTest() {
	}
}
