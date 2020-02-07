package com.epam.reportportal.junit5;

import com.epam.reportportal.junit5.features.beforeafterall.*;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests on {@link BeforeAll} and {@link AfterAll} test methods, which should be executed before and after all tests.
 */
public class BeforeAfterAllTest {
	public static class BeforeAfterAllTestExtension extends ReportPortalExtension {

		final static ThreadLocal<Launch> LAUNCHES = new ThreadLocal<>();
		final static Map<Object, String> ITEM_ID_MAP = new ConcurrentHashMap<>();

		private final Launch launch;

		public BeforeAfterAllTestExtension() {
			launch = mock(Launch.class);
			LAUNCHES.set(launch);
			when(launch.startTestItem(any(), any())).thenAnswer((Answer<Maybe<String>>) invocation -> {
				Maybe<String> result = TestUtils.createItemUuidMaybe();
				ITEM_ID_MAP.put(invocation.getArgument(1), result.blockingGet());
				return result;
			});
		}

		@Override
		Launch getLaunch(ExtensionContext context) {
			return launch;
		}
	}

	public Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> commonTestRun(final Class<?> clazz, int itemNum) {
		TestUtils.runClasses(clazz);
		Launch launch = BeforeAfterAllTestExtension.LAUNCHES.get();

		ArgumentCaptor<Maybe<String>> startParentItemId = ArgumentCaptor.forClass(Maybe.class);
		ArgumentCaptor<StartTestItemRQ> startItemCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<Maybe<String>> finishItemId = ArgumentCaptor.forClass(Maybe.class);
		ArgumentCaptor<FinishTestItemRQ> finishItemCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch, times(itemNum)).startTestItem(startParentItemId.capture(), startItemCaptor.capture());
		verify(launch, times(itemNum)).finishTestItem(finishItemId.capture(), finishItemCaptor.capture());
		verifyNoMoreInteractions(launch);

		List<String> startItemIds = startParentItemId.getAllValues()
				.stream()
				.map(m -> m == null ? null : m.blockingGet())
				.collect(Collectors.toList());
		List<StartTestItemRQ> startItemValues = startItemCaptor.getAllValues();
		List<Pair<String, StartTestItemRQ>> startResult = IntStream.range(0, startItemIds.size())
				.mapToObj(i -> ImmutablePair.of(startItemIds.get(i), startItemValues.get(i)))
				.collect(Collectors.toList());
		List<String> finishItemIds = finishItemId.getAllValues()
				.stream()
				.map(m -> m == null ? null : m.blockingGet())
				.collect(Collectors.toList());
		List<FinishTestItemRQ> finishItemValues = finishItemCaptor.getAllValues();
		Map<String, FinishTestItemRQ> finishResult = IntStream.range(0, finishItemIds.size())
				.boxed()
				.collect(Collectors.toMap(finishItemIds::get, finishItemValues::get));

