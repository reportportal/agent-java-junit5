package com.epam.reportportal.junit5;

import com.epam.reportportal.junit5.features.beforeafterall.*;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.step.StepReporter;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.epam.reportportal.junit5.BeforeAfterAllTest.BeforeAfterAllTestExtension.ITEMS;
import static com.epam.reportportal.junit5.BeforeAfterAllTest.BeforeAfterAllTestExtension.LAUNCH;
import static com.epam.reportportal.junit5.util.Verifications.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests on {@link BeforeAll} and {@link AfterAll} test methods, which should be executed before and after all tests.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BeforeAfterAllTest {
	public static class BeforeAfterAllTestExtension extends ReportPortalExtension {

		final static List<String> ITEMS = new ArrayList<>();
		static Launch LAUNCH;

		@Override
		protected Launch getLaunch(ExtensionContext context) {
			return LAUNCH;
		}
	}

	@BeforeEach
	public void setupMocks() {
		ITEMS.clear();
		LAUNCH = mock(Launch.class);
		when(LAUNCH.getStepReporter()).thenReturn(StepReporter.NOOP_STEP_REPORTER);
		when(LAUNCH.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> {
			Maybe<String> result = CommonUtils.createMaybeUuid();
			ITEMS.add(result.blockingGet());
			return result;
		});
		when(LAUNCH.startTestItem(any(), any())).thenAnswer((Answer<Maybe<String>>) invocation -> {
			Maybe<String> result = CommonUtils.createMaybeUuid();
			ITEMS.add(result.blockingGet());
			return result;
		});
	}

	@Test
	public void test_before_only() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 1; // Start a before all
		int testMethodNumber = 1; // Start a test
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		TestUtils.runClasses(BeforeAllTest.class);
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = verify_call_number_and_capture_arguments(allItemNumber,
				LAUNCH
		);
		verifyNoMoreInteractions(LAUNCH);

		String suiteUuid = ITEMS.get(0);
		verify_suite_calls_positive_finish(suiteUuid, launchCalls);

		verify_before_class_positive_calls(1, suiteUuid, ITEMS.get(1), launchCalls);
		verify_test_positive_calls(2, suiteUuid, ITEMS.get(2), launchCalls);
	}

	@Test
	public void test_after_only() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 1; // Start an after all
		int testMethodNumber = 1; // Start a test
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		TestUtils.runClasses(AfterAllTest.class);
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = verify_call_number_and_capture_arguments(allItemNumber,
				LAUNCH
		);
		verifyNoMoreInteractions(LAUNCH);

		String suiteUuid = ITEMS.get(0);
		verify_suite_calls_positive_finish(suiteUuid, launchCalls);

		verify_test_positive_calls(1, suiteUuid, ITEMS.get(1), launchCalls);
		verify_after_class_positive_calls(2, suiteUuid, ITEMS.get(2), launchCalls);
	}

	@Test
	public void test_before_after() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 2; // Start a before all, after all
		int testMethodNumber = 1; // Start a test
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		TestUtils.runClasses(com.epam.reportportal.junit5.features.beforeafterall.BeforeAfterAllTest.class);
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = verify_call_number_and_capture_arguments(allItemNumber,
				LAUNCH
		);
		verifyNoMoreInteractions(LAUNCH);

		String suiteUuid = ITEMS.get(0);
		verify_suite_calls_positive_finish(suiteUuid, launchCalls);

		verify_before_class_positive_calls(1, suiteUuid, ITEMS.get(1), launchCalls);
		verify_test_positive_calls(2, suiteUuid, ITEMS.get(2), launchCalls);
		verify_after_class_positive_calls(3, suiteUuid, ITEMS.get(3), launchCalls);
	}

	@Test
	public void test_two_tests_in_one_class_with_before_after_all() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 2; // Start a before all, after all
		int testMethodNumber = 2; // Start two tests
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		TestUtils.runClasses(BeforeAfterAllTwoTestsTest.class);
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = verify_call_number_and_capture_arguments(allItemNumber,
				LAUNCH
		);
		verifyNoMoreInteractions(LAUNCH);

		String suiteUuid = ITEMS.get(0);
		verify_suite_calls_positive_finish(suiteUuid, launchCalls);

		verify_before_class_positive_calls(1, suiteUuid, ITEMS.get(1), launchCalls);
		verify_test_positive_calls(2, suiteUuid, ITEMS.get(2), launchCalls);
		verify_test_positive_calls(3, suiteUuid, ITEMS.get(3), launchCalls);
		verify_after_class_positive_calls(4, suiteUuid, ITEMS.get(4), launchCalls);
	}

	@Test
	public void test_failed_before() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 1; // Start a before all
		int testMethodNumber = 0; // Do not start a test
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		TestUtils.runClasses(BeforeAllFailedTest.class);
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = verify_call_number_and_capture_arguments(allItemNumber,
				LAUNCH
		);
		verifyNoMoreInteractions(LAUNCH);

		String suiteUuid = ITEMS.get(0);
		verify_suite_calls_negative_finish(suiteUuid, launchCalls);

		verify_before_class_negative_calls(1, suiteUuid, ITEMS.get(1), launchCalls);
	}

	@Test
	public void test_after_all_failed() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 2; // Start a before all and after all
		int testMethodNumber = 1; // Start a test
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		TestUtils.runClasses(BeforeAllAfterAllFailedTest.class);
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = verify_call_number_and_capture_arguments(allItemNumber,
				LAUNCH
		);
		verifyNoMoreInteractions(LAUNCH);

		String suiteUuid = ITEMS.get(0);
		verify_suite_calls_negative_finish(suiteUuid, launchCalls);

		verify_before_class_positive_calls(1, suiteUuid, ITEMS.get(1), launchCalls);
		verify_test_positive_calls(2, suiteUuid, ITEMS.get(2), launchCalls);
		verify_after_class_negative_calls(3, suiteUuid, ITEMS.get(3), launchCalls);
	}

	@Test
	public void test_failed_test() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 2; // Start a before all and after all
		int testMethodNumber = 1; // Start a test
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		TestUtils.runClasses(BeforeAfterAllFailedTest.class);
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = verify_call_number_and_capture_arguments(allItemNumber,
				LAUNCH
		);
		verifyNoMoreInteractions(LAUNCH);

		String suiteUuid = ITEMS.get(0);
		verify_suite_calls_negative_finish(suiteUuid, launchCalls);

		verify_before_class_positive_calls(1, suiteUuid, ITEMS.get(1), launchCalls);
		verify_test_negative_calls(2, suiteUuid, ITEMS.get(2), launchCalls);
		verify_after_class_positive_calls(3, suiteUuid, ITEMS.get(3), launchCalls);
	}

	@Test
	public void test_two_tests_in_one_class_with_before_after_all_and_each() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 6; // Start a before all, after all, two before each, two after each
		int testMethodNumber = 2; // Start two tests
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		TestUtils.runClasses(BeforeAfterAllBeforeAfterEachTwoTestsTest.class);
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = verify_call_number_and_capture_arguments(allItemNumber,
				LAUNCH
		);
		verifyNoMoreInteractions(LAUNCH);

		String suiteUuid = ITEMS.get(0);
		verify_suite_calls_positive_finish(suiteUuid, launchCalls);

		verify_before_class_positive_calls(1, suiteUuid, ITEMS.get(1), launchCalls);
		verify_before_each_positive_calls(2, suiteUuid, ITEMS.get(2), launchCalls);
		verify_test_positive_calls(3, suiteUuid, ITEMS.get(3), launchCalls);
		verify_after_each_positive_calls(4, suiteUuid, ITEMS.get(4), launchCalls);
		verify_before_each_positive_calls(5, suiteUuid, ITEMS.get(5), launchCalls);
		verify_test_positive_calls(6, suiteUuid, ITEMS.get(6), launchCalls);
		verify_after_each_positive_calls(7, suiteUuid, ITEMS.get(7), launchCalls);
		verify_after_class_positive_calls(8, suiteUuid, ITEMS.get(8), launchCalls);
	}

	@Test
	public void test_before_after_non_static() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 2; // Start a before all, after all
		int testMethodNumber = 1; // Start a test
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		TestUtils.runClasses(NonStaticBeforeAfterAllTest.class);
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = verify_call_number_and_capture_arguments(allItemNumber,
				LAUNCH
		);
		verifyNoMoreInteractions(LAUNCH);

		String suiteUuid = ITEMS.get(0);
		verify_suite_calls_positive_finish(suiteUuid, launchCalls);

		verify_before_class_positive_calls(1, suiteUuid, ITEMS.get(1), launchCalls);
		verify_test_positive_calls(2, suiteUuid, ITEMS.get(2), launchCalls);
		verify_after_class_positive_calls(3, suiteUuid, ITEMS.get(3), launchCalls);
	}

	@Test
	public void test_after_each_failed() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 1; // An after each
		int testMethodNumber = 1; // Start a test
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		TestUtils.runClasses(FailedAfterEachTest.class);
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = verify_call_number_and_capture_arguments(allItemNumber,
				LAUNCH
		);
		verifyNoMoreInteractions(LAUNCH);

		String suiteUuid = ITEMS.get(0);
		verify_suite_calls_negative_finish(suiteUuid, launchCalls);

		verify_test_positive_calls(1, suiteUuid, ITEMS.get(1), launchCalls);
		verify_after_each_negative_calls(2, suiteUuid, ITEMS.get(2), launchCalls);
	}
}
