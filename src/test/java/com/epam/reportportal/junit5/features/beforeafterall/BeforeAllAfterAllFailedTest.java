package com.epam.reportportal.junit5.features.beforeafterall;

import com.epam.reportportal.junit5.BeforeAfterAllTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BeforeAfterAllTest.BeforeAfterAllTestExtension.class)
public class BeforeAllAfterAllFailedTest {
	public static final String CLASS_ID = "28b580fa-a7c0-4a7e-b0bc-54f196565d69";

	@BeforeAll
	public static void beforeAllFailed() {
		System.out.println("Before all: " + CLASS_ID);
	}

	@Test
	public void testBeforeAllFailed() {
		System.out.println("Test: " + CLASS_ID);
	}

	@AfterAll
	public static void afterAllFailed() {
		throw new IllegalStateException("After all: " + CLASS_ID);
	}

}
