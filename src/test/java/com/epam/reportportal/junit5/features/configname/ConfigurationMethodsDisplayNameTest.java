package com.epam.reportportal.junit5.features.configname;

import com.epam.reportportal.junit5.miscellaneous.DisplayNamesForConfigurationItemsTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DisplayNamesForConfigurationItemsTest.TestExtension.class)
public class ConfigurationMethodsDisplayNameTest {
	public static final String DISPLAY_NAME = "My custom display name";

	@DisplayName(DISPLAY_NAME)
	@BeforeAll
	public static void beforeAll() {
		System.out.println("Before all");
	}

	@DisplayName(DISPLAY_NAME)
	@BeforeEach
	public void beforeEach() {
		System.out.println("Before each");
	}

	@DisplayName(DISPLAY_NAME)
	@Test
	public void testBeforeAfterAll() {
		System.out.println("Test");
	}

	@DisplayName(DISPLAY_NAME)
	@AfterEach
	public void afterEach() {
		System.out.println("After each");
	}

	@DisplayName(DISPLAY_NAME)
	@AfterAll
	public static void afterAll() {
		System.out.println("After all");
	}

}
