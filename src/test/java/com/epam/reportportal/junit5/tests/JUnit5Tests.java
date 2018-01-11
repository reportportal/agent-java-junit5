package com.epam.reportportal.junit5.tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class JUnit5Tests {

	@BeforeEach
	void initializeBaseClass() {
		System.out.println("Before each");
	}

	@Test
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
		void firstNestedTest() {
			System.out.println("nested - test");
		}

		@AfterEach
		void afterFirstContext() {
			System.out.println("nested - after each");
		}

	}
}
