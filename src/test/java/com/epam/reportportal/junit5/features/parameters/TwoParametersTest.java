package com.epam.reportportal.junit5.features.parameters;

import com.epam.reportportal.junit5.ParametersTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(ParametersTest.ParameterTestExtension.class)
public class TwoParametersTest {

	@ParameterizedTest
	@CsvSource({ "one, two", "three, four" })
	public void testTwoParameters(String param1, String param2) {
		System.out.println("Two parameters test: " + param1 + " - " + param2);
	}
}
