package com.epam.reportportal.junit5;

import com.epam.reportportal.junit5.features.disabled.OneDisabledOneEnabledTest;
import com.epam.reportportal.junit5.features.disabled.OneDisabledTest;
import com.epam.reportportal.junit5.features.disabled.OneDisabledTestWithReason;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.step.StepReporter;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DisabledTestTest {

	public static class DisabledTestExtension extends ReportPortalExtension {
		static Launch LAUNCH;

		@Override
		protected Launch getLaunch(ExtensionContext context) {
			return LAUNCH;
		}
	}

	@BeforeAll
	public void setupProperty() {
		System.setProperty("reportDisabledTests", Boolean.TRUE.toString());
	}

	@BeforeEach
	public void setupMock() {
		DisabledTestExtension.LAUNCH = mock(Launch.class);
		when(DisabledTestExtension.LAUNCH.getStepReporter()).thenReturn(StepReporter.NOOP_STEP_REPORTER);
		when(DisabledTestExtension.LAUNCH.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> CommonUtils.createMaybeUuid());
		when(DisabledTestExtension.LAUNCH.startTestItem(any(),
				any()
		)).thenAnswer((Answer<Maybe<String>>) invocation -> CommonUtils.createMaybeUuid());

	}

	@Test
	public void verify_a_disabled_test_reported_as_skipped() {
		TestUtils.runClasses(OneDisabledTest.class);

		Launch launch = DisabledTestExtension.LAUNCH;

		verify(launch, times(1)).startTestItem(any()); // Start parent Suite
		ArgumentCaptor<StartTestItemRQ> captorStart = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(1)).startTestItem(notNull(), captorStart.capture()); // Start a test

		ArgumentCaptor<FinishTestItemRQ> captorFinish = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch, times(2)).finishTestItem(notNull(), captorFinish.capture()); // finish a test and a suite

		List<StartTestItemRQ> steps = captorStart.getAllValues();

		assertThat("There is only one StartTestItem request", steps, hasSize(1));
		assertThat("StartTestItem request has proper Description field",
				steps.get(0).getDescription(),
				equalTo("void " + OneDisabledTest.class.getCanonicalName() + ".disabledTest() is @Disabled")
		);

		List<FinishTestItemRQ> finishes = captorFinish.getAllValues();
		assertThat("There are only finish test and finish suite requests", finishes, hasSize(2));

		FinishTestItemRQ finishStep = finishes.get(0);
		assertThat("Finish item has skipped status", finishStep.getStatus(), equalTo(ItemStatus.SKIPPED.name()));
	}

	@Test
	public void verify_a_disabled_test_reason_reported() {
		TestUtils.runClasses(OneDisabledTestWithReason.class);

		Launch launch = DisabledTestExtension.LAUNCH;

		verify(launch, times(1)).startTestItem(any()); // Start parent Suite
		ArgumentCaptor<StartTestItemRQ> captorStart = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(1)).startTestItem(notNull(), captorStart.capture()); // Start a test

		List<StartTestItemRQ> steps = captorStart.getAllValues();
		assertThat("StartTestItem request has proper Description field",
				captorStart.getAllValues().get(0).getDescription(),
				equalTo(OneDisabledTestWithReason.REASON)
		);
	}

	@Test
	public void verify_a_disabled_test_does_not_affect_enabled_in_the_same_class() {
		TestUtils.runClasses(OneDisabledOneEnabledTest.class);

		Launch launch = DisabledTestExtension.LAUNCH;

		verify(launch, times(1)).startTestItem(any()); // Start parent Suite
		ArgumentCaptor<StartTestItemRQ> captorStart = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(2)).startTestItem(notNull(), captorStart.capture()); // Start a disabled test and an enabled one

		ArgumentCaptor<FinishTestItemRQ> captorFinish = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch, times(3)).finishTestItem(notNull(), captorFinish.capture()); // finish two tests and a suite

		List<StartTestItemRQ> steps = captorStart.getAllValues();
		assertThat("There two StartTestItem request", steps, hasSize(2));
		assertThat("StartTestItem request for enabled test has proper Name field",
				steps.get(1).getName(),
				equalTo(OneDisabledOneEnabledTest.DISPLAY_NAME)
		);

		List<FinishTestItemRQ> finishes = captorFinish.getAllValues();
		assertThat("There are two finish tests and a finish suite requests", finishes, hasSize(3));

		FinishTestItemRQ finishStep = finishes.get(0);
		assertThat("Disabled test has skipped status", finishStep.getStatus(), equalTo(ItemStatus.SKIPPED.name()));

		finishStep = finishes.get(1);
		assertThat("Enabled has passed status", finishStep.getStatus(), equalTo(ItemStatus.PASSED.name()));
	}

	@AfterAll
	public void cleanProperty() {
		System.clearProperty("reportDisabledTests");
	}
}
