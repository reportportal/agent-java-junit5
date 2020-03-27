package com.epam.reportportal.junit5.miscellaneous;

import com.epam.reportportal.junit5.ReportPortalExtension;
import com.epam.reportportal.junit5.features.skipped.BeforeEachFailedTest;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.Map;

import static com.epam.reportportal.junit5.miscellaneous.FailedBeforeEachReportsSkippedTestTest.SkippedTestExtension.LAUNCH;
import static com.epam.reportportal.junit5.util.Verifications.verify_call_number_and_capture_arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FailedBeforeEachReportsSkippedTestTest {
	public static class SkippedTestExtension extends ReportPortalExtension {
		static Launch LAUNCH;

		@Override
		protected Launch getLaunch(ExtensionContext context) {
			return LAUNCH;
		}
	}

	@BeforeEach
	public void setupMock() {
		LAUNCH = mock(Launch.class);
		when(LAUNCH.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> TestUtils.createMaybeUuid());
		when(LAUNCH.startTestItem(any(), any())).thenAnswer((Answer<Maybe<String>>) invocation -> TestUtils.createMaybeUuid());
	}

	@Test
	public void agent_should_report_skipped_test_in_case_of_failed_before_each() {
		TestUtils.runClasses(BeforeEachFailedTest.class);
		Pair<List<Pair<String, StartTestItemRQ>>, Map<String, FinishTestItemRQ>> calls = verify_call_number_and_capture_arguments(3,
				LAUNCH
		);
		verifyNoMoreInteractions(LAUNCH);

		// TODO: finish
	}
}
