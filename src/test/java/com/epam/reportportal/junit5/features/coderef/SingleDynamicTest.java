package com.epam.reportportal.junit5.features.coderef;

import com.epam.reportportal.junit5.CodeReferenceTest;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@ExtendWith(CodeReferenceTest.CodeReferenceTestExtension.class)
public class SingleDynamicTest {

	public static final String TEST_CASE_DISPLAY_NAME = UUID.randomUUID().toString();

	@TestFactory
	Stream<DynamicTest> testForTestFactory() {
		return Stream.of(dynamicTest(TEST_CASE_DISPLAY_NAME, () -> System.out.println("Test factory test: " + TEST_CASE_DISPLAY_NAME)));
	}
}
