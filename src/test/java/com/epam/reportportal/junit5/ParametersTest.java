package com.epam.reportportal.junit5;

import com.epam.reportportal.junit5.features.parameters.*;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.step.StepReporter;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.ParameterResource;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.reportportal.junit5.ParametersTest.ParameterTestExtension.LAUNCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ParametersTest {

	public static class ParameterTestExtension extends ReportPortalExtension {
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
	public void verify_a_test_with_enum_parameters_reported() {
		TestUtils.runClasses(EnumParametersTest.class);

		verify(LAUNCH, times(1)).startTestItem(any()); // Start parent Suite
		ArgumentCaptor<StartTestItemRQ> captorStart = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(LAUNCH, times(3)).startTestItem(notNull(), captorStart.capture()); // Start a suite and two tests

		ArgumentCaptor<FinishTestItemRQ> captorFinish = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(LAUNCH, times(4)).finishTestItem(notNull(), captorFinish.capture()); // finish tests and suites

		List<StartTestItemRQ> testSteps = captorStart.getAllValues()
				.stream()
				.filter(e -> e.getType().equals(ItemType.STEP.name()))
				.collect(Collectors.toList());

		assertThat("There are two @Test methods", testSteps, hasSize(2));

		assertThat("There are only one parameter for the first @Test methods", testSteps.get(0).getParameters(), hasSize(1));
		assertThat("There are only one parameter for the second @Test methods", testSteps.get(1).getParameters(), hasSize(1));

		assertThat("First test parameter has correct type",
				testSteps.get(0).getParameters().get(0).getKey(),
				equalTo(EnumParametersTest.TestParams.class.getName())
		);

		assertThat("Second test parameter has correct type",
				testSteps.get(1).getParameters().get(0).getKey(),
				equalTo(EnumParametersTest.TestParams.class.getName())
		);

		assertThat("First test parameter has correct value",
				testSteps.get(0).getParameters().get(0).getValue(),
				equalTo(EnumParametersTest.TestParams.values()[0].name())
		);

		assertThat("Second test parameter has correct value",
				testSteps.get(1).getParameters().get(0).getValue(),
				equalTo(EnumParametersTest.TestParams.values()[1].name())
		);
	}

	@Test
	public void verify_a_test_with_csv_parameters_reported() {
		TestUtils.runClasses(CsvParametersTest.class);

		verify(LAUNCH, times(1)).startTestItem(any()); // Start parent Suite
		ArgumentCaptor<StartTestItemRQ> captorStart = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(LAUNCH, times(3)).startTestItem(notNull(), captorStart.capture()); // Start a suite and two tests

		ArgumentCaptor<FinishTestItemRQ> captorFinish = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(LAUNCH, times(4)).finishTestItem(notNull(), captorFinish.capture()); // finish tests and suites

		List<StartTestItemRQ> testSteps = captorStart.getAllValues()
				.stream()
				.filter(e -> e.getType().equals(ItemType.STEP.name()))
				.collect(Collectors.toList());

		assertThat("There are two @Test methods", testSteps, hasSize(2));

		assertThat("There are only one parameter for the first @Test methods", testSteps.get(0).getParameters(), hasSize(1));
		assertThat("There are only one parameter for the second @Test methods", testSteps.get(1).getParameters(), hasSize(1));

		assertThat("First test parameter has correct type",
				testSteps.get(0).getParameters().get(0).getKey(),
				equalTo(String.class.getName())
		);

		assertThat("Second test parameter has correct type",
				testSteps.get(1).getParameters().get(0).getKey(),
				equalTo(String.class.getName())
		);

		assertThat("First test parameter has correct value", testSteps.get(0).getParameters().get(0).getValue(), equalTo("one"));

		assertThat("Second test parameter has correct value", testSteps.get(1).getParameters().get(0).getValue(), equalTo("two"));
	}

	@Test
	public void verify_a_test_with_two_csv_parameters_reported() {
		TestUtils.runClasses(TwoParametersTest.class);

		verify(LAUNCH, times(1)).startTestItem(any()); // Start parent Suite
		ArgumentCaptor<StartTestItemRQ> captorStart = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(LAUNCH, times(3)).startTestItem(notNull(), captorStart.capture()); // Start a suite and two tests

		ArgumentCaptor<FinishTestItemRQ> captorFinish = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(LAUNCH, times(4)).finishTestItem(notNull(), captorFinish.capture()); // finish tests and suites

		List<StartTestItemRQ> testSteps = captorStart.getAllValues()
				.stream()
				.filter(e -> e.getType().equals(ItemType.STEP.name()))
				.collect(Collectors.toList());

		assertThat("There are two @Test methods", testSteps, hasSize(2));

		assertThat("There are two parameters for the first @Test methods", testSteps.get(0).getParameters(), hasSize(2));
		assertThat("There are two parameters for the second @Test methods", testSteps.get(1).getParameters(), hasSize(2));

		assertThat("First test parameters has correct type",
				testSteps.get(0).getParameters().stream().map(ParameterResource::getKey).collect(Collectors.toList()),
				everyItem(equalTo(String.class.getName()))
		);

		assertThat("Second test parameter has correct type",
				testSteps.get(1).getParameters().stream().map(ParameterResource::getKey).collect(Collectors.toList()),
				everyItem(equalTo(String.class.getName()))
		);

		assertThat("First test parameter has correct value",
				testSteps.get(0).getParameters().stream().map(ParameterResource::getValue).collect(Collectors.toList()),
				contains("one", "two")
		);

		assertThat("Second test parameter has correct value",
				testSteps.get(1).getParameters().stream().map(ParameterResource::getValue).collect(Collectors.toList()),
				contains("three", "four")
		);
	}

	@Test
	public void verify_a_test_with_a_null_parameter_reported() {
		TestUtils.runClasses(NullParameterTest.class);

		verify(LAUNCH, times(1)).startTestItem(any()); // Start parent Suite
		ArgumentCaptor<StartTestItemRQ> captorStart = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(LAUNCH, times(3)).startTestItem(notNull(), captorStart.capture()); // Start a suite and two tests

		ArgumentCaptor<FinishTestItemRQ> captorFinish = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(LAUNCH, times(4)).finishTestItem(notNull(), captorFinish.capture()); // finish tests and suites

		List<StartTestItemRQ> testSteps = captorStart.getAllValues()
				.stream()
				.filter(e -> e.getType().equals(ItemType.STEP.name()))
				.collect(Collectors.toList());

		assertThat("There are two @Test methods", testSteps, hasSize(2));

		assertThat("There are only one parameter for the first @Test methods", testSteps.get(0).getParameters(), hasSize(1));
		assertThat("There are only one parameter for the second @Test methods", testSteps.get(1).getParameters(), hasSize(1));

		assertThat("First test parameters has correct type",
				testSteps.get(0).getParameters().stream().map(ParameterResource::getKey).collect(Collectors.toList()),
				everyItem(equalTo(String.class.getName()))
		);

		assertThat("Second test parameter has correct type",
				testSteps.get(1).getParameters().stream().map(ParameterResource::getKey).collect(Collectors.toList()),
				everyItem(equalTo(String.class.getName()))
		);

		assertThat("First test parameter has correct value", testSteps.get(0).getParameters().get(0).getValue(), equalTo("NULL"));

		assertThat("Second test parameter has correct value", testSteps.get(1).getParameters().get(0).getValue(), equalTo("one"));
	}

	@Test
	public void verify_parameter_names_reported() {
		TestUtils.runClasses(ParameterNamesTest.class);

		ArgumentCaptor<StartTestItemRQ> captorStart = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(LAUNCH, times(3)).startTestItem(notNull(), captorStart.capture()); // Start a suite and two tests

		List<StartTestItemRQ> testSteps = captorStart.getAllValues()
				.stream()
				.filter(e -> e.getType().equals(ItemType.STEP.name()))
				.collect(Collectors.toList());

		assertThat("There are two parameters for the first @Test methods", testSteps.get(0).getParameters(), hasSize(2));
		assertThat("There are two parameters for the second @Test methods", testSteps.get(1).getParameters(), hasSize(2));

		testSteps.forEach(step -> {
			assertThat("Test first parameter has correct name",
					step.getParameters().get(0).getKey(),
					equalTo(ParameterNamesTest.FIRST_PARAMETER_NAME)
			);

			assertThat("Test second parameter has correct name",
					step.getParameters().get(1).getKey(),
					equalTo(ParameterNamesTest.SECOND_PARAMETER_NAME)
			);
		});
	}

	@Test
	public void verify_parameter_names_reported_when_not_all_of_them_set() {
		TestUtils.runClasses(ParameterNamesNotAllNamedTest.class);

		ArgumentCaptor<StartTestItemRQ> captorStart = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(LAUNCH, times(3)).startTestItem(notNull(), captorStart.capture()); // Start a suite and two tests

		List<StartTestItemRQ> testSteps = captorStart.getAllValues()
				.stream()
				.filter(e -> e.getType().equals(ItemType.STEP.name()))
				.collect(Collectors.toList());

		assertThat("There are two parameters for the first @Test methods", testSteps.get(0).getParameters(), hasSize(2));
		assertThat("There are two parameters for the second @Test methods", testSteps.get(1).getParameters(), hasSize(2));

		testSteps.forEach(step -> {
			assertThat("Test first parameter has correct name", step.getParameters().get(0).getKey(), equalTo("int"));

			assertThat("Test second parameter has correct name",
					step.getParameters().get(1).getKey(),
					equalTo(ParameterNamesTest.SECOND_PARAMETER_NAME)
			);
		});
	}

	@Test
	public void verify_test_template_finish_in_case_of_failed_parameterized_test() {
		TestUtils.runClasses(EnumParametersFailedTest.class);

		verify(LAUNCH, times(1)).startTestItem(any()); // Start parent Suite
		ArgumentCaptor<StartTestItemRQ> captorStart = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(LAUNCH, times(3)).startTestItem(notNull(), captorStart.capture()); // Start a suite and two tests

		ArgumentCaptor<FinishTestItemRQ> captorFinish = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(LAUNCH, times(4)).finishTestItem(notNull(), captorFinish.capture()); // finish tests and suites

		List<FinishTestItemRQ> finishMethods = captorFinish.getAllValues();

		Stream.concat(finishMethods.subList(0, 1).stream(), finishMethods.subList(2, finishMethods.size()).stream())
				.forEach(f -> assertThat(f.getStatus(), equalTo(ItemStatus.FAILED.name())));

		assertThat(finishMethods.get(1).getStatus(), equalTo(ItemStatus.PASSED.name()));
	}
}
