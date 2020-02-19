package com.epam.reportportal.junit5;

import com.epam.reportportal.annotations.Step;
import com.epam.reportportal.aspect.StepAspect;
import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class NestedStepTest {

	private static final String NESTED_STEP_NAME_TEMPLATE = "I am nested step with parameter - '{param}'";

	private static final String METHOD_WITH_INNER_METHOD_NAME_TEMPLATE = "I am method with inner method";
	private static final String INNER_METHOD_NAME_TEMPLATE = "I am - {method}";

	@Mock
	private Launch launch;

	@Mock
	private Maybe<String> parentId;

	@BeforeEach
	public void init() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void nestedTest() {
		final String param = "test param";

		StepAspect.addLaunch("test launch", launch);
		StepAspect.setParentId(parentId);

		Maybe<String> firstItemMaybe = createItemIdMaybe("first item");
		when(launch.startTestItem(any(), any())).thenReturn(firstItemMaybe);

		method(param);

		ArgumentCaptor<StartTestItemRQ> nestedStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<FinishTestItemRQ> finishNestedCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch, times(1)).startTestItem(any(), nestedStepCaptor.capture());
		verify(launch, times(1)).finishTestItem(any(), finishNestedCaptor.capture());

		StartTestItemRQ startTestItemRQ = nestedStepCaptor.getValue();

		assertNotNull(startTestItemRQ);
		assertFalse(startTestItemRQ.isHasStats());
		assertEquals("I am nested step with parameter - '" + param + "'", startTestItemRQ.getName());

		FinishTestItemRQ finishNestedRQ = finishNestedCaptor.getValue();
		assertNotNull(finishNestedRQ);
		assertEquals("PASSED", finishNestedRQ.getStatus());

	}

	@Step(NESTED_STEP_NAME_TEMPLATE)
	public void method(String param) {

	}

	@Test
	public void failedNestedTest() {
		final String param = "test param";

		StepAspect.addLaunch("test launch", launch);
		StepAspect.setParentId(parentId);

		Maybe<String> firstItemMaybe = createItemIdMaybe("first item");
		when(launch.startTestItem(any(), any())).thenReturn(firstItemMaybe);

		try {
			failedMethod(param);
		} catch (Exception ex) {
			//to prevent this test failing
		}

		ArgumentCaptor<StartTestItemRQ> nestedStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<FinishTestItemRQ> finishNestedCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch, times(1)).startTestItem(any(), nestedStepCaptor.capture());
		//2 times to finish nested and it's parent
		verify(launch, times(2)).finishTestItem(any(), finishNestedCaptor.capture());

		StartTestItemRQ startTestItemRQ = nestedStepCaptor.getValue();

		assertNotNull(startTestItemRQ);
		assertFalse(startTestItemRQ.isHasStats());
		assertEquals("I am nested step with parameter - '" + param + "'", startTestItemRQ.getName());

		FinishTestItemRQ finishNestedRQ = finishNestedCaptor.getValue();
		assertNotNull(finishNestedRQ);
		assertEquals("FAILED", finishNestedRQ.getStatus());

	}

	@Step(NESTED_STEP_NAME_TEMPLATE)
	public void failedMethod(String param) {
		throw new RuntimeException("Some random error");
	}

	@Test
	public void testWithMultiLevelNested() throws NoSuchMethodException {
		StepAspect.addLaunch("test launch", launch);
		StepAspect.setParentId(parentId);
		ArgumentCaptor<StartTestItemRQ> nestedStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);

		Maybe<String> firstItemMaybe = createItemIdMaybe("first item");
		when(launch.startTestItem(any(), any())).thenReturn(firstItemMaybe);

		methodWithInnerMethod();

		verify(launch, times(2)).startTestItem(any(), nestedStepCaptor.capture());

		List<StartTestItemRQ> nestedSteps = nestedStepCaptor.getAllValues();

		nestedSteps.forEach(step -> {
			assertNotNull(step);
			assertFalse(step.isHasStats());
		});

		StartTestItemRQ stepWithInnerStep = nestedSteps.get(0);
		assertEquals(METHOD_WITH_INNER_METHOD_NAME_TEMPLATE, stepWithInnerStep.getName());

		StartTestItemRQ innerStep = nestedSteps.get(1);
		assertEquals("I am - " + NestedStepTest.class.getDeclaredMethod("innerMethod").getName(), innerStep.getName());
	}

	@Step(METHOD_WITH_INNER_METHOD_NAME_TEMPLATE)
	public void methodWithInnerMethod() {
		innerMethod();
	}

	@Step(INNER_METHOD_NAME_TEMPLATE)
	public void innerMethod() {

	}

	private Maybe<String> createItemIdMaybe(String id) {
		return Maybe.create(emitter -> {
			emitter.onSuccess(id);
			emitter.onComplete();
		});
	}
}
