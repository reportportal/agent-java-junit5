package com.epam.reportportal.junit5.features.displayname;

import com.epam.reportportal.annotations.DisplayName;
import com.epam.reportportal.junit5.DisplayNameTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DisplayNameTest.TestExtension.class)
@DisplayName(DisplayNameAnnotatedClassTest.TEST_DISPLAY_NAME_CLASS)
public class DisplayNameAnnotatedMethodAndClassTest {
	@Test
	@DisplayName(DisplayNameAnnotatedMethodTest.TEST_DISPLAY_NAME_METHOD)
	public void testDisplayNameTest() {
	}
}
