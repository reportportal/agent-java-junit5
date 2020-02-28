/*
 * Copyright 2020 EPAM Systems
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

package com.epam.reportportal.junit5.features.callback;

import com.epam.reportportal.junit5.CallbackReportingTest;
import com.epam.reportportal.junit5.utils.ItemTreeUtils;
import com.epam.reportportal.service.tree.ItemTreeReporter;
import com.epam.reportportal.service.tree.TestItemTree;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.epam.reportportal.junit5.CallbackReportingTest.*;
import static com.epam.reportportal.junit5.ReportPortalExtension.REPORT_PORTAL;
import static com.epam.reportportal.junit5.ReportPortalExtension.TEST_ITEM_TREE;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@ExtendWith(CallbackReportingTest.CallbackReportingExtension.class)
public class CallbackLogFeatureTest {

	@Test
	void someTest() {
		Assertions.assertEquals(1, 1);
	}

	@AfterEach
	void afterMethod(TestInfo testInfo) {
		TestItemTree.TestItemLeaf testItemLeaf = ItemTreeUtils.retrieveLeaf(testInfo, TEST_ITEM_TREE);
		if (testItemLeaf != null) {
			attachLog(testItemLeaf);
		}
	}

	private static void attachLog(TestItemTree.TestItemLeaf testItemLeaf) {
		ItemTreeReporter.sendLog(REPORT_PORTAL.getClient(),
				ERROR_LOG_LEVEL,
				LOG_MESSAGE,
				LOG_TIME,
				TEST_ITEM_TREE.getLaunchId(),
				testItemLeaf
		);
	}

}
