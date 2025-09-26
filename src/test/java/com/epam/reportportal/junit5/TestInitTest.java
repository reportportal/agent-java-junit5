/*
 * Copyright 2023 EPAM Systems
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

import com.epam.reportportal.junit5.features.beforeafterall.FailedConstructorTest;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.step.StepReporter;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.epam.reportportal.junit5.TestInitTest.TestInitTestExtension.LAUNCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestInitTest {
	public static class TestInitTestExtension extends ReportPortalExtension {
		static Launch LAUNCH;

		@Override
		protected Launch getLaunch(ExtensionContext context) {
			return LAUNCH;
		}
	}

	private Map<StartTestItemRQ, Maybe<String>> itemStartMap;
	private Map<Maybe<String>, FinishTestItemRQ> itemFinishMap;

	@BeforeEach
	public void setupMock() {
		itemStartMap = new ConcurrentHashMap<>();
		itemFinishMap = new ConcurrentHashMap<>();
		LAUNCH = mock(Launch.class);
		when(LAUNCH.getStepReporter()).thenReturn(StepReporter.NOOP_STEP_REPORTER);
		when(LAUNCH.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> CommonUtils.createMaybeUuid());
		when(LAUNCH.startTestItem(any(), any())).thenAnswer((Answer<Maybe<String>>) invocation -> {
			Maybe<String> uuidMaybe = CommonUtils.createMaybeUuid();
			StartTestItemRQ start = invocation.getArgument(1);
			itemStartMap.put(start, uuidMaybe);
			return uuidMaybe;
		});
		when(LAUNCH.finishTestItem(any(), any())).thenAnswer((Answer<Maybe<OperationCompletionRS>>) invocation -> {
			Maybe<String> uuidMaybe = invocation.getArgument(0);
			FinishTestItemRQ finish = invocation.getArgument(1);
			itemFinishMap.put(uuidMaybe, finish);
			return Maybe.just(new OperationCompletionRS());
		});
	}

	@Test
	public void verify_a_test_with_failed_init_in_constructor_reported() {
		TestUtils.runClasses(FailedConstructorTest.class);

		verify(LAUNCH, times(1)).startTestItem(any()); // Start parent Suite
		ArgumentCaptor<StartTestItemRQ> captorStart = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(LAUNCH, times(2)).startTestItem(notNull(), captorStart.capture()); // Start a suite and two methods

		ArgumentCaptor<FinishTestItemRQ> captorFinish = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(LAUNCH, times(3)).finishTestItem(notNull(), captorFinish.capture()); // finish tests and suites

		List<StartTestItemRQ> beforeAll = captorStart.getAllValues()
				.stream()
				.filter(e -> e.getType().equals(ItemType.BEFORE_CLASS.name()))
				.collect(Collectors.toList());

		List<StartTestItemRQ> beforeEach = captorStart.getAllValues()
				.stream()
				.filter(e -> e.getType().equals(ItemType.BEFORE_METHOD.name()))
				.collect(Collectors.toList());

		List<StartTestItemRQ> testSteps = captorStart.getAllValues()
				.stream()
				.filter(e -> e.getType().equals(ItemType.STEP.name()))
				.collect(Collectors.toList());

		assertThat("There are one @Test methods", testSteps, hasSize(1));
		assertThat("There are no @BeforeEach methods", beforeEach, hasSize(0));
		assertThat("There are one @BeforeAll method", beforeAll, hasSize(1));

		FinishTestItemRQ finishTest = itemFinishMap.get(itemStartMap.get(testSteps.get(0)));
		assertThat(finishTest.getStatus(), equalTo(ItemStatus.FAILED.name()));

		FinishTestItemRQ finishBeforeAll = itemFinishMap.get(itemStartMap.get(beforeAll.get(0)));
		assertThat(finishBeforeAll.getStatus(), equalTo(ItemStatus.PASSED.name()));
	}
}
