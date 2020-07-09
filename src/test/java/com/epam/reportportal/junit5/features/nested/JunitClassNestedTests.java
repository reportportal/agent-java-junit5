package com.epam.reportportal.junit5.features.nested;

import com.epam.reportportal.junit5.JunitNestedTestTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(JunitNestedTestTest.TestExtension.class)
public class JunitClassNestedTests {
	private static final Logger LOGGER = LoggerFactory.getLogger(JunitClassNestedTests.class);

	@Nested
	class Outer {
		@Nested
		class Inner {
			@Test
			void aTest() {
				LOGGER.info("executing aTest");
				Assertions.assertEquals(1, 1);
			}

			@Test
			void anotherTest() {
				LOGGER.info("executing anotherTest");
				Assertions.assertEquals(1, 1);
			}
		}
	}
}
