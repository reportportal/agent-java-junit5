package com.epam.reportportal.junit5.features.nested;

import com.epam.reportportal.junit5.JunitNestedTestTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(JunitNestedTestTest.TestExtension.class)
public class JunitClassNestedTestsLongDepth {
	private static final Logger LOGGER = LoggerFactory.getLogger(JunitClassNestedTests.class);

	@Nested
	class Outer {
		@Nested
		class Inner {
			@Nested
			class TestOuter {
				@Nested
				class TestInner {
					@Test
					void aTest() {
						LOGGER.info("executing aTest");
						Assertions.assertEquals(1, 1);
					}
				}
			}

			@Test
			void bTest() {
				LOGGER.info("executing bTest");
				Assertions.assertEquals(1, 1);
			}
		}
	}

	@Nested
	class SecondOuter {
		@Nested
		class SecondInner {
			@Nested
			class SecondTestOuter {
				@Nested
				class SecondTestInner {
					@Test
					void cTest() {
						LOGGER.info("executing cTest");
						Assertions.assertEquals(1, 1);
					}
				}
			}
		}
	}
}
