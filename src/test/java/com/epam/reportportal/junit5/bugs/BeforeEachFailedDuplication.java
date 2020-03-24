package com.epam.reportportal.junit5.bugs;

import com.epam.reportportal.junit5.ItemType;
import com.epam.reportportal.junit5.ReportPortalExtension;
import com.epam.reportportal.junit5.features.bug.BeforeEachFailedDuplicate;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BeforeEachFailedDuplication {
	public static class BeforeEachFailedDuplicationExtension extends ReportPortalExtension {
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
		System.setProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "2");
	}

	@BeforeEach
	public void setupMock() {
		BeforeEachFailedDuplicationExtension.LAUNCH = mock(Launch.class);
		when(BeforeEachFailedDuplicationExtension.LAUNCH.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> TestUtils.createItemUuidMaybe());
		when(BeforeEachFailedDuplicationExtension.LAUNCH.startTestItem(any(),
				any()
		)).thenAnswer((Answer<Maybe<String>>) invocation -> TestUtils.createItemUuidMaybe());
	}

	@Test
	public void verify_() {
		TestUtils.runClasses(BeforeEachFailedDuplicate.class);

		Launch launch = BeforeEachFailedDuplicationExtension.LAUNCH;

		verify(launch, times(1)).startTestItem(any()); // Start parent Suite
		ArgumentCaptor<StartTestItemRQ> captorStart = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(16)).startTestItem(notNull(), captorStart.capture()); // Start a test

		ArgumentCaptor<FinishTestItemRQ> captorFinish = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch, times(17)).finishTestItem(notNull(), captorFinish.capture()); // finish a test and a suite

		List<StartTestItemRQ> steps = captorStart.getAllValues();

		List<StartTestItemRQ> startMethodList = steps.stream()
				.filter(e -> e.getType().equals(ItemType.BEFORE_METHOD.name()))
				.collect(Collectors.toList());

		assertThat("Before each request list have proper size", startMethodList, hasSize(4));

		// TODO: Finish this test
	}

	@AfterAll
	public void cleanUpTestParams() {
		System.clearProperty("junit.jupiter.execution.parallel.enabled");
		System.clearProperty("junit.jupiter.execution.parallel.mode.default");
		System.clearProperty("junit.jupiter.execution.parallel.config.strategy");
		System.clearProperty("junit.jupiter.execution.parallel.config.fixed.parallelism");
	}
}
