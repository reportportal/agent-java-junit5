/*
 * Copyright 2024 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.junit5;

import com.epam.reportportal.junit5.features.issue.SimpleIssueTest;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IssueReportingTest {
	public static class TestExtension extends ReportPortalExtension {
		static Launch LAUNCH;

		@Override
		protected Launch getLaunch(ExtensionContext context) {
			return LAUNCH;
		}
	}

	private final String SUITE_ID = CommonUtils.namedId("suite_");
	private final Maybe<String> SUITE_MAYBE = Maybe.just(SUITE_ID);
	private final String STEP_ID = CommonUtils.namedId("step_");
	private final Maybe<String> STEP_MAYBE = Maybe.just(STEP_ID);

	@BeforeEach
	public void setupMock() {
		Launch launch = mock(Launch.class);
		IssueReportingTest.TestExtension.LAUNCH = launch;
		when(launch.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> SUITE_MAYBE);
		when(launch.startTestItem(same(SUITE_MAYBE), any())).thenAnswer((Answer<Maybe<String>>) invocation -> STEP_MAYBE);
		when(launch.finishTestItem(any(),
				any()
		)).thenAnswer((Answer<Maybe<OperationCompletionRS>>) invocation -> Maybe.just(new OperationCompletionRS("OK")));
	}

	@Test
	public void verify_simple_test_failure() {
		TestUtils.runClasses(SimpleIssueTest.class);

		Launch launch = IssueReportingTest.TestExtension.LAUNCH;
		ArgumentCaptor<FinishTestItemRQ> testCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch).finishTestItem(same(STEP_MAYBE), testCaptor.capture());

		FinishTestItemRQ finishTestItemRQ = testCaptor.getValue();
		assertThat(finishTestItemRQ.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishTestItemRQ.getIssue(), notNullValue());
		Issue issue = finishTestItemRQ.getIssue();
		assertThat(issue.getIssueType(), equalTo("pb001"));
		assertThat(issue.getComment(), equalTo(SimpleIssueTest.FAILURE_MESSAGE));
	}
}
