package com.epam.reportportal.junit5.util;

import io.reactivex.Maybe;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.util.UUID;
import java.util.stream.Stream;

public class TestUtils {

	private TestUtils() {
	}

	public static void runClasses(final Class<?>... testClasses) {
		ClassSelector[] classSelectors = Stream.of(testClasses).map(DiscoverySelectors::selectClass).toArray(ClassSelector[]::new);
		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request().selectors(classSelectors).build();
		LauncherConfig config = LauncherConfig.builder().enableTestExecutionListenerAutoRegistration(false).build();
		LauncherFactory.create(config).execute(request);
	}

	public static Maybe<String> createItemUuidMaybe() {
		final String uuid = UUID.randomUUID().toString();
		return Maybe.create(emitter -> {
			emitter.onSuccess(uuid);
			emitter.onComplete();
		});
	}
}
