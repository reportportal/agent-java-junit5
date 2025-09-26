package com.epam.reportportal.junit5.features.skipped;

import com.epam.reportportal.junit5.miscellaneous.FailedBeforeEachSkippedTestOrderTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(FailedBeforeEachSkippedTestOrderTest.SkippedTestExtension.class)
public class BeforeEachFailedWithAfterEachTest {
	@BeforeEach
	public void beforeEachFailed() throws InterruptedException {
		Thread.sleep(2);
		throw new IllegalStateException("Before each");
	}

	@Test
	public void testBeforeEachFailed() {
		System.out.println("Test: testBeforeEachFailed");
	}

	@AfterEach
	public void afterEach() {
		System.out.println("After Each");
	}
}
