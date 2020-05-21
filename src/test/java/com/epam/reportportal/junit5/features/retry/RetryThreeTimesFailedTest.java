package com.epam.reportportal.junit5.features.retry;

import com.epam.reportportal.junit5.ReportPortalExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(ReportPortalExtension.class)
public class RetryThreeTimesFailedTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(RetryThreeTimesFailedTest.class);

	private static final AtomicInteger testInvocationNumber = new AtomicInteger();

	@BeforeEach
	public void setUp1() {
		LOGGER.info("Inside @BeforeMethod setUp1 step");
	}

	@BeforeEach
	public void setUp2() {
		LOGGER.info("Inside @BeforeMethod setUp2 step");
	}

	@RepeatedTest(3)
	public void invocationCountTest() {
		if (testInvocationNumber.incrementAndGet() == 2) {
			Assertions.fail("Failed inside @Test invocationCountTest step");
		}
		LOGGER.info("Inside @Test invocationCountTest step");
	}

	@AfterEach
	public void shutDown() {
		LOGGER.info("Inside @AfterMethod shutDown method");
	}
}
