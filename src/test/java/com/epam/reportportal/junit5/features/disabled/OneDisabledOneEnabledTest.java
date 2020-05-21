package com.epam.reportportal.junit5.features.disabled;

import com.epam.reportportal.junit5.DisabledTestTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DisabledTestTest.DisabledTestExtension.class)
public class OneDisabledOneEnabledTest {

	public static final String DISPLAY_NAME = "My enabled display name";

	// JUnit 5 executes test in alphabetical order, so names matters
	@Disabled
	@Test
	void testADisabledTest() {
		System.out.println("disabled");
	}

	@Test
	@DisplayName(DISPLAY_NAME)
	void testBEnabledTest() {
		System.out.println("enabled");
	}
}
