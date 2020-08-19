package com.epam.reportportal.junit5.features.nested;

import com.epam.reportportal.annotations.Step;
import com.epam.reportportal.junit5.NestedStepTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.epam.reportportal.junit5.NestedStepTest.NESTED_STEP_NAME_TEMPLATE;
import static com.epam.reportportal.junit5.NestedStepTest.PARAM;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@ExtendWith(NestedStepTest.TestExtension.class)
public class NestedStepWithBeforeEachTest {

	@BeforeEach
	public void before() {
		method(PARAM);
	}

	@Test()
	public void test() {

	}

	@Step(NESTED_STEP_NAME_TEMPLATE)
	public void method(String param) {

	}
}
