package com.epam.reportportal.junit5.features.displayname;

import com.epam.reportportal.annotations.DisplayName;
import com.epam.reportportal.junit5.DisplayNameTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DisplayNameTest.TestExtension.class)
public class DisplayNameAnnotatedMethodWithEmptyValueTest {
	public static final String TEST_DISPLAY_NAME_METHOD = "";

	@Test
	@DisplayName(TEST_DISPLAY_NAME_METHOD)
	public void testDisplayNameTest() {
	}
}
