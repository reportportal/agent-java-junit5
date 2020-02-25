package com.epam.reportportal.junit5;

import com.epam.reportportal.junit5.features.nested.NestedStepFeatureFailedTest;
import com.epam.reportportal.junit5.features.nested.NestedStepFeaturePassedTest;
import com.epam.reportportal.junit5.features.nested.NestedStepMultiLevelTest;
import com.epam.reportportal.junit5.features.nested.NestedStepWithBeforeEachTest;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

import static com.epam.reportportal.junit5.NestedStepTest.NestedStepExtension.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class NestedStepTest {

	public static final String PARAM = "test param";

	public static final String NESTED_STEP_NAME_TEMPLATE = "I am nested step with parameter - '{param}'";

	public static final String METHOD_WITH_INNER_METHOD_NAME_TEMPLATE = "I am method with inner method";
	public static final String INNER_METHOD_NAME_TEMPLATE = "I am - {method}";

	public static class NestedStepExtension extends ReportPortalExtension {

		static final ThreadLocal<Maybe<String>> TEST_METHOD_ID = new ThreadLocal<>();
		static final ThreadLocal<Maybe<String>> NESTED_STEP_ID = new ThreadLocal<>();
		static final ThreadLocal<Maybe<String>> INNER_NESTED_STEP_ID = new ThreadLocal<>();

		static final ThreadLocal<Launch> LAUNCH = new ThreadLocal<>();
		final static ThreadLocal<String> LAUNCH_ID = new ThreadLocal<>();
		static final ThreadLocal<ListenerParameters> LISTENER_PARAMETERS = new ThreadLocal<>();
		static final ThreadLocal<ReportPortal> REPORT_PORTAL = new ThreadLocal<>();

		public static Maybe<String> createIdMaybe(String id) {
			return Maybe.create(emitter -> {
				emitter.onSuccess(id);
				emitter.onComplete();
			});
		}

		public NestedStepExtension() {
			LAUNCH.set(mock(Launch.class));
			Maybe<String> rootItemId = createIdMaybe("Root item id");
			when(LAUNCH.get().startTestItem(any())).thenReturn(rootItemId);

			Maybe<String> launchId = createIdMaybe("Launch " + UUID.randomUUID().toString());
			LAUNCH_ID.set(launchId.blockingGet());

			TEST_METHOD_ID.set(createIdMaybe("Test method id"));
			when(LAUNCH.get().startTestItem(eq(rootItemId), any())).thenReturn(TEST_METHOD_ID.get());

			NESTED_STEP_ID.set(createIdMaybe("Nested step id"));
			when(LAUNCH.get().startTestItem(eq(TEST_METHOD_ID.get()), any())).thenReturn(NESTED_STEP_ID.get());

			INNER_NESTED_STEP_ID.set(createIdMaybe("Inner nested step id"));
			when(LAUNCH.get().startTestItem(eq(NESTED_STEP_ID.get()), any())).thenReturn(INNER_NESTED_STEP_ID.get());

			REPORT_PORTAL.set(mock(ReportPortal.class));
			when(REPORT_PORTAL.get().newLaunch(any())).thenReturn(LAUNCH.get());

			LISTENER_PARAMETERS.set(mock(ListenerParameters.class));
			when(REPORT_PORTAL.get().getParameters()).thenReturn(LISTENER_PARAMETERS.get());
		}

		@Override
		ReportPortal getReporter() {
			return REPORT_PORTAL.get();
		}

		@Override
		String getLaunchId(ExtensionContext context) {
			return LAUNCH_ID.get();
		}
	}

	@BeforeEach
	public void init() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void nestedTest() {

		TestUtils.runClasses(NestedStepFeaturePassedTest.class);

		ArgumentCaptor<StartTestItemRQ> nestedStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<FinishTestItemRQ> finishNestedCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(LAUNCH.get(), times(1)).startTestItem(eq(TEST_METHOD_ID.get()), nestedStepCaptor.capture());
		verify(LAUNCH.get(), times(1)).finishTestItem(eq(NESTED_STEP_ID.get()), finishNestedCaptor.capture());

		StartTestItemRQ startTestItemRQ = nestedStepCaptor.getValue();

		assertNotNull(startTestItemRQ);
		assertFalse(startTestItemRQ.isHasStats());
		assertEquals("I am nested step with parameter - '" + PARAM + "'", startTestItemRQ.getName());

		FinishTestItemRQ finishNestedRQ = finishNestedCaptor.getValue();
		assertNotNull(finishNestedRQ);
		assertEquals("PASSED", finishNestedRQ.getStatus());

	}

	@Test
	public void nestedInBeforeMethodTest() {
		TestUtils.runClasses(NestedStepWithBeforeEachTest.class);

		ArgumentCaptor<StartTestItemRQ> nestedStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<FinishTestItemRQ> finishNestedCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(LAUNCH.get(), times(1)).startTestItem(eq(TEST_METHOD_ID.get()), nestedStepCaptor.capture());
		verify(LAUNCH.get(), times(1)).finishTestItem(eq(NESTED_STEP_ID.get()), finishNestedCaptor.capture());

		StartTestItemRQ startTestItemRQ = nestedStepCaptor.getValue();

		assertNotNull(startTestItemRQ);
		assertFalse(startTestItemRQ.isHasStats());
		assertEquals("I am nested step with parameter - '" + PARAM + "'", startTestItemRQ.getName());

		FinishTestItemRQ finishNestedRQ = finishNestedCaptor.getValue();
		assertNotNull(finishNestedRQ);
		assertEquals("PASSED", finishNestedRQ.getStatus());

	}

	@Test
	public void failedNestedTest() {

		try {
			TestUtils.runClasses(NestedStepFeatureFailedTest.class);
		} catch (Exception ex) {
			//to prevent this test failing
		}

		ArgumentCaptor<StartTestItemRQ> nestedStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<FinishTestItemRQ> finishNestedCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(LAUNCH.get(), times(1)).startTestItem(eq(TEST_METHOD_ID.get()), nestedStepCaptor.capture());
		verify(LAUNCH.get(), times(1)).finishTestItem(eq(NESTED_STEP_ID.get()), finishNestedCaptor.capture());

		StartTestItemRQ startTestItemRQ = nestedStepCaptor.getValue();

		assertNotNull(startTestItemRQ);
		assertFalse(startTestItemRQ.isHasStats());
		assertEquals("I am nested step with parameter - '" + PARAM + "'", startTestItemRQ.getName());

		FinishTestItemRQ finishNestedRQ = finishNestedCaptor.getValue();
		assertNotNull(finishNestedRQ);
		assertEquals("FAILED", finishNestedRQ.getStatus());

	}

	@Test
	public void testWithMultiLevelNested() throws NoSuchMethodException {

		TestUtils.runClasses(NestedStepMultiLevelTest.class);

		ArgumentCaptor<StartTestItemRQ> nestedStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<FinishTestItemRQ> finishNestedCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(LAUNCH.get(), times(1)).startTestItem(eq(TEST_METHOD_ID.get()), nestedStepCaptor.capture());
		verify(LAUNCH.get(), times(1)).finishTestItem(eq(NESTED_STEP_ID.get()), finishNestedCaptor.capture());
		verify(LAUNCH.get(), times(1)).startTestItem(eq(NESTED_STEP_ID.get()), nestedStepCaptor.capture());
		verify(LAUNCH.get(), times(1)).finishTestItem(eq(INNER_NESTED_STEP_ID.get()), finishNestedCaptor.capture());

		List<StartTestItemRQ> nestedSteps = nestedStepCaptor.getAllValues();

		nestedSteps.forEach(step -> {
			assertNotNull(step);
			assertFalse(step.isHasStats());
		});

		StartTestItemRQ stepWithInnerStep = nestedSteps.get(0);
		assertEquals(METHOD_WITH_INNER_METHOD_NAME_TEMPLATE, stepWithInnerStep.getName());

		StartTestItemRQ innerStep = nestedSteps.get(1);
		assertEquals("I am - " + NestedStepMultiLevelTest.class.getDeclaredMethod("innerMethod").getName(), innerStep.getName());
	}

}
