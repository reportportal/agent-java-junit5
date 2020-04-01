package com.epam.reportportal.junit5.features.skipped;

import com.epam.reportportal.junit5.miscellaneous.FailedBeforeEachReportsSkippedTestTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(FailedBeforeEachReportsSkippedTestTest.SkippedTestExtension.class)
public class BeforeEachFailedTest {

	@BeforeEach
	public void beforeEachFailed() {
		throw new IllegalStateException("Before each");
	}

	@Test
	public void testBeforeEachFailed() {
		System.out.println("Test: testBeforeEachFailed");
	}
}
