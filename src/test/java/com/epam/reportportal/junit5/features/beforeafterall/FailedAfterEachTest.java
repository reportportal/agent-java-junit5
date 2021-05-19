package com.epam.reportportal.junit5.features.beforeafterall;

import com.epam.reportportal.junit5.BeforeAfterAllTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BeforeAfterAllTest.BeforeAfterAllTestExtension.class)
public class FailedAfterEachTest {

	@Test
	public void testAfterEachFailed() {
		System.out.println("Test: testAfterEachFailed");
	}

	@AfterEach
	public void afterEach() {
		throw new IllegalStateException("After each: afterEach");
	}
}
