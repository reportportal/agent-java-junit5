package com.epam.reportportal.junit5.features.issue;

import com.epam.reportportal.annotations.Issue;
import com.epam.reportportal.junit5.DisplayNameTest;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@ExtendWith(DisplayNameTest.TestExtension.class)
public class TwoDynamicIssueTest {
	public static final String FAILURE_MESSAGE = "This test is expected to fail";

	@TestFactory
	@Issue(value = "ab001", comment = FAILURE_MESSAGE)
	Stream<DynamicTest> testForTestFactory() {
		return Stream.of(dynamicTest("My dynamic test", () -> {
			throw new IllegalStateException(FAILURE_MESSAGE);
		}), dynamicTest("My dynamic test 2", () -> {
			throw new IllegalStateException(FAILURE_MESSAGE);
		}));
	}
}
