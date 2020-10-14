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

package com.epam.reportportal.junit5.utils;

import com.epam.reportportal.service.tree.TestItemTree;
import io.reactivex.annotations.Nullable;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Util class for creating {@link com.epam.reportportal.service.tree.TestItemTree.ItemTreeKey} objects from JUnit5 test-related entities.
 * {@link com.epam.reportportal.service.tree.TestItemTree.ItemTreeKey} is required for accessing {@link TestItemTree} entries which
 * are used in {@link com.epam.reportportal.service.tree.ItemTreeReporter} that provides possibility to send requests to the Report Portal
 * instance right from the end-user code
 *
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class ItemTreeUtils {

	private ItemTreeUtils() {
		//static only
	}

	public static TestItemTree.ItemTreeKey createItemTreeKey(String name) {
		return TestItemTree.ItemTreeKey.of(name);
	}

	public static TestItemTree.ItemTreeKey createItemTreeKey(String name, int hash) {
		return TestItemTree.ItemTreeKey.of(name, hash);
	}

	public static TestItemTree.ItemTreeKey createItemTreeKey(TestInfo testInfo) {
		return TestItemTree.ItemTreeKey.of(testInfo.getDisplayName());
	}

	public static TestItemTree.ItemTreeKey createItemTreeKey(TestInfo testInfo, int hash) {
		return TestItemTree.ItemTreeKey.of(testInfo.getDisplayName(), hash);
	}

	public static TestItemTree.ItemTreeKey createItemTreeKey(ExtensionContext extensionContext) {
		return TestItemTree.ItemTreeKey.of(extensionContext.getDisplayName());
	}

	public static TestItemTree.ItemTreeKey createItemTreeKey(ExtensionContext extensionContext, int hash) {
		return TestItemTree.ItemTreeKey.of(extensionContext.getDisplayName(), hash);
	}

	@Nullable
	public static TestItemTree.TestItemLeaf retrieveLeaf(String name, TestItemTree testItemTree) {
		return testItemTree.getTestItems().get(createItemTreeKey(name));
	}

	@Nullable
	public static TestItemTree.TestItemLeaf retrieveLeaf(TestInfo testInfo, TestItemTree testItemTree) {
		return retrieveLeaf(testInfo.getDisplayName(), testItemTree);
	}
}
