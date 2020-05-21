/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.junit5;

import com.epam.reportportal.junit5.features.retry.RetryThreeTimesTest;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
public class RetryTest {

	@Test
	void verify_three_retry_request_creation() {
		TestUtils.runClasses(RetryThreeTimesTest.class);

		Launch launch = RetryTestExtension.LAUNCH;

		verify(launch, times(1)).startTestItem(any()); // Start parent Suite

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(4)).startTestItem(notNull(), captor.capture()); // Start a test

		List<StartTestItemRQ> retriedSteps = captor.getAllValues()
				.stream()
				.filter(it -> it.getType().equalsIgnoreCase("step"))
				.collect(Collectors.toList());
		assertEquals(3, retriedSteps.size());
		retriedSteps.stream().map(StartTestItemRQ::isRetry).forEach(Assertions::assertTrue);
	}

	public static class RetryTestExtension extends ReportPortalExtension {
		static final Launch LAUNCH;

		static {
			LAUNCH = mock(Launch.class);
			when(LAUNCH.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> TestUtils.createMaybeUuid());
			when(LAUNCH.startTestItem(any(), any())).thenAnswer((Answer<Maybe<String>>) invocation -> TestUtils.createMaybeUuid());
		}

		@Override
		protected Launch getLaunch(ExtensionContext context) {
			return LAUNCH;
		}
	}
}
