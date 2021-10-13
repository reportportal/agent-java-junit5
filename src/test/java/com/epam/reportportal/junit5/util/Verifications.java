package com.epam.reportportal.junit5.util;

import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * A static class with agent call assertions.
 */
public class Verifications {

	private Verifications() {
	}

	public static Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> verify_call_number_and_capture_arguments(
			int itemNum, Launch launch) {
		return verify_call_number_and_capture_arguments(itemNum, 0, launch);
	}

	@SuppressWarnings("unchecked")
	public static Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> verify_call_number_and_capture_arguments(
			int itemNum, int templateNum, Launch launch) {
		ArgumentCaptor<Maybe<String>> parentItemIdCaptor = ArgumentCaptor.forClass(Maybe.class);
		ArgumentCaptor<StartTestItemRQ> startItemCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<Maybe<String>> finishItemId = ArgumentCaptor.forClass(Maybe.class);
		ArgumentCaptor<FinishTestItemRQ> finishItemCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch, times(1)).startTestItem(startItemCaptor.capture());
		verify(launch, times(itemNum - 1)).startTestItem(parentItemIdCaptor.capture(), startItemCaptor.capture());
		verify(launch, times(itemNum)).finishTestItem(finishItemId.capture(), finishItemCaptor.capture());
		verifyNoMoreInteractions(launch);

		List<String> startParentItemIds = parentItemIdCaptor.getAllValues()
				.stream()
				.map(m -> m == null ? null : m.blockingGet())
				.collect(Collectors.toList());
		List<StartTestItemRQ> startItemValues = startItemCaptor.getAllValues();
		List<Pair<String, StartTestItemRQ>> startResult = IntStream.range(0, startItemValues.size())
				.mapToObj(i -> ImmutablePair.of(i == 0 ? null : startParentItemIds.get(i - 1), startItemValues.get(i)))
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

	public static void verify_correct_start_item_responses(String itemType, int callIndex, String suiteUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		Pair<String, StartTestItemRQ> beforeClassStartCall = launchCalls.getKey().get(callIndex);
		assertThat(String.format("Check %s has correct parent ID", itemType), beforeClassStartCall.getKey(), equalTo(suiteUuid));
		assertThat(String.format("Check %s has correct type", itemType), beforeClassStartCall.getValue().getType(), equalTo(itemType));
	}

	public static void verify_items_finish_statuses(String type, String status, String testUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		assertThat("Check there is a finish test item call", launchCalls.getValue(), hasKey(testUuid));
		FinishTestItemRQ rq = launchCalls.getValue().get(testUuid);

		assertThat(String.format("Check %s has no issue", type), rq.getIssue(), nullValue());
		assertThat(String.format("Check %s has no retry", type), rq.isRetry(), anyOf(nullValue(), equalTo(Boolean.FALSE)));
		assertThat(String.format("Check %s has %s status", type, status), rq.getStatus(), equalTo(status));
	}

	public static void verify_items_positive_finish_statuses(String type, String testUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		verify_items_finish_statuses(type, ItemStatus.PASSED.name(), testUuid, launchCalls);
	}

	public static void verify_items_negative_finish_statuses(String type, String testUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		verify_items_finish_statuses(type, ItemStatus.FAILED.name(), testUuid, launchCalls);
	}

	public static void verify_correct_suite_start_responses(
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		int callIndex = 0; // usually a suite goes first
		String itemType = "SUITE";
		Pair<String, StartTestItemRQ> suiteStartCall = launchCalls.getKey().get(callIndex);
		assertThat("Check suite has 'null' parent ID", suiteStartCall.getKey(), nullValue());
		assertThat("Check suite has correct type", suiteStartCall.getValue().getType(), equalTo(itemType));
	}

	public static void verify_suite_calls_positive_finish(String testUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		verify_correct_suite_start_responses(launchCalls);
		verify_items_positive_finish_statuses("SUITE", testUuid, launchCalls);
	}

	public static void verify_suite_calls_negative_finish(String testUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		verify_correct_suite_start_responses(launchCalls);
		verify_items_negative_finish_statuses("SUITE", testUuid, launchCalls);
	}

	public static void verify_before_class_positive_calls(int callIndex, String suiteUuid, String testUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		String itemType = "BEFORE_CLASS";
		verify_correct_start_item_responses(itemType, callIndex, suiteUuid, launchCalls);
		verify_items_positive_finish_statuses(itemType, testUuid, launchCalls);
	}

	public static void verify_before_class_negative_calls(int callIndex, String suiteUuid, String testUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		String itemType = "BEFORE_CLASS";
		verify_correct_start_item_responses(itemType, callIndex, suiteUuid, launchCalls);
		verify_items_negative_finish_statuses(itemType, testUuid, launchCalls);
	}

	public static void verify_before_each_positive_calls(int callIndex, String suiteUuid, String testUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		String itemType = "BEFORE_METHOD";
		verify_correct_start_item_responses(itemType, callIndex, suiteUuid, launchCalls);
		verify_items_positive_finish_statuses(itemType, testUuid, launchCalls);
	}

	public static void verify_after_class_positive_calls(int callIndex, String suiteUuid, String testUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		String itemType = "AFTER_CLASS";
		verify_correct_start_item_responses(itemType, callIndex, suiteUuid, launchCalls);
		verify_items_positive_finish_statuses(itemType, testUuid, launchCalls);
	}

	public static void verify_after_class_negative_calls(int callIndex, String suiteUuid, String testUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		String itemType = "AFTER_CLASS";
		verify_correct_start_item_responses(itemType, callIndex, suiteUuid, launchCalls);
		verify_items_negative_finish_statuses(itemType, testUuid, launchCalls);
	}

	public static void verify_after_each_positive_calls(int callIndex, String suiteUuid, String testUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		String itemType = "AFTER_METHOD";
		verify_correct_start_item_responses(itemType, callIndex, suiteUuid, launchCalls);
		verify_items_positive_finish_statuses(itemType, testUuid, launchCalls);
	}

	public static void verify_after_each_negative_calls(int callIndex, String suiteUuid, String testUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		String itemType = "AFTER_METHOD";
		verify_correct_start_item_responses(itemType, callIndex, suiteUuid, launchCalls);
		verify_items_negative_finish_statuses(itemType, testUuid, launchCalls);
	}

	public static void verify_test_positive_calls(int callIndex, String suiteUuid, String testUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		String itemType = "STEP";
		verify_correct_start_item_responses(itemType, callIndex, suiteUuid, launchCalls);
		verify_items_positive_finish_statuses(itemType, testUuid, launchCalls);
	}

	public static void verify_test_negative_calls(int callIndex, String suiteUuid, String testUuid,
			Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> launchCalls) {
		String itemType = "STEP";
		verify_correct_start_item_responses(itemType, callIndex, suiteUuid, launchCalls);
		verify_items_negative_finish_statuses(itemType, testUuid, launchCalls);
	}
}
