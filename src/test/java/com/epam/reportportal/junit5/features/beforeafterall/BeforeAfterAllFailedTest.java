package com.epam.reportportal.junit5.features.beforeafterall;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(com.epam.reportportal.junit5.BeforeAfterAllTest.BeforeAfterAllTestExtension.class)
public class BeforeAfterAllFailedTest {
	public static final String CLASS_ID = "e3a09449-b508-485d-aa9b-b6b2cf4b4fd5";

	@BeforeAll
	public static void beforeAll() {
		System.out.println("Before all: " + CLASS_ID);
	}

	@Test
	public void testBeforeAfterAllFailed() {
		throw new IllegalStateException("Test: " + CLASS_ID);
	}

	@AfterAll
	public static void afterAll() {
		System.out.println("After all: " + CLASS_ID);
	}
}
