package com.epam.reportportal.junit5.miscellaneous;

import com.epam.reportportal.junit5.ReportPortalExtension;
import com.epam.reportportal.junit5.features.skipped.BeforeEachFailedWithAfterEach;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.List;

import static com.epam.reportportal.junit5.miscellaneous.FailedBeforeEachSkippedTestOrder.SkippedTestExtension.LAUNCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class FailedBeforeEachSkippedTestOrder {
	public static class SkippedTestExtension extends ReportPortalExtension {
		static Launch LAUNCH;

		@Override
		protected Launch getLaunch(ExtensionContext context) {
			return LAUNCH;
		}
	}

	@BeforeEach
	public void setupMock() {
		LAUNCH = TestUtils.getBasicMockedLaunch();
	}

	@Test
	@RepeatedTest(10)
	public void test_skipped_test_order_by_start_time_after_failed_before_each() {
		TestUtils.runClasses(BeforeEachFailedWithAfterEach.class);

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(LAUNCH, times(3)).startTestItem(notNull(), captor.capture()); // Start a before, a test, an after

		List<StartTestItemRQ> rqValues = captor.getAllValues();
		Date firstDate = rqValues.get(0).getStartTime();
		for (int i = 1; i < rqValues.size(); i++) {
			Date itemDate = rqValues.get(i).getStartTime();
			assertThat(itemDate, greaterThan(firstDate));
			firstDate = itemDate;
		}
	}
}
