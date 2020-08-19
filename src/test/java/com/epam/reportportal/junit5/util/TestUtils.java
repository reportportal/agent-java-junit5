package com.epam.reportportal.junit5.util;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import io.reactivex.Maybe;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.mockito.stubbing.Answer;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.reportportal.util.test.CommonUtils.createMaybe;
import static com.epam.reportportal.util.test.CommonUtils.createMaybeUuid;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {

	private TestUtils() {
	}

	public static ListenerParameters standardParameters() {
		ListenerParameters result = new ListenerParameters();
		result.setClientJoin(false);
		result.setBatchLogsSize(1);
		result.setLaunchName("My-test-launch-" + CommonUtils.generateUniqueId());
		result.setProjectName("unit-test");
		result.setEnable(true);
		return result;
	}

	public static void runClasses(final Class<?>... testClasses) {
		runClasses(null, testClasses);
	}

	public static void runClasses(final TestExecutionListener listener, final Class<?>... testClasses) {
		ClassSelector[] classSelectors = Stream.of(testClasses).map(DiscoverySelectors::selectClass).toArray(ClassSelector[]::new);
		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request().selectors(classSelectors).build();
		LauncherConfig.Builder builder = LauncherConfig.builder();
		if (listener != null) {
			builder.addTestExecutionListeners(listener);
		}
		LauncherConfig config = builder.enableTestExecutionListenerAutoRegistration(false).build();
		LauncherFactory.create(config).execute(request);
	}

	public static Launch getBasicMockedLaunch() {
		Launch result = mock(Launch.class);
		when(result.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> createMaybeUuid());
		when(result.startTestItem(any(), any())).thenAnswer((Answer<Maybe<String>>) invocation -> createMaybeUuid());
		return result;
	}

	public static void mockLaunch(ReportPortalClient client, String launchUuid, String testClassUuid, String testMethodUuid) {
		mockLaunch(client, launchUuid, testClassUuid, Collections.singleton(testMethodUuid));
	}

	@SuppressWarnings("unchecked")
	public static void mockLaunch(ReportPortalClient client, String launchUuid, String testClassUuid,
			Collection<String> testMethodUuidList) {
		when(client.startLaunch(any())).thenReturn(createMaybe(new StartLaunchRS(launchUuid, 1L)));

		Maybe<ItemCreatedRS> testClassMaybe = createMaybe(new ItemCreatedRS(testClassUuid, testClassUuid));
		when(client.startTestItem(any())).thenReturn(testClassMaybe);

		List<Maybe<ItemCreatedRS>> responses = testMethodUuidList.stream()
				.map(uuid -> createMaybe(new ItemCreatedRS(uuid, uuid)))
				.collect(Collectors.toList());
		Maybe<ItemCreatedRS> first = responses.get(0);
		Maybe<ItemCreatedRS>[] other = responses.subList(1, responses.size()).toArray(new Maybe[0]);
		when(client.startTestItem(eq(testClassUuid), any())).thenReturn(first, other);
		new HashSet<>(testMethodUuidList).forEach(testMethodUuid -> when(client.finishTestItem(eq(testMethodUuid), any())).thenReturn(
				createMaybe(new OperationCompletionRS())));

		Maybe<OperationCompletionRS> testClassFinishMaybe = createMaybe(new OperationCompletionRS());
		when(client.finishTestItem(eq(testClassUuid), any())).thenReturn(testClassFinishMaybe);

		when(client.finishLaunch(eq(launchUuid), any())).thenReturn(createMaybe(new OperationCompletionRS()));
	}

	public static StartLaunchRQ launchRQ(ListenerParameters parameters) {
		StartLaunchRQ result = new StartLaunchRQ();
		result.setName(parameters.getLaunchName());
		result.setStartTime(Calendar.getInstance().getTime());
		return result;
	}

	public static void mockNestedSteps(ReportPortalClient client, Pair<String, String> parentNestedPair) {
		mockNestedSteps(client, Collections.singletonList(parentNestedPair));
	}

	@SuppressWarnings("unchecked")
	public static void mockNestedSteps(final ReportPortalClient client, final List<Pair<String, String>> parentNestedPairs) {
		Map<String, List<String>> responseOrders = parentNestedPairs.stream()
				.collect(Collectors.groupingBy(Pair::getKey, Collectors.mapping(Pair::getValue, Collectors.toList())));
		responseOrders.forEach((k, v) -> {
			List<Maybe<ItemCreatedRS>> responses = v.stream()
					.map(uuid -> CommonUtils.createMaybe(new ItemCreatedRS(uuid, uuid)))
					.collect(Collectors.toList());

			Maybe<ItemCreatedRS> first = responses.get(0);
			Maybe<ItemCreatedRS>[] other = responses.subList(1, responses.size()).toArray(new Maybe[0]);
			when(client.startTestItem(same(k), any())).thenReturn(first, other);
		});
		parentNestedPairs.forEach(p -> when(client.finishTestItem(
				same(p.getValue()),
				any()
		)).thenAnswer((Answer<Maybe<OperationCompletionRS>>) invocation -> CommonUtils.createMaybe(new OperationCompletionRS())));
	}
}
