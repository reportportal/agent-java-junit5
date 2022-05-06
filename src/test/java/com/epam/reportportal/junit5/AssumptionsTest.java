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

import com.epam.reportportal.junit5.features.coderef.SingleTest;
import com.epam.reportportal.junit5.features.skipped.AssumptionFailedTest;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.step.StepReporter;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AssumptionsTest {

	public static class AssumptionsTestExtension extends ReportPortalExtension {
		static Launch LAUNCH;

		@Override
		protected Launch getLaunch(ExtensionContext context) {
			return LAUNCH;
		}
	}

	@BeforeEach
	public void setupMock() {
		AssumptionsTestExtension.LAUNCH = mock(Launch.class);
		Launch launch = AssumptionsTestExtension.LAUNCH;
		when(launch.getStepReporter()).thenReturn(StepReporter.NOOP_STEP_REPORTER);
		when(launch.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> CommonUtils.createMaybeUuid());
		when(launch.startTestItem(any(), any())).thenAnswer((Answer<Maybe<String>>) invocation -> CommonUtils.createMaybeUuid());
	}

	@Test
	public void verify_assumption_failure_marks_test_as_skipped() {
		TestUtils.runClasses(AssumptionFailedTest.class);

		Launch launch = AssumptionsTestExtension.LAUNCH;

		verify(launch).startTestItem(any(StartTestItemRQ.class)); // Start parent Suite
		verify(launch, times(1)).startTestItem(notNull(), any(StartTestItemRQ.class)); // Start a test

		ArgumentCaptor<FinishTestItemRQ> finishCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch, times(2)).finishTestItem(any(), finishCaptor.capture());
		List<FinishTestItemRQ> finishItems = finishCaptor.getAllValues();
		assertThat(finishItems.get(0).getStatus(), equalTo(ItemStatus.SKIPPED.name()));
		assertThat(finishItems.get(1).getStatus(), nullValue());
	}
}
