package com.epam.reportportal.junit5.features.displayname;

import com.epam.reportportal.annotations.DisplayName;
import com.epam.reportportal.junit5.DisplayNameTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DisplayNameTest.TestExtension.class)
@DisplayName(DisplayNameAnnotatedClassTest.TEST_DISPLAY_NAME_CLASS)
@org.junit.jupiter.api.DisplayName("Junit class")
public class DisplayNameBothJunitAndRPAnnotatedMethodAndClassTest {
	@Test
	@DisplayName(DisplayNameAnnotatedMethodTest.TEST_DISPLAY_NAME_METHOD)
	@org.junit.jupiter.api.DisplayName("Junit method")
	public void testDisplayNameTest() {
	}
}
