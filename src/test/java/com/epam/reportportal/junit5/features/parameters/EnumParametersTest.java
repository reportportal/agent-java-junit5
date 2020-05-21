package com.epam.reportportal.junit5.features.parameters;

import com.epam.reportportal.junit5.ParametersTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@ExtendWith(ParametersTest.ParameterTestExtension.class)
public class EnumParametersTest {

	public enum TestParams {
		ONE,
		TWO
	}

	@ParameterizedTest
	@EnumSource(TestParams.class)
	public void testParameters(TestParams param) throws InterruptedException {
		System.out.println("Test: " + param.name());
	}

}
