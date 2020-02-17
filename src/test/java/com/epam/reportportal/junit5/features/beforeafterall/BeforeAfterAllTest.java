package com.epam.reportportal.junit5.features.beforeafterall;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(com.epam.reportportal.junit5.BeforeAfterAllTest.BeforeAfterAllTestExtension.class)
public class BeforeAfterAllTest {
	public static final String CLASS_ID = "96a167df-0353-41cf-8d4d-dcba8368ddac";

	@BeforeAll
	public static void beforeAll() {
		System.out.println("Before all: " + CLASS_ID);
	}

	@Test
	public void testBeforeAfterAll() {
		System.out.println("Test: " + CLASS_ID);
	}

	@AfterAll
	public static void afterAll() {
		System.out.println("After all: " + CLASS_ID);
	}
}
