/*
 * Copyright 2020 EPAM Systems
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

import com.epam.reportportal.junit5.features.step.ManualStepReporterFeatureTest;
import com.epam.reportportal.junit5.features.step.ManualStepReporterSimpleTest;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.reportportal.junit5.util.TestUtils.runClasses;
import static com.epam.reportportal.util.test.CommonUtils.namedId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class StepReporterTest {

	public static class TestExtension extends ReportPortalExtension {
		static final String testClassUuid = namedId("class");
		static final String testMethodUuid = namedId("test");
		static final List<String> stepUuidList = Stream.generate(() -> namedId("step")).limit(3).collect(Collectors.toList());
		static final List<Pair<String, String>> testStepUuidOrder = stepUuidList.stream()
				.map(u -> Pair.of(testMethodUuid, u))
				.collect(Collectors.toList());

		static final ThreadLocal<ReportPortalClient> client = new ThreadLocal<>();
		static final ThreadLocal<Launch> launch = new ThreadLocal<>();

		public static void init() {
			client.set(mock(ReportPortalClient.class));
			TestUtils.mockLaunch(client.get(), "launchUuid", testClassUuid, testMethodUuid);
			TestUtils.mockNestedSteps(client.get(), testStepUuidOrder);
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
			results.add(testExecutionResult);
		}
	}

	@Test
	public void verify_failed_nested_step_not_fails_test_run() {
		TestExtension.init();
		Listener listener = new Listener();
		runClasses(listener, ManualStepReporterFeatureTest.class);

		ReportPortalClient client = TestExtension.client.get();
		ArgumentCaptor<FinishTestItemRQ> finishNestedStep = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000)).finishTestItem(eq(TestExtension.stepUuidList.get(2)), finishNestedStep.capture());

		ArgumentCaptor<FinishTestItemRQ> finishTestStep = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(1000)).finishTestItem(eq(TestExtension.testMethodUuid), finishTestStep.capture());

		assertThat(finishNestedStep.getValue().getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishTestStep.getValue().getStatus(), equalTo(ItemStatus.FAILED.name()));

		assertThat(listener.results.remove().getStatus(), sameInstance(TestExecutionResult.Status.SUCCESSFUL));
	}

	@Test
	public void verify_listener_finishes_unfinished_step() {
		TestExtension.init();
		runClasses(ManualStepReporterSimpleTest.class);

		ReportPortalClient client = TestExtension.client.get();
		verify(client, timeout(1000)).finishTestItem(eq(TestExtension.stepUuidList.get(0)), any());
	}

}
