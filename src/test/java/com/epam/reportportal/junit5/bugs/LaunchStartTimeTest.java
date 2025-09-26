package com.epam.reportportal.junit5.bugs;

import com.epam.reportportal.junit5.ReportPortalExtension;
import com.epam.reportportal.junit5.features.bug.TestIncorrectStartTime;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.ApiInfo;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

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
		TestUtils.mockLogging(client);
		ApiInfo info = new ApiInfo();
		ApiInfo.Build build = new ApiInfo.Build();
		info.setBuild(build);
		build.setVersion("5.13.2");
		when(client.getApiInfo()).thenReturn(Maybe.just(info));
		TestExtension.REPORT_PORTAL = ReportPortal.create(client, TestUtils.standardParameters());
	}

	@Test
	public void verify_start_time_has_correct_order() {
		TestUtils.runClasses(TestIncorrectStartTime.class);

		ArgumentCaptor<StartLaunchRQ> startLaunchCaptor = ArgumentCaptor.forClass(StartLaunchRQ.class);
		verify(client, timeout(TimeUnit.SECONDS.toMillis(2)).times(1)).startLaunch(startLaunchCaptor.capture());

		ArgumentCaptor<StartTestItemRQ> startSuiteCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(TimeUnit.SECONDS.toMillis(2)).times(1)).startTestItem(startSuiteCaptor.capture());

		ArgumentCaptor<StartTestItemRQ> startTestCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(TimeUnit.SECONDS.toMillis(2)).times(1)).startTestItem(any(), startTestCaptor.capture());

		Instant launchStart = (Instant) startLaunchCaptor.getValue().getStartTime();
		Instant suiteStart = (Instant) startSuiteCaptor.getValue().getStartTime();
		Instant itemStart = (Instant) startTestCaptor.getValue().getStartTime();

		assertThat(launchStart, lessThanOrEqualTo(suiteStart));
		assertThat(suiteStart, lessThanOrEqualTo(itemStart));
	}
}
