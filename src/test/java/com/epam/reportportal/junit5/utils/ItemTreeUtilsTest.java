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

package com.epam.reportportal.junit5.utils;

import com.epam.reportportal.service.tree.TestItemTree;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
class ItemTreeUtilsTest {

	private static final String NAME = "Simple name";
	private static final int ANOTHER_HASH = NAME.hashCode() - 1;

	@Mock
	private TestInfo testInfo;

	@Mock
	private ExtensionContext extensionContext;

	private TestItemTree testItemTree;

	@BeforeEach
	void init() {
		MockitoAnnotations.initMocks(this);
		when(testInfo.getDisplayName()).thenReturn(NAME);
		when(extensionContext.getDisplayName()).thenReturn(NAME);

		testItemTree = new TestItemTree();
	}

	@Test
	void createKeyFromString() {
		TestItemTree.ItemTreeKey itemTreeKey = ItemTreeUtils.createItemTreeKey(NAME);

		assertThat(itemTreeKey.getName(), equalTo(NAME));
		assertThat(itemTreeKey.getHash(), equalTo(NAME.hashCode()));
	}

	@Test
	void createKeyFromStringAndHash() {
		TestItemTree.ItemTreeKey itemTreeKey = ItemTreeUtils.createItemTreeKey(NAME, ANOTHER_HASH);

		assertThat(itemTreeKey.getName(), equalTo(NAME));
		assertThat(itemTreeKey.getHash(), equalTo(ANOTHER_HASH));
	}

	@Test
	void createKeyFromTestInfo() {
		TestItemTree.ItemTreeKey itemTreeKey = ItemTreeUtils.createItemTreeKey(testInfo);

		assertThat(itemTreeKey.getName(), equalTo(NAME));
		assertThat(itemTreeKey.getHash(), equalTo(NAME.hashCode()));
	}

	@Test
	void createKeyFromTestInfoAndHash() {
		TestItemTree.ItemTreeKey itemTreeKey = ItemTreeUtils.createItemTreeKey(testInfo, ANOTHER_HASH);

		assertThat(itemTreeKey.getName(), equalTo(NAME));
		assertThat(itemTreeKey.getHash(), equalTo(ANOTHER_HASH));
	}

	@Test
	void createKeyFromExtensionContext() {
		TestItemTree.ItemTreeKey itemTreeKey = ItemTreeUtils.createItemTreeKey(extensionContext);

		assertThat(itemTreeKey.getName(), equalTo(NAME));
		assertThat(itemTreeKey.getHash(), equalTo(NAME.hashCode()));
	}

	@Test
	void createKeyFromExtensionContextAndHash() {
		TestItemTree.ItemTreeKey itemTreeKey = ItemTreeUtils.createItemTreeKey(extensionContext, ANOTHER_HASH);

		assertThat(itemTreeKey.getName(), equalTo(NAME));
		assertThat(itemTreeKey.getHash(), equalTo(ANOTHER_HASH));
	}

	@Test
	void retrieveLeafByName() {

		TestItemTree.ItemTreeKey treeKey = TestItemTree.ItemTreeKey.of(NAME);
		Maybe<String> itemId = createIdMaybe("first");
		TestItemTree.TestItemLeaf itemLeaf = TestItemTree.createTestItemLeaf(itemId);

		testItemTree.getTestItems().put(treeKey, itemLeaf);

		TestItemTree.TestItemLeaf retrievedLeaf = ItemTreeUtils.retrieveLeaf(NAME, testItemTree);

		assertThat(retrievedLeaf.getItemId(), notNullValue());
		assertThat(retrievedLeaf.getItemId(), equalTo(itemId));
	}

	@Test
	void retrieveLeafByTestInfo() {
		TestItemTree.ItemTreeKey treeKey = TestItemTree.ItemTreeKey.of(NAME);
		Maybe<String> itemId = createIdMaybe("first");
		TestItemTree.TestItemLeaf itemLeaf = TestItemTree.createTestItemLeaf(itemId);

		testItemTree.getTestItems().put(treeKey, itemLeaf);

		TestItemTree.TestItemLeaf retrievedLeaf = ItemTreeUtils.retrieveLeaf(testInfo, testItemTree);

		assertThat(retrievedLeaf.getItemId(), notNullValue());
		assertThat(retrievedLeaf.getItemId(), equalTo(itemId));
	}

	public static Maybe<String> createIdMaybe(String id) {
		return Maybe.create(emitter -> {
			emitter.onSuccess(id);
			emitter.onComplete();
		});
	}
}