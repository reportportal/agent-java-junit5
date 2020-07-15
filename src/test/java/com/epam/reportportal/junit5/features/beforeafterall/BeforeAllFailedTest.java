package com.epam.reportportal.junit5.features.beforeafterall;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(com.epam.reportportal.junit5.BeforeAfterAllTest.BeforeAfterAllTestExtension.class)
public class BeforeAllFailedTest {
	public static final String CLASS_ID = "710b184b-4f81-403c-8310-9a54f56b687b";

	@BeforeAll
	public static void beforeAllFailed() {
		throw new IllegalStateException("Before all: " + CLASS_ID);
	}

	@Test
	public void testBeforeAllFailed() {
		System.out.println("Test: " + CLASS_ID);
	}
}
