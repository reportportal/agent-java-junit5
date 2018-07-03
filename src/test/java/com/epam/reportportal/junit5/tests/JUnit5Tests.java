package com.epam.reportportal.junit5.tests;

import org.junit.jupiter.api.*;

public class JUnit5Tests {

	@BeforeEach
	void initializeBaseClass() {
		System.out.println("base-class-before-each");
	}

	@Test
	@Tag("tag1")
	@Tag("tag2")
	void baseClassTest() {
		System.out.println("base-class-test");
	}

	@Nested
	class FirstContext {

		@BeforeEach
		void initializeFirstNesting() {
			System.out.println("nested-before-each");
		}

		@RepeatedTest(10)
		void firstNestedTest() {
			System.out.println("first-nested-test");
		}
		
		@Test
		void secondNestedTest() {
            System.out.println("second-nested-test");
		}

		@AfterEach
		void afterFirstContext() {
			System.out.println("nested-after-each");
		}
		
		@Nested
		class SecondContext {
	        @BeforeEach
	        void initializeSecondNesting() {
	            System.out.println("nested-before-each");
	        }

	        @RepeatedTest(10)
	        void firstNestedTest() {
	            System.out.println("first-nested-test");
	        }
	        
	        @Test
	        void secondNestedTest() {
	            System.out.println("second-nested-test");
	        }

	        @AfterEach
	        void afterFirstContext() {
	            System.out.println("nested-after-each");
	        }
		}

	}
}
