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

package com.epam.reportportal.junit5.features.testcaseid;

import com.epam.reportportal.junit5.TestCaseIdTest;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
@ExtendWith(TestCaseIdTest.TestCaseIdExtension.class)
public class SingleDynamicTest {
	public static final String TEST_CASE_DISPLAY_NAME = UUID.randomUUID().toString();

	@TestFactory
	Stream<DynamicTest> testForTestFactory() {
		return Stream.of(dynamicTest(TEST_CASE_DISPLAY_NAME, () -> System.out.println("Test factory test: " + TEST_CASE_DISPLAY_NAME)));
	}

}
