package com.epam.reportportal.junit5;

import com.epam.reportportal.junit5.features.nested.NestedStepFeatureFailedTest;
import com.epam.reportportal.junit5.features.nested.NestedStepFeaturePassedTest;
import com.epam.reportportal.junit5.features.nested.NestedStepMultiLevelTest;
import com.epam.reportportal.junit5.features.nested.NestedStepWithBeforeEachTest;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.ta.reportportal.ws.model.BatchSaveOperatingRS;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.reportportal.junit5.NestedStepTest.TestExtension.*;
import static com.epam.reportportal.util.test.CommonUtils.namedId;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class NestedStepTest {

	public static final String PARAM = "test param";
	public static final String NESTED_STEP_NAME_TEMPLATE = "I am nested step with parameter - '{param}'";
	public static final String METHOD_WITH_INNER_METHOD_NAME_TEMPLATE = "I am method with inner method";
	public static final String INNER_METHOD_NAME_TEMPLATE = "I am - {method}";

	public static class TestExtension extends ReportPortalExtension {
		static final ThreadLocal<ReportPortalClient> CLIENT = new ThreadLocal<>();

		static final String TEST_CLASS_ID = namedId("class");
		static final String TEST_METHOD_ID = namedId("test");
		static final List<String> STEP_ID_LIST = Stream.generate(() -> namedId("step")).limit(2).collect(Collectors.toList());
		static final List<Pair<String, String>> TEST_STEP_ID_ORDER = Arrays.asList(Pair.of(TEST_METHOD_ID, STEP_ID_LIST.get(0)),
				Pair.of(STEP_ID_LIST.get(0), STEP_ID_LIST.get(1))
		);

		private final ReportPortal rp;
		private final String id;

		@SuppressWarnings("unchecked")
		public TestExtension() {
			ReportPortalClient myClient = mock(ReportPortalClient.class);
			id = String.valueOf(myClient.hashCode());
			CLIENT.set(myClient);
			TestUtils.mockLaunch(myClient, "launchUuid", TEST_CLASS_ID, TEST_METHOD_ID);
			TestUtils.mockNestedSteps(myClient, TEST_STEP_ID_ORDER);
			lenient().when(myClient.log(any(List.class))).thenReturn(Maybe.just(new BatchSaveOperatingRS()));
			rp = ReportPortal.create(myClient, TestUtils.standardParameters());
		}

		@Override
		protected ReportPortal getReporter() {
			return rp;
		}

		@Override
		protected String getLaunchId(ExtensionContext context) {
			return id;
		}
	}

	@Test
	public void nestedTest() {

		TestUtils.runClasses(NestedStepFeaturePassedTest.class);

		ArgumentCaptor<StartTestItemRQ> nestedStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<FinishTestItemRQ> finishNestedCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(CLIENT.get(), timeout(1000).times(1)).startTestItem(same(TEST_METHOD_ID), nestedStepCaptor.capture());
		verify(CLIENT.get(), timeout(1000).times(1)).finishTestItem(same(STEP_ID_LIST.get(0)), finishNestedCaptor.capture());

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
		verify(CLIENT.get(), timeout(1000).times(1)).startTestItem(same(TEST_METHOD_ID), nestedStepCaptor.capture());
		verify(CLIENT.get(), timeout(1000).times(1)).finishTestItem(same(STEP_ID_LIST.get(0)), finishNestedCaptor.capture());

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
		verify(CLIENT.get(), timeout(1000).times(1)).startTestItem(same(TEST_METHOD_ID), nestedStepCaptor.capture());
		verify(CLIENT.get(), timeout(1000).times(1)).finishTestItem(same(STEP_ID_LIST.get(0)), finishNestedCaptor.capture());

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
		verify(CLIENT.get(), timeout(1000).times(1)).startTestItem(same(TEST_METHOD_ID), nestedStepCaptor.capture());
		verify(CLIENT.get(), timeout(1000).times(1)).finishTestItem(same(STEP_ID_LIST.get(0)), finishNestedCaptor.capture());
		verify(CLIENT.get(), timeout(1000).times(1)).startTestItem(same(STEP_ID_LIST.get(0)), nestedStepCaptor.capture());
		verify(CLIENT.get(), timeout(1000).times(1)).finishTestItem(same(STEP_ID_LIST.get(1)), finishNestedCaptor.capture());

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
