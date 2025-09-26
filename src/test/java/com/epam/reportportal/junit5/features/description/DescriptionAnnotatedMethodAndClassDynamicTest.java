package com.epam.reportportal.junit5.features.description;

import com.epam.reportportal.annotations.Description;
import com.epam.reportportal.junit5.DescriptionTest;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@ExtendWith(DescriptionTest.TestExtension.class)
@Description(DescriptionAnnotatedClassDynamicTest.TEST_DESCRIPTION_DYNAMIC_CLASS)
public class DescriptionAnnotatedMethodAndClassDynamicTest {
	@TestFactory
	@Description(DescriptionAnnotatedMethodDynamicTest.TEST_DESCRIPTION_DYNAMIC_METHOD)
	Stream<DynamicTest> testForTestFactory() {
		return Stream.of(dynamicTest("My dynamic test", () -> System.out.println("Inside dynamic test")));
	}
}
