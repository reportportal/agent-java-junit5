package com.epam.reportportal.junit5.features.nested;

import com.epam.reportportal.annotations.Step;
import com.epam.reportportal.junit5.NestedStepTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.epam.reportportal.junit5.NestedStepTest.INNER_METHOD_NAME_TEMPLATE;
import static com.epam.reportportal.junit5.NestedStepTest.METHOD_WITH_INNER_METHOD_NAME_TEMPLATE;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@ExtendWith(NestedStepTest.TestExtension.class)
public class NestedStepMultiLevelTest {

	@Test
	public void test() {
		methodWithInnerMethod();
	}

	@Step(METHOD_WITH_INNER_METHOD_NAME_TEMPLATE)
	public void methodWithInnerMethod() {
		innerMethod();
	}

	@Step(INNER_METHOD_NAME_TEMPLATE)
	public void innerMethod() {

	}
}
