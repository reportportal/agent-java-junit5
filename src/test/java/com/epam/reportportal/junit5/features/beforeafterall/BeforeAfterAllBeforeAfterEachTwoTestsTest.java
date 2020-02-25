package com.epam.reportportal.junit5.features.beforeafterall;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(com.epam.reportportal.junit5.BeforeAfterAllTest.BeforeAfterAllTestExtension.class)
public class BeforeAfterAllBeforeAfterEachTwoTestsTest {
	public static final String CLASS_ID = "777a99a9-e9fd-43f2-9ab4-8621cffbebf4";

	@BeforeAll
	public static void beforeAll() {
		System.out.println("Before all: " + CLASS_ID);
	}

	@BeforeEach
	public void beforeEach() {
		System.out.println("Before each: " + CLASS_ID);
	}

	@Test
	public void testFirstBeforeAfterAllBeforeAfterEach() {
		System.out.println("First Test: " + CLASS_ID);
	}

	@Test
	public void testSecondBeforeAfterAllBeforeAfterEach() {
		System.out.println("Second Test: " + CLASS_ID);
	}

	@AfterEach
	public void afterEach() {
		System.out.println("After each: " + CLASS_ID);
	}

	@AfterAll
	public static void afterAll() {
		System.out.println("After all: " + CLASS_ID);
	}
}
