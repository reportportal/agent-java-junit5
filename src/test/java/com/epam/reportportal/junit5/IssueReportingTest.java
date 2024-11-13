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

import com.epam.reportportal.junit5.features.issue.*;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class IssueReportingTest {
	public static class TestExtension extends ReportPortalExtension {
		static Launch LAUNCH;

		@Override
		protected Launch getLaunch(ExtensionContext context) {
			return LAUNCH;
		}
	}

	private final String suiteId = CommonUtils.namedId("suite_");
	private final Maybe<String> suiteMaybe = Maybe.just(suiteId);
	private final String stepOneId = CommonUtils.namedId("step_");
	private final Maybe<String> stepOneMaybe = Maybe.just(stepOneId);
	private final String stepTwoId = CommonUtils.namedId("step_");
	private final Maybe<String> stepTwoMaybe = Maybe.just(stepTwoId);
	private final String stepThreeId = CommonUtils.namedId("step_");
	private final Maybe<String> stepThreeMaybe = Maybe.just(stepThreeId);
	private final Queue<Maybe<String>> stepIds = new LinkedList<>(Arrays.asList(stepOneMaybe, stepTwoMaybe, stepThreeMaybe));

	@BeforeAll
	public static void setupProperty() {
		System.setProperty("reportDisabledTests", Boolean.TRUE.toString());
	}

	@BeforeEach
	public void setupMock() {
		Launch launch = mock(Launch.class);
		IssueReportingTest.TestExtension.LAUNCH = launch;
		when(launch.startTestItem(any())).thenReturn(suiteMaybe);
		when(launch.startTestItem(any(), any())).thenAnswer((Answer<Maybe<String>>) invocation -> CommonUtils.createMaybeUuid());
		when(launch.startTestItem(same(suiteMaybe), any())).thenAnswer((Answer<Maybe<String>>) invocation -> stepIds.poll());
		when(launch.startTestItem(same(stepOneMaybe), any())).thenAnswer((Answer<Maybe<String>>) invocation -> stepIds.poll());
		when(launch.finishTestItem(any(),
				any()
		)).thenAnswer((Answer<Maybe<OperationCompletionRS>>) invocation -> Maybe.just(new OperationCompletionRS("OK")));
	}

	@Test
	public void verify_simple_test_failure() {
		TestUtils.runClasses(SimpleIssueTest.class);

		Launch launch = IssueReportingTest.TestExtension.LAUNCH;
		ArgumentCaptor<FinishTestItemRQ> testCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch).finishTestItem(same(stepOneMaybe), testCaptor.capture());

		FinishTestItemRQ finishTestItemRQ = testCaptor.getValue();
		assertThat(finishTestItemRQ.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishTestItemRQ.getIssue(), notNullValue());
		Issue issue = finishTestItemRQ.getIssue();
		assertThat(issue.getIssueType(), equalTo("pb001"));
		assertThat(issue.getComment(), equalTo(SimpleIssueTest.FAILURE_MESSAGE));
	}

	@Test
	public void verify_test_failure_with_two_issues() {
		TestUtils.runClasses(SimpleTwoIssuesTest.class);

		Launch launch = IssueReportingTest.TestExtension.LAUNCH;
		ArgumentCaptor<FinishTestItemRQ> testCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch).finishTestItem(same(stepOneMaybe), testCaptor.capture());

		FinishTestItemRQ finishTestItemRQ = testCaptor.getValue();
		assertThat(finishTestItemRQ.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishTestItemRQ.getIssue(), notNullValue());
		Issue issue = finishTestItemRQ.getIssue();
		assertThat(issue.getIssueType(), equalTo("ab001"));
		assertThat(issue.getComment(), equalTo(SimpleTwoIssuesTest.FAILURE_MESSAGE));
	}

	@Test
	public void verify_parameterized_test_failure_with_one_issue() {
		TestUtils.runClasses(ParameterizedWithOneIssueTest.class);

		Launch launch = IssueReportingTest.TestExtension.LAUNCH;
		ArgumentCaptor<FinishTestItemRQ> firstTestCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch).finishTestItem(same(stepTwoMaybe), firstTestCaptor.capture());
		ArgumentCaptor<FinishTestItemRQ> secondTestCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch).finishTestItem(same(stepThreeMaybe), secondTestCaptor.capture());

		FinishTestItemRQ finishTestItemRQ = firstTestCaptor.getValue();
		assertThat(finishTestItemRQ.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishTestItemRQ.getIssue(), nullValue());

		finishTestItemRQ = secondTestCaptor.getValue();
		assertThat(finishTestItemRQ.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishTestItemRQ.getIssue(), notNullValue());
		Issue issue = finishTestItemRQ.getIssue();
		assertThat(issue.getIssueType(), equalTo("ab001"));
		assertThat(issue.getComment(), equalTo(ParameterizedWithOneIssueTest.ISSUE_MESSAGE));
	}

	@Test
	public void verify_parameterized_test_failure_with_two_issues() {
		TestUtils.runClasses(ParameterizedWithTwoIssueTest.class);

		Launch launch = IssueReportingTest.TestExtension.LAUNCH;
		ArgumentCaptor<FinishTestItemRQ> firstTestCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch).finishTestItem(same(stepTwoMaybe), firstTestCaptor.capture());
		ArgumentCaptor<FinishTestItemRQ> secondTestCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch).finishTestItem(same(stepThreeMaybe), secondTestCaptor.capture());

		FinishTestItemRQ finishTestItemRQ = firstTestCaptor.getValue();
		assertThat(finishTestItemRQ.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishTestItemRQ.getIssue(), notNullValue());
		Issue issue = finishTestItemRQ.getIssue();
		assertThat(issue.getIssueType(), equalTo("ab001"));
		assertThat(issue.getComment(), equalTo(ParameterizedWithTwoIssueTest.ISSUE_MESSAGE));

		finishTestItemRQ = secondTestCaptor.getValue();
		assertThat(finishTestItemRQ.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishTestItemRQ.getIssue(), notNullValue());
		issue = finishTestItemRQ.getIssue();
		assertThat(issue.getIssueType(), equalTo("pb001"));
		assertThat(issue.getComment(), equalTo(ParameterizedWithTwoIssueTest.ISSUE_MESSAGE));
	}

	@Test
	public void verify_dynamic_test_failure() {
		TestUtils.runClasses(DynamicIssueTest.class);

		Launch launch = IssueReportingTest.TestExtension.LAUNCH;
		ArgumentCaptor<FinishTestItemRQ> testCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch).finishTestItem(same(stepTwoMaybe), testCaptor.capture());

		FinishTestItemRQ finishTestItemRQ = testCaptor.getValue();
		assertThat(finishTestItemRQ.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishTestItemRQ.getIssue(), notNullValue());
		Issue issue = finishTestItemRQ.getIssue();
		assertThat(issue.getIssueType(), equalTo("ab001"));
		assertThat(issue.getComment(), equalTo(DynamicIssueTest.FAILURE_MESSAGE));
	}

	@Test
	public void verify_two_dynamic_test_failures() {
		TestUtils.runClasses(TwoDynamicIssueTest.class);

		Launch launch = IssueReportingTest.TestExtension.LAUNCH;
		ArgumentCaptor<FinishTestItemRQ> firstTestCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch).finishTestItem(same(stepTwoMaybe), firstTestCaptor.capture());
		ArgumentCaptor<FinishTestItemRQ> secondTestCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch).finishTestItem(same(stepThreeMaybe), secondTestCaptor.capture());

		FinishTestItemRQ finishTestItemRQ = firstTestCaptor.getValue();
		assertThat(finishTestItemRQ.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishTestItemRQ.getIssue(), notNullValue());
		Issue issue = finishTestItemRQ.getIssue();
		assertThat(issue.getIssueType(), equalTo("ab001"));
		assertThat(issue.getComment(), equalTo(TwoDynamicIssueTest.FAILURE_MESSAGE));

		finishTestItemRQ = secondTestCaptor.getValue();
		assertThat(finishTestItemRQ.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishTestItemRQ.getIssue(), notNullValue());
		issue = finishTestItemRQ.getIssue();
		assertThat(issue.getIssueType(), equalTo("ab001"));
		assertThat(issue.getComment(), equalTo(TwoDynamicIssueTest.FAILURE_MESSAGE));
	}

	@Test
	public void verify_two_dynamic_test_failures_two_issues() {
		TestUtils.runClasses(TwoDynamicTwoIssueTest.class);

		Launch launch = IssueReportingTest.TestExtension.LAUNCH;
		ArgumentCaptor<FinishTestItemRQ> firstTestCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch).finishTestItem(same(stepTwoMaybe), firstTestCaptor.capture());
		ArgumentCaptor<FinishTestItemRQ> secondTestCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch).finishTestItem(same(stepThreeMaybe), secondTestCaptor.capture());

		FinishTestItemRQ finishTestItemRQ = firstTestCaptor.getValue();
		assertThat(finishTestItemRQ.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishTestItemRQ.getIssue(), notNullValue());
		Issue issue = finishTestItemRQ.getIssue();
		assertThat(issue.getIssueType(), equalTo("ab001"));
		assertThat(issue.getComment(), equalTo(TwoDynamicTwoIssueTest.FAILURE_MESSAGE));

		finishTestItemRQ = secondTestCaptor.getValue();
		assertThat(finishTestItemRQ.getStatus(), equalTo(ItemStatus.FAILED.name()));
		assertThat(finishTestItemRQ.getIssue(), notNullValue());
		issue = finishTestItemRQ.getIssue();
		assertThat(issue.getIssueType(), equalTo("pb001"));
		assertThat(issue.getComment(), equalTo(TwoDynamicTwoIssueTest.FAILURE_MESSAGE));
	}

	@Test
	public void verify_simple_test_skip() {
		TestUtils.runClasses(SimpleSkippedIssueTest.class);

		Launch launch = IssueReportingTest.TestExtension.LAUNCH;
		ArgumentCaptor<FinishTestItemRQ> testCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(launch).finishTestItem(same(stepOneMaybe), testCaptor.capture());

		FinishTestItemRQ finishTestItemRQ = testCaptor.getValue();
		assertThat(finishTestItemRQ.getStatus(), equalTo(ItemStatus.SKIPPED.name()));
		assertThat(finishTestItemRQ.getIssue(), notNullValue());
		Issue issue = finishTestItemRQ.getIssue();
		assertThat(issue.getIssueType(), equalTo("pb001"));
		assertThat(issue.getComment(), equalTo(SimpleSkippedIssueTest.FAILURE_MESSAGE));
	}
}
