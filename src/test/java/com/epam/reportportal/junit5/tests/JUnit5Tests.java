package com.epam.reportportal.junit5.tests;

import org.junit.jupiter.api.*;

public class JUnit5Tests {

	@BeforeEach
	void initializeBaseClass() {
		System.out.println("Before each");
	}

	@Test
	@Tag("tag1")
	@Tag("tag2")
	void baseClassTest() {
		System.out.println("test");
	}

	@Nested
	class FirstContext {

		@BeforeEach
		void initializeFirstNesting() {
			System.out.println("nested - before each");
		}

		@Test
		@RepeatedTest(10)
		void firstNestedTest() {
			System.out.println("nested - test");
		}

		@AfterEach
		void afterFirstContext() {
			System.out.println("nested - after each");
		}

	}
}
