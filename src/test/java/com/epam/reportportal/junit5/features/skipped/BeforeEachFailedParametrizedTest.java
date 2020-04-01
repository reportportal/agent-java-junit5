package com.epam.reportportal.junit5.features.skipped;

import com.epam.reportportal.junit5.miscellaneous.FailedBeforeEachReportsSkippedTestTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@ExtendWith(FailedBeforeEachReportsSkippedTestTest.SkippedTestExtension.class)
public class BeforeEachFailedParametrizedTest {

	public enum TestParams {
		ONE,
		TWO
	}

	@BeforeEach
	public void beforeEachFailed() {
		throw new IllegalStateException("Before each");
	}

	@ParameterizedTest
	@EnumSource(TestParams.class)
	public void testBeforeEachFailed(TestParams param) {
		System.out.println("Test: testBeforeEachFailed - " + param.name());
	}
}
