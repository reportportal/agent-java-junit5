package com.epam.reportportal.junit5;

import com.epam.reportportal.junit5.features.beforeafterall.*;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.epam.reportportal.junit5.BeforeAfterAllTest.BeforeAfterAllTestExtension.LAUNCHES;
import static com.epam.reportportal.junit5.BeforeAfterAllTest.BeforeAfterAllTestExtension.getItemId;
import static com.epam.reportportal.junit5.util.Verifications.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests on {@link BeforeAll} and {@link AfterAll} test methods, which should be executed before and after all tests.
 */
public class BeforeAfterAllTest {
	public static class BeforeAfterAllTestExtension extends ReportPortalExtension {

		final static ThreadLocal<List<String>> ITEMS = ThreadLocal.withInitial(ArrayList::new);
		final static ThreadLocal<String> LAUNCH_ID = new ThreadLocal<>();
		final static ThreadLocal<Launch> LAUNCHES = new ThreadLocal<>();

		private final ReportPortal reportPortal;

		public BeforeAfterAllTestExtension() {
			reportPortal = mock(ReportPortal.class);
			Launch launch = mock(Launch.class);
			LAUNCHES.set(launch);
			ITEMS.get().clear();
			Maybe<String> launchIdMaybe = TestUtils.createItemUuidMaybe();
			LAUNCH_ID.set(launchIdMaybe.blockingGet());
			when(reportPortal.getParameters()).thenReturn(new ListenerParameters());
			when(reportPortal.newLaunch(any())).thenReturn(launch);
			when(launch.start()).thenAnswer((Answer<Maybe<String>>) invocation -> launchIdMaybe);
			when(launch.startTestItem(any(), any())).thenAnswer((Answer<Maybe<String>>) invocation -> {
				Maybe<String> result = TestUtils.createItemUuidMaybe();
				ITEMS.get().add(result.blockingGet());
				return result;
			});
		}

		@Override
		ReportPortal getReporter() {
			return reportPortal;
		}

		@Override
		String getLaunchId(ExtensionContext context) {
			return LAUNCH_ID.get();
		}

		public static String getItemId(int index) {
			return ITEMS.get().get(index);
		}
	}

	@Test
	public void test_before_only() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 1; // Start a before all
		int testMethodNumber = 1; // Start a test
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		TestUtils.runClasses(BeforeAllTest.class);
		Launch launch = LAUNCHES.get();
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = verify_call_number_and_capture_arguments(allItemNumber,
				launch
		);
		verifyNoMoreInteractions(launch);

		String suiteUuid = getItemId(0);
		verify_suite_calls_positive_finish(suiteUuid, launchCalls);

