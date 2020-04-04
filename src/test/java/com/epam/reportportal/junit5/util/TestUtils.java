package com.epam.reportportal.junit5.util;

import com.epam.reportportal.service.Launch;
import io.reactivex.Maybe;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.mockito.stubbing.Answer;

import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {

	private TestUtils() {
	}

	public static void runClasses(final Class<?>... testClasses) {
		ClassSelector[] classSelectors = Stream.of(testClasses).map(DiscoverySelectors::selectClass).toArray(ClassSelector[]::new);
		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request().selectors(classSelectors).build();
		LauncherConfig config = LauncherConfig.builder().enableTestExecutionListenerAutoRegistration(false).build();
		LauncherFactory.create(config).execute(request);
	}

	public static Maybe<String> createMaybeUuid() {
		return createMaybe(UUID.randomUUID().toString());
	}

	public static <T> Maybe<T> createMaybe(T id) {
		return Maybe.create(emitter -> {
			emitter.onSuccess(id);
			emitter.onComplete();
		});
	}

	public static Launch getBasicMockedLaunch() {
		Launch result = mock(Launch.class);
		when(result.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> TestUtils.createMaybeUuid());
		when(result.startTestItem(any(), any())).thenAnswer((Answer<Maybe<String>>) invocation -> TestUtils.createMaybeUuid());
		return result;
	}
}
