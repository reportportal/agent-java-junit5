package com.epam.reportportal.junit5.features.beforeafterall;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(com.epam.reportportal.junit5.BeforeAfterAllTest.BeforeAfterAllTestExtension.class)
public class BeforeAllTest {
	public static final String CLASS_ID = "8b2354a9-4448-4189-ae5d-f74f547f0f66";

	@BeforeAll
	public static void beforeAll() {
		System.out.println("Before all: " + CLASS_ID);
	}

	@Test
	public void testBeforeAll() {
		System.out.println("Test: " + CLASS_ID);
	}

	@Test
	@Disabled
	public void testDisabled() {
		System.out.println("Test: " + CLASS_ID);
	}

}
