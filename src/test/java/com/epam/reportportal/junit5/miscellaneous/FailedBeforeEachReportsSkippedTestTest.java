package com.epam.reportportal.junit5.miscellaneous;

import com.epam.reportportal.junit5.ReportPortalExtension;
import com.epam.reportportal.junit5.features.skipped.AfterEachFailedTest;
import com.epam.reportportal.junit5.features.skipped.BeforeEachFailedParametrizedTest;
import com.epam.reportportal.junit5.features.skipped.BeforeEachFailedTest;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.step.StepReporter;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.epam.reportportal.junit5.miscellaneous.FailedBeforeEachReportsSkippedTestTest.SkippedTestExtension.ITEMS;
import static com.epam.reportportal.junit5.miscellaneous.FailedBeforeEachReportsSkippedTestTest.SkippedTestExtension.LAUNCH;
import static com.epam.reportportal.junit5.util.Verifications.verify_call_number_and_capture_arguments;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FailedBeforeEachReportsSkippedTestTest {
	public static class SkippedTestExtension extends ReportPortalExtension {

		final static List<String> ITEMS = new ArrayList<>();
		static Launch LAUNCH;

		@Override
		protected Launch getLaunch(ExtensionContext context) {
			return LAUNCH;
		}
	}

	@BeforeEach
	public void setupMock() {
		ITEMS.clear();
		LAUNCH = mock(Launch.class);
		when(LAUNCH.getStepReporter()).thenReturn(StepReporter.NOOP_STEP_REPORTER);
		when(LAUNCH.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> {
			Maybe<String> result = CommonUtils.createMaybeUuid();
			ITEMS.add(result.blockingGet());
			return result;
		});
		when(LAUNCH.startTestItem(any(), any())).thenAnswer((Answer<Maybe<String>>) invocation -> {
			Maybe<String> result = CommonUtils.createMaybeUuid();
			ITEMS.add(result.blockingGet());
			return result;
		});
	}

	@Test
	public void agent_should_report_skipped_test_in_case_of_failed_before_each() {
		TestUtils.runClasses(BeforeEachFailedTest.class);
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> calls = verify_call_number_and_capture_arguments(3,
				LAUNCH
		);
		verifyNoMoreInteractions(LAUNCH);

		assertThat("There are 3 item created: suite, @BeforeEach and @Test", ITEMS, hasSize(3));

		FinishTestItemRQ beforeEachFinish = calls.getValue().get(ITEMS.get(1));

		assertThat("@BeforeEach failed", beforeEachFinish.getStatus(), equalTo("FAILED"));

		StartTestItemRQ testStart = calls.getKey().get(2).getValue();
		assertThat("@Test has correct code reference",
				testStart.getCodeRef(),
				equalTo(BeforeEachFailedTest.class.getCanonicalName() + ".testBeforeEachFailed")
		);

		assertThat("@Test has correct name", testStart.getName(), equalTo("testBeforeEachFailed()"));

		FinishTestItemRQ testFinish = calls.getValue().get(ITEMS.get(2));
		assertThat("@Test reported as skipped", testFinish.getStatus(), equalTo(ItemStatus.SKIPPED.name()));
		assertThat("@Test issue muted", testFinish.getIssue(), sameInstance(Launch.NOT_ISSUE));
		Date currentDate = new Date();
		assertThat(testFinish.getEndTime(), lessThanOrEqualTo(currentDate));
	}

	@Test
	public void agent_should_report_skipped_parametrized_tests_in_case_of_failed_before_each() {
		TestUtils.runClasses(BeforeEachFailedParametrizedTest.class);
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> calls = verify_call_number_and_capture_arguments(6,
				1,
				LAUNCH
		);
		verifyNoMoreInteractions(LAUNCH);

		assertThat("There are 6 item created: parent suite, suite, 2 @BeforeEach, @Test 1 and @Test 2", ITEMS, hasSize(6));

		FinishTestItemRQ beforeEachFinish = calls.getValue().get(ITEMS.get(2));

		assertThat("@BeforeEach failed", beforeEachFinish.getStatus(), equalTo("FAILED"));

		Date currentDate = new Date();
		IntStream.rangeClosed(0, 1).boxed().forEach(i -> {
			StartTestItemRQ testStart = calls.getKey().get(3 + (i * 2)).getValue();
			assertThat("@Test has correct code reference",
					testStart.getCodeRef(),
					equalTo(BeforeEachFailedParametrizedTest.class.getCanonicalName() + ".testBeforeEachFailed")
			);

			assertThat("@Test has correct name",
					testStart.getName(),
					equalTo("[" + (i + 1) + "] " + BeforeEachFailedParametrizedTest.TestParams.values()[i])
			);

			FinishTestItemRQ testFinish = calls.getValue().get(ITEMS.get(3 + (i * 2)));
			assertThat("@Test reported as skipped", testFinish.getStatus(), equalTo(ItemStatus.SKIPPED.name()));
			assertThat("@Test issue muted", testFinish.getIssue(), sameInstance(Launch.NOT_ISSUE));
			assertThat(testFinish.getEndTime(), lessThanOrEqualTo(currentDate));
		});
	}

	@Test
	public void agent_should_not_report_skipped_test_in_case_of_failed_after_each() {
		TestUtils.runClasses(AfterEachFailedTest.class);
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> calls = verify_call_number_and_capture_arguments(4,
				LAUNCH
		);
		verifyNoMoreInteractions(LAUNCH);

		assertThat("There are 4 item created: suite, @BeforeEach, @Test and @AfterEach", ITEMS, hasSize(4));

		FinishTestItemRQ beforeEachFinish = calls.getValue().get(ITEMS.get(1));

		assertThat("@BeforeEach passed", beforeEachFinish.getStatus(), equalTo("PASSED"));

		FinishTestItemRQ testFinish = calls.getValue().get(ITEMS.get(2));
		assertThat("@Test passed", testFinish.getStatus(), equalTo("PASSED"));

		FinishTestItemRQ afterEachFinish = calls.getValue().get(ITEMS.get(3));
		assertThat("@AfterEach failed", afterEachFinish.getStatus(), equalTo("FAILED"));
	}
}