		return ImmutablePair.of(startResult, finishResult);
	}

	private void verify_correct_start_item_responses(String itemType, int callIndex, String suiteUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		Pair<String, StartTestItemRQ> beforeClassStartCall = launchCalls.getKey().get(callIndex);
		assertThat(String.format("Check %s has correct parent ID", itemType), beforeClassStartCall.getKey(), equalTo(suiteUuid));
		assertThat(String.format("Check %s has correct type", itemType), beforeClassStartCall.getValue().getType(), equalTo(itemType));
	}

	private void verify_items_finish_statuses(String type, int callIndex, String status,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		String testUuid = BeforeAfterAllTestExtension.ITEM_ID_MAP.get(launchCalls.getKey().get(callIndex).getValue());
		assertThat("Check there is a finish test item call", launchCalls.getValue(), hasKey(testUuid));
		FinishTestItemRQ rq = launchCalls.getValue().get(testUuid);

		assertThat(String.format("Check %s has no issue", type), rq.getIssue(), nullValue());
		assertThat(String.format("Check %s has no retry", type), rq.isRetry(), anyOf(nullValue(), equalTo(Boolean.FALSE)));
		assertThat(String.format("Check %s has %s status", type, status), rq.getStatus(), equalTo(status));
	}

	private void verify_items_positive_finish_statuses(String type, int callIndex,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		verify_items_finish_statuses(type, callIndex, Statuses.PASSED, launchCalls);
	}

	private void verify_items_negative_finish_statuses(String type, int callIndex,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		verify_items_finish_statuses(type, callIndex, Statuses.FAILED, launchCalls);
	}

	private void verify_correct_suite_start_responses(
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		int callIndex = 0; // usually a suite goes first
		String itemType = "SUITE";
		Pair<String, StartTestItemRQ> suiteStartCall = launchCalls.getKey().get(callIndex);
		assertThat("Check suite has 'null' parent ID", suiteStartCall.getKey(), nullValue());
		assertThat("Check suite has correct type", suiteStartCall.getValue().getType(), equalTo(itemType));
	}

	private void verify_suite_calls_positive_finish(Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		int callIndex = 0; // usually a suite goes first
		verify_correct_suite_start_responses(launchCalls);
		verify_items_positive_finish_statuses("SUITE", callIndex, launchCalls);
	}

	private void verify_suite_calls_negative_finish(Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		int callIndex = 0; // usually a suite goes first
		verify_correct_suite_start_responses(launchCalls);

		verify_items_negative_finish_statuses("SUITE", callIndex, launchCalls);
	}

	private void verify_before_class_positive_calls(int callIndex, String suiteUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		String itemType = "BEFORE_CLASS";
		verify_correct_start_item_responses(itemType, callIndex, suiteUuid, launchCalls);
		verify_items_positive_finish_statuses(itemType, callIndex, launchCalls);
	}

	private void verify_before_class_negative_calls(int callIndex, String suiteUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		String itemType = "BEFORE_CLASS";
		verify_correct_start_item_responses(itemType, callIndex, suiteUuid, launchCalls);
		verify_items_negative_finish_statuses(itemType, callIndex, launchCalls);
	}

	private void verify_before_each_positive_calls(int callIndex, String suiteUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		String itemType = "BEFORE_METHOD";
		verify_correct_start_item_responses(itemType, callIndex, suiteUuid, launchCalls);
		verify_items_positive_finish_statuses(itemType, callIndex, launchCalls);
	}

	private void verify_after_class_positive_calls(int callIndex, String suiteUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		String itemType = "AFTER_CLASS";
		verify_correct_start_item_responses(itemType, callIndex, suiteUuid, launchCalls);
		verify_items_positive_finish_statuses(itemType, callIndex, launchCalls);
	}

	private void verify_after_class_negative_calls(int callIndex, String suiteUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		String itemType = "AFTER_CLASS";
		verify_correct_start_item_responses(itemType, callIndex, suiteUuid, launchCalls);
		verify_items_negative_finish_statuses(itemType, callIndex, launchCalls);
	}

	private void verify_after_each_positive_calls(int callIndex, String suiteUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		String itemType = "AFTER_METHOD";
		verify_correct_start_item_responses(itemType, callIndex, suiteUuid, launchCalls);
		verify_items_positive_finish_statuses(itemType, callIndex, launchCalls);
	}

	private void verify_test_positive_calls(int callIndex, String suiteUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		String itemType = "STEP";
		verify_correct_start_item_responses(itemType, callIndex, suiteUuid, launchCalls);
		verify_items_positive_finish_statuses(itemType, callIndex, launchCalls);
	}

	private void verify_test_negative_calls(int callIndex, String suiteUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		String itemType = "STEP";
		verify_correct_start_item_responses(itemType, callIndex, suiteUuid, launchCalls);
		verify_items_negative_finish_statuses(itemType, callIndex, launchCalls);
	}

	@Test
	public void test_before_only() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 1; // Start a before all
		int testMethodNumber = 1; // Start a test
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = commonTestRun(BeforeAllTest.class,
				allItemNumber
		);
		verifyNoMoreInteractions(BeforeAfterAllTestExtension.LAUNCHES.get());

		verify_suite_calls_positive_finish(launchCalls);

		String suiteUuid = BeforeAfterAllTestExtension.ITEM_ID_MAP.get(launchCalls.getKey().get(0).getValue());
		verify_before_class_positive_calls(1, suiteUuid, launchCalls);
		verify_test_positive_calls(2, suiteUuid, launchCalls);
	}

	@Test
	public void test_after_only() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 1; // Start an after all
		int testMethodNumber = 1; // Start a test
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = commonTestRun(AfterAllTest.class,
				allItemNumber
		);
		verifyNoMoreInteractions(BeforeAfterAllTestExtension.LAUNCHES.get());

		verify_suite_calls_positive_finish(launchCalls);

		String suiteUuid = BeforeAfterAllTestExtension.ITEM_ID_MAP.get(launchCalls.getKey().get(0).getValue());
		verify_test_positive_calls(1, suiteUuid, launchCalls);
		verify_after_class_positive_calls(2, suiteUuid, launchCalls);
	}

	@Test
	public void test_before_after() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 2; // Start a before all, after all
		int testMethodNumber = 1; // Start a test
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = commonTestRun(com.epam.reportportal.junit5.features.beforeafterall.BeforeAfterAllTest.class,
				allItemNumber
		);
		verifyNoMoreInteractions(BeforeAfterAllTestExtension.LAUNCHES.get());

		verify_suite_calls_positive_finish(launchCalls);

		String suiteUuid = BeforeAfterAllTestExtension.ITEM_ID_MAP.get(launchCalls.getKey().get(0).getValue());
		verify_before_class_positive_calls(1, suiteUuid, launchCalls);
		verify_test_positive_calls(2, suiteUuid, launchCalls);
		verify_after_class_positive_calls(3, suiteUuid, launchCalls);
	}

	@Test
	public void test_two_tests_in_one_class_with_before_after_all() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 2; // Start a before all, after all
		int testMethodNumber = 2; // Start two tests
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = commonTestRun(BeforeAfterAllTwoTestsTest.class,
				allItemNumber
		);
		verifyNoMoreInteractions(BeforeAfterAllTestExtension.LAUNCHES.get());

		verify_suite_calls_positive_finish(launchCalls);

		String suiteUuid = BeforeAfterAllTestExtension.ITEM_ID_MAP.get(launchCalls.getKey().get(0).getValue());
		verify_before_class_positive_calls(1, suiteUuid, launchCalls);
		verify_test_positive_calls(2, suiteUuid, launchCalls);
		verify_test_positive_calls(3, suiteUuid, launchCalls);
		verify_after_class_positive_calls(4, suiteUuid, launchCalls);
	}

	@Test
	public void test_failed_before() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 1; // Start a before all
		int testMethodNumber = 0; // Do not start a test
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = commonTestRun(BeforeAllFailedTest.class,
				allItemNumber
		);
		verifyNoMoreInteractions(BeforeAfterAllTestExtension.LAUNCHES.get());

		verify_suite_calls_negative_finish(launchCalls);

		String suiteUuid = BeforeAfterAllTestExtension.ITEM_ID_MAP.get(launchCalls.getKey().get(0).getValue());
		verify_before_class_negative_calls(1, suiteUuid, launchCalls);
	}

	@Test
	public void test_failed_after() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 2; // Start a before all and after all
		int testMethodNumber = 1; // Start a test
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = commonTestRun(BeforeAllFailedAfterAllTest.class,
				allItemNumber
		);
		verifyNoMoreInteractions(BeforeAfterAllTestExtension.LAUNCHES.get());

		verify_suite_calls_negative_finish(launchCalls);

		String suiteUuid = BeforeAfterAllTestExtension.ITEM_ID_MAP.get(launchCalls.getKey().get(0).getValue());
		verify_before_class_positive_calls(1, suiteUuid, launchCalls);
		verify_test_positive_calls(2, suiteUuid, launchCalls);
		verify_after_class_negative_calls(3, suiteUuid, launchCalls);
	}

	@Test
	public void test_failed_test() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 2; // Start a before all and after all
		int testMethodNumber = 1; // Start a test
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = commonTestRun(BeforeAfterAllFailedTest.class,
				allItemNumber
		);
		verifyNoMoreInteractions(BeforeAfterAllTestExtension.LAUNCHES.get());

		verify_suite_calls_negative_finish(launchCalls);

		String suiteUuid = BeforeAfterAllTestExtension.ITEM_ID_MAP.get(launchCalls.getKey().get(0).getValue());
		verify_before_class_positive_calls(1, suiteUuid, launchCalls);
		verify_test_negative_calls(2, suiteUuid, launchCalls);
		verify_after_class_positive_calls(3, suiteUuid, launchCalls);
	}

	@Test
	public void test_two_tests_in_one_class_with_before_after_all_and_each() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 6; // Start a before all, after all, two before each, two after each
		int testMethodNumber = 2; // Start two tests
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = commonTestRun(BeforeAfterAllBeforeAfterEachTwoTestsTest.class,
				allItemNumber
		);
		verifyNoMoreInteractions(BeforeAfterAllTestExtension.LAUNCHES.get());

		verify_suite_calls_positive_finish(launchCalls);

		String suiteUuid = BeforeAfterAllTestExtension.ITEM_ID_MAP.get(launchCalls.getKey().get(0).getValue());
		verify_before_class_positive_calls(1, suiteUuid, launchCalls);
		verify_before_each_positive_calls(2, suiteUuid, launchCalls);
		verify_test_positive_calls(3, suiteUuid, launchCalls);
		verify_after_each_positive_calls(4, suiteUuid, launchCalls);
		verify_before_each_positive_calls(5, suiteUuid, launchCalls);
		verify_test_positive_calls(6, suiteUuid, launchCalls);
		verify_after_each_positive_calls(7, suiteUuid, launchCalls);
		verify_after_class_positive_calls(8, suiteUuid, launchCalls);
	}

	@Test
	public void test_before_after_non_static() {
		int suitesNumber = 1; // start a parent suite once
		int testBeforeAfterNumber = 2; // Start a before all, after all
		int testMethodNumber = 1; // Start a test
		int allItemNumber = suitesNumber + testMethodNumber + testBeforeAfterNumber;

		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls = commonTestRun(NonStaticBeforeAfterAllTest.class,
				allItemNumber
		);
		verifyNoMoreInteractions(BeforeAfterAllTestExtension.LAUNCHES.get());

		verify_suite_calls_positive_finish(launchCalls);

		String suiteUuid = BeforeAfterAllTestExtension.ITEM_ID_MAP.get(launchCalls.getKey().get(0).getValue());
		verify_before_class_positive_calls(1, suiteUuid, launchCalls);
		verify_test_positive_calls(2, suiteUuid, launchCalls);
		verify_after_class_positive_calls(3, suiteUuid, launchCalls);
	}
}
