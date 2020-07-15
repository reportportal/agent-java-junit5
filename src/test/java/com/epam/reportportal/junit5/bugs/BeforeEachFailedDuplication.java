package com.epam.reportportal.junit5.bugs;

import com.epam.reportportal.junit5.ItemType;
import com.epam.reportportal.junit5.ReportPortalExtension;
import com.epam.reportportal.junit5.features.bug.BeforeEachFailedDuplicate;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.epam.reportportal.junit5.bugs.BeforeEachFailedDuplication.BeforeEachFailedDuplicationExtension.ITEMS;
import static com.epam.reportportal.junit5.bugs.BeforeEachFailedDuplication.BeforeEachFailedDuplicationExtension.LAUNCH;
import static com.epam.reportportal.junit5.util.Verifications.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BeforeEachFailedDuplication {
	public static class BeforeEachFailedDuplicationExtension extends ReportPortalExtension {

		final static Map<StartTestItemRQ, String> ITEMS = new ConcurrentHashMap<>();
		static Launch LAUNCH;

		@Override
		protected Launch getLaunch(ExtensionContext context) {
			return LAUNCH;
		}
	}

	@BeforeAll
	public void setupTestParams() {
		System.setProperty("junit.jupiter.execution.parallel.enabled", "true");
		System.setProperty("junit.jupiter.execution.parallel.mode.default", "concurrent");
		System.setProperty("junit.jupiter.execution.parallel.config.strategy", "fixed");
		System.setProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "5");
	}

	@BeforeEach
	public void setupMock() {
		ITEMS.clear();
		LAUNCH = mock(Launch.class);
		when(LAUNCH.getStepReporter()).thenReturn(StepReporter.NOOP_STEP_REPORTER);
		when(LAUNCH.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> {
			StartTestItemRQ rq = invocation.getArgument(0);
			Maybe<String> result = CommonUtils.createMaybeUuid();
			ITEMS.put(rq, result.blockingGet());
			return result;
		});
		when(LAUNCH.startTestItem(any(), any())).thenAnswer((Answer<Maybe<String>>) invocation -> {
			StartTestItemRQ rq = invocation.getArgument(1);
			Maybe<String> result = CommonUtils.createMaybeUuid();
			ITEMS.put(rq, result.blockingGet());
			return result;
		});
	}

	@Test
	public void verify_test_item_order_in_parallel_run_with_two_tests_before_methods_and_parameters() {
		TestUtils.runClasses(BeforeEachFailedDuplicate.class);
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> calls = verify_call_number_and_capture_arguments(17,
				2,
				LAUNCH
		);
		verifyNoMoreInteractions(LAUNCH);

		String parentSuiteUuid = ITEMS.get(calls.getKey().get(0).getRight());
		verify_suite_calls_positive_finish(parentSuiteUuid, calls);

		// Before / After All

		List<Pair<String, StartTestItemRQ>> beforeAllMethodList = calls.getKey()
				.stream()
				.filter(e -> e.getValue().getType().equals(ItemType.BEFORE_CLASS.name()))
				.collect(Collectors.toList());
		List<Pair<String, StartTestItemRQ>> afterAllMethodList = calls.getKey()
				.stream()
				.filter(e -> e.getValue().getType().equals(ItemType.AFTER_CLASS.name()))
				.collect(Collectors.toList());

		assertThat("There in only one @BeforeAll method", beforeAllMethodList, hasSize(1));
		assertThat("There in only one @AfterAll method", afterAllMethodList, hasSize(1));

		assertThat("@BeforeAll method is on parent suite level", beforeAllMethodList.get(0).getKey(), equalTo(parentSuiteUuid));
		assertThat("@AfterAll method is on parent suite level", afterAllMethodList.get(0).getKey(), equalTo(parentSuiteUuid));

		// Test Suites

		List<Pair<String, StartTestItemRQ>> suiteMethodList = calls.getKey()
				.stream()
				.filter(e -> e.getValue().getType().equals(ItemType.SUITE.name()))
				.collect(Collectors.toList());

		assertThat("There are 3 suites in the test", suiteMethodList, hasSize(3));

		List<String> childSuiteUuids = IntStream.range(1, suiteMethodList.size())
				.boxed()
				.map(i -> ITEMS.get(suiteMethodList.get(i).getValue()))
				.collect(Collectors.toList());

		assertThat("There are 2 children suites in the test", childSuiteUuids, hasSize(2));

		verify_suite_calls_positive_finish(childSuiteUuids.get(0), calls);
		verify_suite_calls_positive_finish(childSuiteUuids.get(1), calls);

		assertThat("Parent Suite has null parent value", suiteMethodList.get(0).getKey(), nullValue());

		assertThat("Other Suites has Parent Suite UUID value", suiteMethodList.get(1).getKey(), equalTo(parentSuiteUuid));
		assertThat("Other Suites has Parent Suite UUID value", suiteMethodList.get(2).getKey(), equalTo(parentSuiteUuid));

		// Before / After Each

		List<Pair<String, StartTestItemRQ>> beforeEachMethodList = calls.getKey()
				.stream()
				.filter(e -> e.getValue().getType().equals(ItemType.BEFORE_METHOD.name()))
				.collect(Collectors.toList());
		List<Pair<String, StartTestItemRQ>> afterEachMethodList = calls.getKey()
				.stream()
				.filter(e -> e.getValue().getType().equals(ItemType.AFTER_METHOD.name()))
				.collect(Collectors.toList());

		assertThat("There are 4 @BeforeEach methods in the test", beforeEachMethodList, hasSize(4));
		assertThat("There are 4 @AfterEach methods in the test", afterEachMethodList, hasSize(4));

		List<Pair<String, StartTestItemRQ>> suiteOneBeforeEachList = beforeEachMethodList.stream()
				.filter(e -> e.getKey().equals(childSuiteUuids.get(0)))
				.collect(Collectors.toList());

		List<Pair<String, StartTestItemRQ>> suiteTwoBeforeEachList = beforeEachMethodList.stream()
				.filter(e -> e.getKey().equals(childSuiteUuids.get(1)))
				.collect(Collectors.toList());

		assertThat("There are 2 @BeforeEach methods in the first child suite", suiteOneBeforeEachList, hasSize(2));
		assertThat("There are 2 @BeforeEach methods in the second child suite", suiteTwoBeforeEachList, hasSize(2));

		List<Pair<String, StartTestItemRQ>> suiteOneAfterEachList = afterEachMethodList.stream()
				.filter(e -> e.getKey().equals(childSuiteUuids.get(0)))
				.collect(Collectors.toList());

		List<Pair<String, StartTestItemRQ>> suiteTwoAfterEachList = afterEachMethodList.stream()
				.filter(e -> e.getKey().equals(childSuiteUuids.get(1)))
				.collect(Collectors.toList());

		assertThat("There are 2 @AfterEach methods in the first child suite", suiteOneAfterEachList, hasSize(2));
		assertThat("There are 2 @AfterEach methods in the second child suite", suiteTwoAfterEachList, hasSize(2));

		IntStream.range(0, calls.getKey().size())
				.boxed()
				.filter(i -> suiteOneBeforeEachList.contains(calls.getKey().get(i)))
				.forEach(i -> verify_before_each_positive_calls(i,
						childSuiteUuids.get(0),
						ITEMS.get(calls.getKey().get(i).getValue()),
						calls
				));

		IntStream.range(0, calls.getKey().size())
				.boxed()
				.filter(i -> suiteTwoBeforeEachList.contains(calls.getKey().get(i)))
				.forEach(i -> verify_before_each_positive_calls(i,
						childSuiteUuids.get(1),
						ITEMS.get(calls.getKey().get(i).getValue()),
						calls
				));

		IntStream.range(0, calls.getKey().size())
				.boxed()
				.filter(i -> suiteOneAfterEachList.contains(calls.getKey().get(i)))
				.forEach(i -> verify_after_each_positive_calls(i,
						childSuiteUuids.get(0),
						ITEMS.get(calls.getKey().get(i).getValue()),
						calls
				));

		IntStream.range(0, calls.getKey().size())
				.boxed()
				.filter(i -> suiteTwoAfterEachList.contains(calls.getKey().get(i)))
				.forEach(i -> verify_after_each_positive_calls(i,
						childSuiteUuids.get(1),
						ITEMS.get(calls.getKey().get(i).getValue()),
						calls
				));

		// Tests

		List<Pair<String, StartTestItemRQ>> testMethodList = calls.getKey()
				.stream()
				.filter(e -> e.getValue().getType().equals(ItemType.STEP.name()))
				.collect(Collectors.toList());

		assertThat("There are 4 @Test methods in the test", testMethodList, hasSize(4));

		List<Pair<String, StartTestItemRQ>> suiteOneTestList = testMethodList.stream()
				.filter(e -> e.getKey().equals(childSuiteUuids.get(0)))
				.collect(Collectors.toList());

		List<Pair<String, StartTestItemRQ>> suiteTwoTestList = testMethodList.stream()
				.filter(e -> e.getKey().equals(childSuiteUuids.get(1)))
				.collect(Collectors.toList());

		assertThat("There are 2 @Test methods in the first child suite", suiteOneTestList, hasSize(2));
		assertThat("There are 2 @Test methods in the second child suite", suiteTwoTestList, hasSize(2));

		IntStream.range(0, calls.getKey().size())
				.boxed()
				.filter(i -> suiteOneTestList.contains(calls.getKey().get(i)))
				.forEach(i -> verify_test_positive_calls(i, childSuiteUuids.get(0), ITEMS.get(calls.getKey().get(i).getValue()), calls));

		IntStream.range(0, calls.getKey().size())
				.boxed()
				.filter(i -> suiteTwoTestList.contains(calls.getKey().get(i)))
				.forEach(i -> verify_test_positive_calls(i, childSuiteUuids.get(1), ITEMS.get(calls.getKey().get(i).getValue()), calls));
	}

	@AfterAll
	public void cleanUpTestParams() {
		System.clearProperty("junit.jupiter.execution.parallel.enabled");
		System.clearProperty("junit.jupiter.execution.parallel.mode.default");
		System.clearProperty("junit.jupiter.execution.parallel.config.strategy");
		System.clearProperty("junit.jupiter.execution.parallel.config.fixed.parallelism");
	}
}
