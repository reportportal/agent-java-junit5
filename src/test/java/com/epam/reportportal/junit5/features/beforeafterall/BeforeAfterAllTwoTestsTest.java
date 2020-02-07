package com.epam.reportportal.junit5.features.beforeafterall;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(com.epam.reportportal.junit5.BeforeAfterAllTest.BeforeAfterAllTestExtension.class)
public class BeforeAfterAllTwoTestsTest {
	public static final String CLASS_ID = "7a9347b3-4d66-4133-88b7-60d52db61436";

	@BeforeAll
	public static void beforeAll() {
		System.out.println("Before all: " + CLASS_ID);
	}

	@Test
	public void testFirstBeforeAfterAll() {
		System.out.println("First Test: " + CLASS_ID);
	}

	@Test
	public void testSecondBeforeAfterAll() {
		System.out.println("Second Test: " + CLASS_ID);
	}

	@AfterAll
	public static void afterAll() {
		System.out.println("After all: " + CLASS_ID);
	}
}
