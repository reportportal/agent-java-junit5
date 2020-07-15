package com.epam.reportportal.junit5.features.nested;

import com.epam.reportportal.junit5.JunitNestedTestTest;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(JunitNestedTestTest.TestExtension.class)
public class JunitDynamicNestedTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(JunitDynamicNestedTest.class);

	@TestFactory
	Stream<DynamicNode> dynamicTestsWithContainers() {
		return Stream.of("A", "B")
				.map(input -> DynamicContainer.dynamicContainer(input,
						Stream.of(DynamicContainer.dynamicContainer(input + " inner container",
								Stream.of(DynamicTest.dynamicTest(input + " Test 1", () -> {
									LOGGER.info("Checking length == 1");
									assertThat(input, hasLength(1));
								}), DynamicTest.dynamicTest(input + " Test 2", () -> {
									LOGGER.info("Checking not empty");
									assertThat(input, not(emptyOrNullString()));
								}))
						))
				));
	}
}
