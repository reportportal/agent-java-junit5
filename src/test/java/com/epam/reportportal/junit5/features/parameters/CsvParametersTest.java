package com.epam.reportportal.junit5.features.parameters;

import com.epam.reportportal.junit5.ParametersTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(ParametersTest.ParameterTestExtension.class)
public class CsvParametersTest {

	@ParameterizedTest
	@CsvSource({"one", "two, three"})
	public void testParameters(String param) {
		System.out.println("Csv parameter test: " + param);
	}
}
