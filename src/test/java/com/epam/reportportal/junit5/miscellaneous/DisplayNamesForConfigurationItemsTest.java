package com.epam.reportportal.junit5.miscellaneous;

import com.epam.reportportal.junit5.ReportPortalExtension;
import com.epam.reportportal.junit5.features.configname.ConfigurationMethodsDefaultDisplayNameTest;
import com.epam.reportportal.junit5.features.configname.ConfigurationMethodsDisplayNameGeneratorTest;
import com.epam.reportportal.junit5.features.configname.ConfigurationMethodsDisplayNameTest;
import com.epam.reportportal.junit5.features.configname.CustomDisplayNameGenerator;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.step.StepReporter;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.epam.reportportal.junit5.miscellaneous.DisplayNamesForConfigurationItemsTest.TestExtension.LAUNCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DisplayNamesForConfigurationItemsTest {

	public static class TestExtension extends ReportPortalExtension {
		static Launch LAUNCH;

		@Override
		protected Launch getLaunch(ExtensionContext context) {
			return LAUNCH;
		}
	}

	@BeforeEach
	public void setupMock() {
		LAUNCH = mock(Launch.class);
		when(LAUNCH.getStepReporter()).thenReturn(StepReporter.NOOP_STEP_REPORTER);
		when(LAUNCH.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> CommonUtils.createMaybeUuid());
		when(LAUNCH.startTestItem(any(), any())).thenAnswer((Answer<Maybe<String>>) invocation -> CommonUtils.createMaybeUuid());
	}

	@Test
	public void test_configuration_method_display_name_annotation() {
		TestUtils.runClasses(ConfigurationMethodsDisplayNameTest.class);

		verify(LAUNCH, times(1)).startTestItem(any()); // Start parent Suite
		ArgumentCaptor<StartTestItemRQ> captorStart = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(LAUNCH, times(5)).startTestItem(notNull(), captorStart.capture()); // Start a suite and two tests

		captorStart.getAllValues().forEach(i -> assertThat(i.getName(), equalTo(ConfigurationMethodsDisplayNameTest.DISPLAY_NAME)));
	}

	@Test
	public void test_configuration_method_display_name_generator() {
		TestUtils.runClasses(ConfigurationMethodsDisplayNameGeneratorTest.class);

		ArgumentCaptor<StartTestItemRQ> startSuiteCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(LAUNCH, times(1)).startTestItem(startSuiteCaptor.capture()); // Start parent Suite
		ArgumentCaptor<StartTestItemRQ> startItemCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(LAUNCH, times(5)).startTestItem(notNull(), startItemCaptor.capture()); // Start a suite and two tests

		assertThat(startSuiteCaptor.getValue().getName(), equalTo(CustomDisplayNameGenerator.DISPLAY_NAME_CLASS));

		startItemCaptor.getAllValues().forEach(i -> assertThat(i.getName(), equalTo(CustomDisplayNameGenerator.DISPLAY_NAME_METHOD)));
	}

	@Test
	public void test_configuration_method_display_name_default_values() {
		TestUtils.runClasses(ConfigurationMethodsDefaultDisplayNameTest.class);

		verify(LAUNCH, times(1)).startTestItem(any()); // Start parent Suite
		ArgumentCaptor<StartTestItemRQ> startItemCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(LAUNCH, times(5)).startTestItem(notNull(), startItemCaptor.capture()); // Start a suite and two tests

		List<String> itemNames = startItemCaptor.getAllValues()
				.stream()
				.map(StartTestItemRQ::getName)
				.filter(e -> !e.startsWith("test"))
				.sorted()
				.collect(Collectors.toList());
		List<String> methodNames = Arrays.stream(ConfigurationMethodsDefaultDisplayNameTest.class.getMethods())
				.map(Method::getName)
				.filter(e -> e.startsWith("before") || e.startsWith("after"))
				.sorted()
				.collect(Collectors.toList());

		IntStream.range(0, itemNames.size()).forEach(i -> assertThat(itemNames.get(i), equalTo(methodNames.get(i) + "()")));
	}
}
