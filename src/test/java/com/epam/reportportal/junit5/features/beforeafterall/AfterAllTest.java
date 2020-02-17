package com.epam.reportportal.junit5.features.beforeafterall;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(com.epam.reportportal.junit5.BeforeAfterAllTest.BeforeAfterAllTestExtension.class)
public class AfterAllTest {
	public static final String CLASS_ID = "3b6f28a9-4988-464e-b07f-17ab9f3abe72";

	@Test
	public void testAfterAll() {
		System.out.println("Test: " + CLASS_ID);
	}

	@AfterAll
	public static void afterAll() {
		System.out.println("After all: " + CLASS_ID);
	}
}
