package com.epam.reportportal.junit5.features.skipped;

import com.epam.reportportal.junit5.miscellaneous.FailedBeforeEachReportsSkippedTestTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(FailedBeforeEachReportsSkippedTestTest.SkippedTestExtension.class)
public class AfterEachFailedTest {

	@BeforeEach
	public void beforeEachFailed() {
		System.out.println("Before each");
	}

	@Test
	public void testAfterEachFailed() {
		System.out.println("Test: testAfterEachFailed");
	}

	@AfterEach
	public void afterEachFailed() {
		throw new IllegalStateException("After each");
	}
}
