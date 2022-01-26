package com.epam.reportportal.junit5;

import com.epam.reportportal.junit5.features.nested.JunitClassNestedTests;
import com.epam.reportportal.junit5.features.nested.JunitClassNestedTestsLongDepth;
import com.epam.reportportal.junit5.features.nested.JunitDynamicNestedTest;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.step.StepReporter;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JunitNestedTestTest {

	public static class TestExtension extends ReportPortalExtension {
		static Launch LAUNCH;

		@Override
		protected Launch getLaunch(ExtensionContext context) {
			return LAUNCH;
		}
	}

	@BeforeEach
	public void setupMock() {
		TestExtension.LAUNCH = mock(Launch.class);
		when(TestExtension.LAUNCH.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> {
			StartTestItemRQ rq = invocation.getArgument(0);
			return Maybe.just(CommonUtils.namedId("suite-" + rq.getName() + "-"));
		});
		when(TestExtension.LAUNCH.startTestItem(any(), any())).thenAnswer((Answer<Maybe<String>>) invocation -> {
			StartTestItemRQ rq = invocation.getArgument(1);
			return Maybe.just(CommonUtils.namedId(rq.getType().toLowerCase() + "-" + rq.getName() + "-"));
		});
		when(TestExtension.LAUNCH.getStepReporter()).thenReturn(StepReporter.NOOP_STEP_REPORTER);
	}

	private static List<Pair<Pair<Maybe<String>, StartTestItemRQ>, Pair<Maybe<String>, FinishTestItemRQ>>> toList(
			ArgumentCaptor<StartTestItemRQ> suiteCaptor, ArgumentCaptor<Maybe<String>> parentItemIdCaptor,
			ArgumentCaptor<StartTestItemRQ> itemRqCaptor, ArgumentCaptor<Maybe<String>> finishTestIdCaptor,
			ArgumentCaptor<FinishTestItemRQ> finishItemRqCaptor) {
		List<Maybe<String>> finishTestIds = finishTestIdCaptor.getAllValues();
		List<FinishTestItemRQ> finishItemRqs = finishItemRqCaptor.getAllValues();

		List<Pair<Pair<Maybe<String>, StartTestItemRQ>, Pair<Maybe<String>, FinishTestItemRQ>>> result = new ArrayList<>();
		result.add(Pair.of(Pair.of(null, suiteCaptor.getValue()),
				Pair.of(finishTestIds.get(finishTestIds.size() - 1), finishItemRqs.get(finishItemRqs.size() - 1))
		));

		List<Maybe<String>> parentIds = parentItemIdCaptor.getAllValues();
		List<StartTestItemRQ> itemRqs = itemRqCaptor.getAllValues();

		result.addAll(IntStream.range(0, parentIds.size()).mapToObj(i -> Pair.of(Pair.of(parentIds.get(i), itemRqs.get(i)),
				Pair.of(finishTestIds.get(finishTestIds.size() - 2 - i), finishItemRqs.get(finishItemRqs.size() - 2 - i))
		)).collect(Collectors.toList()));
		return result;
	}

	@Test
	@SuppressWarnings("unchecked")
	public void verify_dynamically_generated_nested_tests() {
		TestUtils.runClasses(JunitDynamicNestedTest.class);

		Launch launch = TestExtension.LAUNCH;

		ArgumentCaptor<StartTestItemRQ> suiteCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(1)).startTestItem(suiteCaptor.capture()); // Start parent Suite

		ArgumentCaptor<Maybe<String>> parentIdCaptor = ArgumentCaptor.forClass(Maybe.class);
		ArgumentCaptor<StartTestItemRQ> itemRqCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(9)).startTestItem(parentIdCaptor.capture(), itemRqCaptor.capture()); // Start inner items

		verify(launch, times(10)).finishTestItem(any(Maybe.class), any(FinishTestItemRQ.class));

		List<Maybe<String>> parentIds = parentIdCaptor.getAllValues();
		List<StartTestItemRQ> itemRqs = itemRqCaptor.getAllValues();

		int index = 0;
		String parentId = parentIds.get(index).blockingGet();
		StartTestItemRQ itemRq = itemRqs.get(index);
		assertThat(parentId, Matchers.startsWith("suite-JunitDynamicNestedTest-"));
		assertThat(itemRq.getName(), equalTo("dynamicTestsWithContainers()"));

		parentId = parentIds.get(++index).blockingGet();
		itemRq = itemRqs.get(index);
		assertThat(parentId, Matchers.startsWith("suite-dynamicTestsWithContainers()-"));
		assertThat(itemRq.getName(), equalTo("A"));

		parentId = parentIds.get(++index).blockingGet();
		itemRq = itemRqs.get(index);
		assertThat(parentId, Matchers.startsWith("suite-A-"));
		assertThat(itemRq.getName(), equalTo("A inner container"));

		parentId = parentIds.get(++index).blockingGet();
		itemRq = itemRqs.get(index);
		assertThat(parentId, Matchers.startsWith("suite-A inner container-"));
		assertThat(itemRq.getName(), equalTo("A Test 1"));

		parentId = parentIds.get(++index).blockingGet();
		itemRq = itemRqs.get(index);
		assertThat(parentId, Matchers.startsWith("suite-A inner container-"));
		assertThat(itemRq.getName(), equalTo("A Test 2"));

		parentId = parentIds.get(++index).blockingGet();
		itemRq = itemRqs.get(index);
		assertThat(parentId, Matchers.startsWith("suite-dynamicTestsWithContainers()-"));
		assertThat(itemRq.getName(), equalTo("B"));

		parentId = parentIds.get(++index).blockingGet();
		itemRq = itemRqs.get(index);
		assertThat(parentId, Matchers.startsWith("suite-B"));
		assertThat(itemRq.getName(), equalTo("B inner container"));

		parentId = parentIds.get(++index).blockingGet();
		itemRq = itemRqs.get(index);
		assertThat(parentId, Matchers.startsWith("suite-B inner container-"));
		assertThat(itemRq.getName(), equalTo("B Test 1"));

		parentId = parentIds.get(++index).blockingGet();
		itemRq = itemRqs.get(index);
		assertThat(parentId, Matchers.startsWith("suite-B inner container-"));
		assertThat(itemRq.getName(), equalTo("B Test 2"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void verify_static_nested_tests() {
		TestUtils.runClasses(JunitClassNestedTests.class);

		Launch launch = TestExtension.LAUNCH;

		ArgumentCaptor<StartTestItemRQ> suiteCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(1)).startTestItem(suiteCaptor.capture()); // Start parent Suite

		ArgumentCaptor<Maybe<String>> testIdCaptor = ArgumentCaptor.forClass(Maybe.class);
		ArgumentCaptor<StartTestItemRQ> itemRqCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(4)).startTestItem(testIdCaptor.capture(), itemRqCaptor.capture()); // Start inner items

		ArgumentCaptor<Maybe<String>> finishTestIdCaptor = ArgumentCaptor.forClass(Maybe.class);
		ArgumentCaptor<FinishTestItemRQ> finishItemRqCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch, times(5)).finishTestItem(finishTestIdCaptor.capture(), finishItemRqCaptor.capture());

		List<Pair<Pair<Maybe<String>, StartTestItemRQ>, Pair<Maybe<String>, FinishTestItemRQ>>> rqList = toList(suiteCaptor,
				testIdCaptor,
				itemRqCaptor,
				finishTestIdCaptor,
				finishItemRqCaptor
		);

		IntStream.range(1, 4).forEach(i -> {
			Pair<Pair<Maybe<String>, StartTestItemRQ>, Pair<Maybe<String>, FinishTestItemRQ>> item = rqList.get(i);
			Pair<Pair<Maybe<String>, StartTestItemRQ>, Pair<Maybe<String>, FinishTestItemRQ>> parent = rqList.get(i - 1);

			assertThat(item.getKey().getKey(), equalTo(parent.getValue().getKey()));
		});

		Pair<Pair<Maybe<String>, StartTestItemRQ>, Pair<Maybe<String>, FinishTestItemRQ>> item = rqList.get(4);
		Pair<Pair<Maybe<String>, StartTestItemRQ>, Pair<Maybe<String>, FinishTestItemRQ>> parent = rqList.get(2);

		assertThat(item.getKey().getKey(), equalTo(parent.getValue().getKey()));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void verify_static_nested_tests_long_depth() {
		TestUtils.runClasses(JunitClassNestedTestsLongDepth.class);

		Launch launch = TestExtension.LAUNCH;
		verify(launch, times(1)).startTestItem(any(StartTestItemRQ.class));
		verify(launch, times(11)).startTestItem(any(Maybe.class), any(StartTestItemRQ.class));
		verify(launch, times(12)).finishTestItem(any(Maybe.class), any(FinishTestItemRQ.class));
	}
}
