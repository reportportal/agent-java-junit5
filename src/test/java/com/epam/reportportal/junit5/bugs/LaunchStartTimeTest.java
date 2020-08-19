package com.epam.reportportal.junit5.bugs;

import com.epam.reportportal.junit5.ReportPortalExtension;
import com.epam.reportportal.junit5.features.bug.IncorrectStartTime;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LaunchStartTimeTest {

	private final String launchUuid = CommonUtils.namedId("launch");
	private final String suitedUuid = CommonUtils.namedId("suite");
	private final String testMethodUuid = CommonUtils.namedId("test");

	public static class TestExtension extends ReportPortalExtension {
		static volatile ReportPortal REPORT_PORTAL;

		@Override
		protected ReportPortal getReporter() {
			return REPORT_PORTAL;
		}
	}

	private ReportPortalClient client;

	@BeforeEach
	public void setupMock() {
		client = mock(ReportPortalClient.class);
		TestUtils.mockLaunch(client, launchUuid, suitedUuid, testMethodUuid);
		TestExtension.REPORT_PORTAL = ReportPortal.create(client, TestUtils.standardParameters());
	}

	@Test
	public void verify_start_time_has_correct_order() {
		TestUtils.runClasses(IncorrectStartTime.class);

		ArgumentCaptor<StartLaunchRQ> startLaunchCaptor = ArgumentCaptor.forClass(StartLaunchRQ.class);
		verify(client, timeout(1000).times(1)).startLaunch(startLaunchCaptor.capture());

		ArgumentCaptor<StartTestItemRQ> startSuiteCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(1000).times(1)).startTestItem(startSuiteCaptor.capture());

		ArgumentCaptor<StartTestItemRQ> startTestCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(1000).times(1)).startTestItem(any(), startTestCaptor.capture());

		Date launchStart = startLaunchCaptor.getValue().getStartTime();
		Date suiteStart = startSuiteCaptor.getValue().getStartTime();
		Date itemStart = startTestCaptor.getValue().getStartTime();

		assertThat(launchStart, lessThanOrEqualTo(suiteStart));
		assertThat(suiteStart, lessThanOrEqualTo(itemStart));
	}
}
