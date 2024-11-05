/*
 * Copyright 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.junit5;

import com.epam.reportportal.junit5.features.TestFailure;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.mockito.ArgumentCaptor;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import static com.epam.reportportal.util.test.CommonUtils.namedId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BasicTest {

	public static class BasicTestExtension extends ReportPortalExtension {
		static final String testClassUuid = namedId("class");
		static final String testMethodUuid = namedId("test");

		static final ThreadLocal<ReportPortalClient> client = new ThreadLocal<>();
		static final ThreadLocal<Launch> launch = new ThreadLocal<>();

		public static void init() {
			client.set(mock(ReportPortalClient.class));
			TestUtils.mockLaunch(client.get(), "launchUuid", testClassUuid, testMethodUuid);
			TestUtils.mockLogging(client.get());
			ReportPortal reportPortal = ReportPortal.create(client.get(), TestUtils.standardParameters());
			launch.set(reportPortal.newLaunch(TestUtils.launchRQ(reportPortal.getParameters())));
		}

		@Override
		protected Launch getLaunch(ExtensionContext context) {
			return launch.get();
		}
	}

	public static class Listener implements TestExecutionListener {
		public Deque<TestExecutionResult> results = new ConcurrentLinkedDeque<>();

		@Override
		public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
			if (testIdentifier.isTest()) {
				results.add(testExecutionResult);
			}
		}
	}

	@Test
	public void verify_test_failure_report() {
		BasicTestExtension.init();
		Listener listener = new Listener();
		TestUtils.runClasses(listener, TestFailure.class);

		ReportPortalClient client = BasicTestExtension.client.get();
		ArgumentCaptor<StartLaunchRQ> launchCaptor = ArgumentCaptor.forClass(StartLaunchRQ.class);
		verify(client, timeout(1000)).startLaunch(launchCaptor.capture());

		ArgumentCaptor<StartTestItemRQ> suiteCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(1000)).startTestItem(suiteCaptor.capture());

		ArgumentCaptor<StartTestItemRQ> testCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(1000)).startTestItem(anyString(), testCaptor.capture());

		ArgumentCaptor<FinishTestItemRQ> testFinishCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000).times(2)).finishTestItem(anyString(), testFinishCaptor.capture());

		assertThat(listener.results.remove().getStatus(), sameInstance(TestExecutionResult.Status.FAILED));

		StartLaunchRQ launchStart = launchCaptor.getValue();
		assertThat(launchStart.getName(), allOf(notNullValue(), Matchers.startsWith("My-test-launch-")));

		List<FinishTestItemRQ> itemFinish = testFinishCaptor.getAllValues();
		assertThat(itemFinish.get(0).getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(itemFinish.get(1).getStatus(), nullValue());
	}
}
