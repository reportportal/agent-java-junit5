package com.epam.reportportal.junit5.tests;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class JUnit5Tests {

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

	private static List<String> testData() {
	    return Arrays.asList(
	        "first", "second", "third"
        );
    }

	@ParameterizedTest
    @MethodSource("testData")
    void parameterizedTestWithMethodSource(String value) {
        System.out.println("parameterized-test-with-method-source, parameter: " + value);
    }

    @ParameterizedTest
    @CsvSource({"first", "second", "third"})
    void parameterizedTestWithCsvSource(String value) {
        System.out.println("parameterized-test-with-csv-source, parameter: " + value);
    }

    @ParameterizedTest
    @EnumSource(value = DayOfWeek.class, names = {"MONDAY", "WEDNESDAY", "FRIDAY"})
    void parameterizedTestWithEnumSource(DayOfWeek day) {
        System.out.println("parameterized-test-with-enum-source, parameter: " + day);
    }

    @ParameterizedTest
    @ValueSource(floats = {1.1f, 2.2f, 3.3f})
    void parameterizedTestWithFloatsValueSource(float value) {
        System.out.println("parameterized-test-with-value-source, parameter: " + value);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void parameterizedTestWithIntValueSource(int value) {
        System.out.println("parameterized-test-with-value-source, parameter: " + value);
    }

    @ParameterizedTest
    @ValueSource(shorts = {1, 2, 3})
    void parameterizedTestWithShortsValueSource(short value) {
        System.out.println("parameterized-test-with-value-source, parameter: " + value);
    }

    @ParameterizedTest
    @ValueSource(bytes = {1, 2, 3})
    void parameterizedTestWithBytesValueSource(byte value) {
        System.out.println("parameterized-test-with-value-source, parameter: " + value);
    }

    @ParameterizedTest
    @ValueSource(chars = {'a', 'b', 'c'})
    void parameterizedTestWithCharsValueSource(char value) {
        System.out.println("parameterized-test-with-value-source, parameter: " + value);
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.1, 2.2, 3.3})
    void parameterizedTestWithDoublesValueSource(double value) {
        System.out.println("parameterized-test-with-value-source, parameter: " + value);
    }

    @ParameterizedTest
    @ValueSource(longs = {1147483648L, 2147483648L, 3147483648L})
    void parameterizedTestWithLongsValueSource(long value) {
        System.out.println("parameterized-test-with-value-source, parameter: " + value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "b", "c"})
    void parameterizedTestWithStringsValueSource(String value) {
        System.out.println("parameterized-test-with-value-source, parameter: " + value);
    }

    @TestFactory
    Stream<DynamicTest> testForTestFactory() {
        return testData().stream()
            .map(testData ->
                     dynamicTest("Check Test Factory " + testData, () ->
                         System.out.println("test-for-test-factory, test " + testData)
                     )
            );
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
