package com.epam.reportportal.junit5.features.displayname;

import com.epam.reportportal.annotations.DisplayName;
import com.epam.reportportal.junit5.DisplayNameTest;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@ExtendWith(DisplayNameTest.TestExtension.class)
public class DisplayNameAnnotatedMethodDynamicTest {
	public static final String TEST_DISPLAY_NAME_DYNAMIC_METHOD = "My test displayName on the dynamic method";

	@TestFactory
	@DisplayName(TEST_DISPLAY_NAME_DYNAMIC_METHOD)
	Stream<DynamicTest> testForTestFactory() {
		return Stream.of(dynamicTest("My dynamic test", () -> System.out.println("Inside dynamic test")));
	}
}
