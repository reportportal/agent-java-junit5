package com.epam.reportportal.junit5.features;

import com.epam.reportportal.service.ReportPortal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class JUnit5Tests {

	@BeforeAll
	static void beforeAll() {
		System.out.println("before-all");
	}

	@BeforeEach
	void initializeBaseClass() {
		System.out.println("base-class-before-each");
	}

	@Test
	@Tag("tag1")
	@Tag("tag2")
	void baseClassTest() throws IOException {
		// Report launch log
		File file = File.createTempFile("rp-test", ".css");
		Resources.asByteSource(Resources.getResource("files/css.css")).copyTo(Files.asByteSink(file));
		ReportPortal.emitLaunchLog("LAUNCH LOG MESAGE WITH ATTACHMENT", "error", new Date(), file);

		System.out.println("base-class-test");
	}

	private static List<String> testData() {
		return Arrays.asList("first", "second", "third");
	}

	@Disabled
	@Test
	void disabledTest() {
		System.out.println("disabled");
	}

	@ParameterizedTest
	@MethodSource("testData")
	void parameterizedTestWithMethodSource(String value) {
		System.out.println("parameterized-test-with-method-source, parameter: " + value);
	}

	@ParameterizedTest
	@CsvSource({ "first", "second", "third" })
	void parameterizedTestWithCsvSource(String value) {
		System.out.println("parameterized-test-with-csv-source, parameter: " + value);
	}

	@ParameterizedTest
	@EnumSource(value = DayOfWeek.class, names = { "MONDAY", "WEDNESDAY", "FRIDAY" })
	void parameterizedTestWithEnumSource(DayOfWeek day) {
		System.out.println("parameterized-test-with-enum-source, parameter: " + day);
	}

	@ParameterizedTest
	@NullSource
	void parameterizedTestWithNullSource(String value) {
		System.out.println("parameterized-test-with-null-source, parameter: " + value);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "a", "b", "c" })
	void parameterizedTestWithNullSourceAndValueSource(String value) {
		System.out.println("parameterized-test-with-null-source-and-value-source, parameter: " + value);
	}

	@ParameterizedTest
	@EmptySource
	void parameterizedTestWithEmptySource(String value) {
		System.out.println("parameterized-test-with-empty-source, parameter: " + value);
	}

	@ParameterizedTest
	@EmptySource
	@ValueSource(strings = { "a", "b", "c" })
	void parameterizedTestWithEmptySourceAndValueSource(String value) {
		System.out.println("parameterized-test-with-empty-source-and-value-source, parameter: " + value);
	}

	@ParameterizedTest
	@NullSource
	@EmptySource
	@ValueSource(strings = { "a", "b", "c" })
	void parameterizedTestWithNullSourceAndEmptySourceAndValueSource(String value) {
		System.out.println("parameterized-test-with-null-source-empty-source-and-value-source, parameter: " + value);
	}

	@ParameterizedTest
	@NullAndEmptySource
	void parameterizedTestWithNullAndEmptySource(String value) {
		System.out.println("parameterized-test-with-null-and-empty-source, parameter: " + value);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "a", "b", "c" })
	void parameterizedTestWithNullAndEmptySourceAndValueSource(String value) {
		System.out.println("parameterized-test-with-null-and-empty-source-and-value-source, parameter: " + value);
	}

	@ParameterizedTest
	@ValueSource(floats = { 1.1f, 2.2f, 3.3f })
	void parameterizedTestWithFloatsValueSource(float value) {
		System.out.println("parameterized-test-with-value-source, parameter: " + value);
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 3 })
	void parameterizedTestWithIntValueSource(int value) {
		System.out.println("parameterized-test-with-value-source, parameter: " + value);
	}

	@ParameterizedTest
	@ValueSource(shorts = { 1, 2, 3 })
	void parameterizedTestWithShortsValueSource(short value) {
		System.out.println("parameterized-test-with-value-source, parameter: " + value);
	}

	@ParameterizedTest
	@ValueSource(bytes = { 1, 2, 3 })
	void parameterizedTestWithBytesValueSource(byte value) {
		System.out.println("parameterized-test-with-value-source, parameter: " + value);
	}

	@ParameterizedTest
	@ValueSource(chars = { 'a', 'b', 'c' })
	void parameterizedTestWithCharsValueSource(char value) {
		System.out.println("parameterized-test-with-value-source, parameter: " + value);
	}

	@ParameterizedTest
	@ValueSource(doubles = { 1.1, 2.2, 3.3 })
	void parameterizedTestWithDoublesValueSource(double value) {
		System.out.println("parameterized-test-with-value-source, parameter: " + value);
	}

	@ParameterizedTest
	@ValueSource(longs = { 1147483648L, 2147483648L, 3147483648L })
	void parameterizedTestWithLongsValueSource(long value) {
		System.out.println("parameterized-test-with-value-source, parameter: " + value);
	}

	@ParameterizedTest
	@ValueSource(strings = { "a", "b", "c" })
	void parameterizedTestWithStringsValueSource(String value) {
		System.out.println("parameterized-test-with-value-source, parameter: " + value);
	}

	@TestFactory
	Stream<DynamicTest> testForTestFactory() {
		return testData().stream()
				.map(testData -> dynamicTest("Check Test Factory " + testData,
						() -> System.out.println("test-for-test-factory, test " + testData)
				));
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
