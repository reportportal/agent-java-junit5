package com.epam.reportportal.junit5.features.beforeafterall;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(com.epam.reportportal.junit5.BeforeAfterAllTest.BeforeAfterAllTestExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NonStaticBeforeAfterAllTest {
	public static final String CLASS_ID = "d93ecaae-2161-4c1c-8b68-124ed4dcb4f1";

	@BeforeAll
	public void beforeAll() {
		System.out.println("Before all: " + CLASS_ID);
	}

	@Test
	public void testBeforeAfterAll() {
		System.out.println("Test: " + CLASS_ID);
	}

	@AfterAll
	public void afterAll() {
		System.out.println("After all: " + CLASS_ID);
	}
}
