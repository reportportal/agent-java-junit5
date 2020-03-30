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

import com.epam.reportportal.junit5.features.testcaseid.TestCaseIdFromAnnotationTest;
import com.epam.reportportal.junit5.features.testcaseid.TestCaseIdFromCodeRefAndParamsTest;
import com.epam.reportportal.junit5.features.testcaseid.TestCaseIdFromCodeRefTest;
import com.epam.reportportal.junit5.features.testcaseid.TestCaseIdFromParametrizedAnnotationTest;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import rp.com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestCaseIdTest {

	@BeforeEach
	public void setupMock() {
		TestCaseIdExtension.LAUNCH = mock(Launch.class);
		when(TestCaseIdExtension.LAUNCH.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> TestUtils.createMaybeUuid());
		when(TestCaseIdExtension.LAUNCH.startTestItem(any(),
				any()
		)).thenAnswer((Answer<Maybe<String>>) invocation -> TestUtils.createMaybeUuid());
	}

	@Test
	void testCaseIdFromCodeRefTest() {
		TestUtils.runClasses(TestCaseIdFromCodeRefTest.class);
		String expected = "com.epam.reportportal.junit5.features.testcaseid.TestCaseIdFromCodeRefTest.test";

		Launch launch = TestCaseIdExtension.LAUNCH;

		verify(launch, times(1)).startTestItem(any()); // Start parent Suite

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(1)).startTestItem(notNull(), captor.capture()); // Start a test

		StartTestItemRQ request = captor.getValue();
		assertEquals(expected, request.getTestCaseId());
	}

	@Test
	void testCaseIdFromCodeRefAndParamsTest() {
		TestUtils.runClasses(TestCaseIdFromCodeRefAndParamsTest.class);

		String expectedCodeRef = "com.epam.reportportal.junit5.features.testcaseid.TestCaseIdFromCodeRefAndParamsTest.parametrized";
		List<String> expected = IntStream.of(101, 0).mapToObj(it -> expectedCodeRef + "[" + it + "]").collect(Collectors.toList());

		Launch launch = TestCaseIdExtension.LAUNCH;

		verify(launch, times(1)).startTestItem(any()); // Start parent Suite

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(3)).startTestItem(notNull(), captor.capture()); // Start a test

		List<StartTestItemRQ> requests = captor.getAllValues();

		assertTrue(requests.stream().anyMatch(it -> "suite".equalsIgnoreCase(it.getType())));
		List<String> actual = requests.stream()
				.filter(it -> "step".equalsIgnoreCase(it.getType()))
				.map(StartTestItemRQ::getTestCaseId)
				.collect(Collectors.toList());
		assertEquals(expected.size(), actual.size());
		assertEquals(expected, actual);
	}

	@Test
	void testCaseIdFromAnnotationTest() {
		TestUtils.runClasses(TestCaseIdFromAnnotationTest.class);

		Launch launch = TestCaseIdExtension.LAUNCH;

		verify(launch, times(1)).startTestItem(any()); // Start parent Suite

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(1)).startTestItem(notNull(), captor.capture()); // Start a test

		StartTestItemRQ request = captor.getValue();
		assertEquals(TestCaseIdFromAnnotationTest.TEST_CASE_ID_VALUE, request.getTestCaseId());
	}

	@Test
	void testCaseIdFromParametrizedTestWithAnnotationTest() {
		TestUtils.runClasses(TestCaseIdFromParametrizedAnnotationTest.class);

		ArrayList<String> expected = Lists.newArrayList("[one]", "[two]");

		Launch launch = TestCaseIdExtension.LAUNCH;
		verify(launch, times(1)).startTestItem(any()); // Start parent Suite

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(3)).startTestItem(notNull(), captor.capture()); // Start a test

		List<StartTestItemRQ> requests = captor.getAllValues();

		assertTrue(requests.stream().anyMatch(it -> "suite".equalsIgnoreCase(it.getType())));
		List<String> actual = requests.stream()
				.filter(it -> "step".equalsIgnoreCase(it.getType()))
				.map(StartTestItemRQ::getTestCaseId)
				.collect(Collectors.toList());
		assertEquals(expected.size(), actual.size());
		assertEquals(expected, actual);
	}

	public static class TestCaseIdExtension extends ReportPortalExtension {
		static Launch LAUNCH;

		@Override
		protected Launch getLaunch(ExtensionContext context) {
			return LAUNCH;
		}
	}
}
