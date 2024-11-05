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

package com.epam.reportportal.junit5.features.issue;

import com.epam.reportportal.annotations.Issue;
import com.epam.reportportal.annotations.TestFilter;
import com.epam.reportportal.annotations.TestParamFilter;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(com.epam.reportportal.junit5.ReportPortalExtension.class)
public class ParameterizedWithTwoIssueTest {

	public static final String FAILURE_MESSAGE = "This parameterized test is expected to fail: ";
	public static final String ISSUE_MESSAGE = "This test is expected to fail";

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	@Issue(value = "ab001", comment = ISSUE_MESSAGE, filter = @TestFilter(param = { @TestParamFilter(valueStartsWith = "true") }))
	@Issue(value = "pb001", comment = ISSUE_MESSAGE, filter = @TestFilter(param = { @TestParamFilter(valueStartsWith = "false") }))
	public void failureTest(boolean param) {
		throw new IllegalStateException(FAILURE_MESSAGE + param);
	}
}
