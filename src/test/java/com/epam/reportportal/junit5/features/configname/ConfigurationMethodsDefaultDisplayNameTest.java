package com.epam.reportportal.junit5.features.configname;

import com.epam.reportportal.junit5.miscellaneous.DisplayNamesForConfigurationItemsTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DisplayNamesForConfigurationItemsTest.TestExtension.class)
public class ConfigurationMethodsDefaultDisplayNameTest {
	public static final String DISPLAY_NAME = "My custom display name";

	@BeforeAll
	public static void beforeAll() {
		System.out.println("Before all");
	}

	@BeforeEach
	public void beforeEach() {
		System.out.println("Before each");
	}

	@Test
	public void testBeforeAfterAll() {
		System.out.println("Test");
	}

	@AfterEach
	public void afterEach() {
		System.out.println("After each");
	}

	@AfterAll
	public static void afterAll() {
		System.out.println("After all");
	}

}