		verify_before_class_positive_calls(1, suiteUuid, getItemId(1), launchCalls);
		verify_test_positive_calls(2, suiteUuid, getItemId(2), launchCalls);
	}

	@Test
	public void test_after_only() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 1; // Start an after all
		int testMethodNumber = 1; // Start a test
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		TestUtils.runClasses(AfterAllTest.class);
		Launch launch = LAUNCHES.get();
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = verify_call_number_and_capture_arguments(allItemNumber,
				launch
		);
		verifyNoMoreInteractions(launch);

		String suiteUuid = getItemId(0);
		verify_suite_calls_positive_finish(suiteUuid, launchCalls);

		verify_test_positive_calls(1, suiteUuid, getItemId(1), launchCalls);
		verify_after_class_positive_calls(2, suiteUuid, getItemId(2), launchCalls);
	}

	@Test
	public void test_before_after() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 2; // Start a before all, after all
		int testMethodNumber = 1; // Start a test
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		TestUtils.runClasses(com.epam.reportportal.junit5.features.beforeafterall.BeforeAfterAllTest.class);
		Launch launch = LAUNCHES.get();
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = verify_call_number_and_capture_arguments(allItemNumber,
				launch
		);
		verifyNoMoreInteractions(launch);

		String suiteUuid = getItemId(0);
		verify_suite_calls_positive_finish(suiteUuid, launchCalls);

		verify_before_class_positive_calls(1, suiteUuid, getItemId(1), launchCalls);
		verify_test_positive_calls(2, suiteUuid, getItemId(2), launchCalls);
		verify_after_class_positive_calls(3, suiteUuid, getItemId(3), launchCalls);
	}

	@Test
	public void test_two_tests_in_one_class_with_before_after_all() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 2; // Start a before all, after all
		int testMethodNumber = 2; // Start two tests
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		TestUtils.runClasses(BeforeAfterAllTwoTestsTest.class);
		Launch launch = LAUNCHES.get();
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = verify_call_number_and_capture_arguments(allItemNumber,
				launch
		);
		verifyNoMoreInteractions(launch);

		String suiteUuid = getItemId(0);
		verify_suite_calls_positive_finish(suiteUuid, launchCalls);

		verify_before_class_positive_calls(1, suiteUuid, getItemId(1), launchCalls);
		verify_test_positive_calls(2, suiteUuid, getItemId(2), launchCalls);
		verify_test_positive_calls(3, suiteUuid, getItemId(3), launchCalls);
		verify_after_class_positive_calls(4, suiteUuid, getItemId(4), launchCalls);
	}

	@Test
	public void test_failed_before() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 1; // Start a before all
		int testMethodNumber = 0; // Do not start a test
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		TestUtils.runClasses(BeforeAllFailedTest.class);
		Launch launch = LAUNCHES.get();
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = verify_call_number_and_capture_arguments(allItemNumber,
				launch
		);
		verifyNoMoreInteractions(launch);

		String suiteUuid = getItemId(0);
		verify_suite_calls_negative_finish(suiteUuid, launchCalls);

		verify_before_class_negative_calls(1, suiteUuid, getItemId(1), launchCalls);
	}

	@Test
	public void test_failed_after() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 2; // Start a before all and after all
		int testMethodNumber = 1; // Start a test
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		TestUtils.runClasses(BeforeAllFailedAfterAllTest.class);
		Launch launch = LAUNCHES.get();
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = verify_call_number_and_capture_arguments(allItemNumber,
				launch
		);
		verifyNoMoreInteractions(launch);

		String suiteUuid = getItemId(0);
		verify_suite_calls_negative_finish(suiteUuid, launchCalls);

		verify_before_class_positive_calls(1, suiteUuid, getItemId(1), launchCalls);
		verify_test_positive_calls(2, suiteUuid, getItemId(2), launchCalls);
		verify_after_class_negative_calls(3, suiteUuid, getItemId(3), launchCalls);
	}

	@Test
	public void test_failed_test() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 2; // Start a before all and after all
		int testMethodNumber = 1; // Start a test
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		TestUtils.runClasses(BeforeAfterAllFailedTest.class);
		Launch launch = LAUNCHES.get();
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = verify_call_number_and_capture_arguments(allItemNumber,
				launch
		);
		verifyNoMoreInteractions(launch);

		String suiteUuid = getItemId(0);
		verify_suite_calls_negative_finish(suiteUuid, launchCalls);

		verify_before_class_positive_calls(1, suiteUuid, getItemId(1), launchCalls);
		verify_test_negative_calls(2, suiteUuid, getItemId(2), launchCalls);
		verify_after_class_positive_calls(3, suiteUuid, getItemId(3), launchCalls);
	}

	@Test
	public void test_two_tests_in_one_class_with_before_after_all_and_each() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 6; // Start a before all, after all, two before each, two after each
		int testMethodNumber = 2; // Start two tests
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		TestUtils.runClasses(BeforeAfterAllBeforeAfterEachTwoTestsTest.class);
		Launch launch = LAUNCHES.get();
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = verify_call_number_and_capture_arguments(allItemNumber,
				launch
		);
		verifyNoMoreInteractions(launch);

		String suiteUuid = getItemId(0);
		verify_suite_calls_positive_finish(suiteUuid, launchCalls);

		verify_before_class_positive_calls(1, suiteUuid, getItemId(1), launchCalls);
		verify_before_each_positive_calls(2, suiteUuid, getItemId(2), launchCalls);
		verify_test_positive_calls(3, suiteUuid, getItemId(3), launchCalls);
		verify_after_each_positive_calls(4, suiteUuid, getItemId(4), launchCalls);
		verify_before_each_positive_calls(5, suiteUuid, getItemId(5), launchCalls);
		verify_test_positive_calls(6, suiteUuid, getItemId(6), launchCalls);
		verify_after_each_positive_calls(7, suiteUuid, getItemId(7), launchCalls);
		verify_after_class_positive_calls(8, suiteUuid, getItemId(8), launchCalls);
	}

	@Test
	public void test_before_after_non_static() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 2; // Start a before all, after all
		int testMethodNumber = 1; // Start a test
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		TestUtils.runClasses(NonStaticBeforeAfterAllTest.class);
		Launch launch = LAUNCHES.get();
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = verify_call_number_and_capture_arguments(allItemNumber,
				launch
		);
		verifyNoMoreInteractions(launch);

		String suiteUuid = getItemId(0);
		verify_suite_calls_positive_finish(suiteUuid, launchCalls);

		verify_before_class_positive_calls(1, suiteUuid, getItemId(1), launchCalls);
		verify_test_positive_calls(2, suiteUuid, getItemId(2), launchCalls);
		verify_after_class_positive_calls(3, suiteUuid, getItemId(3), launchCalls);
	}
}
