package com.epam.reportportal.junit5.features.parameters;

import com.epam.reportportal.annotations.ParameterKey;
import com.epam.reportportal.junit5.ParametersTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(ParametersTest.ParameterTestExtension.class)
public class ParameterNamesTest {

	public static final String FIRST_PARAMETER_NAME = "first_parameter";
	public static final String SECOND_PARAMETER_NAME = "second_parameter";

	@ParameterizedTest
	@CsvSource({ "one, two", "three, four" })
	public void testTwoParameters(@ParameterKey(FIRST_PARAMETER_NAME) String param1, @ParameterKey(SECOND_PARAMETER_NAME) String param2) {
		System.out.println("Two parameters test: " + param1 + " - " + param2);
	}
}
