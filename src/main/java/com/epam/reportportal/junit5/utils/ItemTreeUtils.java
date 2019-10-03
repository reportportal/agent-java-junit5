package com.epam.reportportal.junit5.utils;

import com.epam.reportportal.service.tree.TestItemTree;
import io.reactivex.annotations.Nullable;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
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
		return TestItemTree.ItemTreeKey.of(getTestItemName(extensionContext));
	}

	public static TestItemTree.ItemTreeKey createItemTreeKey(ExtensionContext extensionContext, int hash) {
		return TestItemTree.ItemTreeKey.of(getTestItemName(extensionContext), hash);
	}

	@Nullable
	public static TestItemTree.TestItemLeaf retrieveLeaf(String name, TestItemTree testItemTree) {
		return testItemTree.getTestItems().get(createItemTreeKey(name));
	}

	@Nullable
	public static TestItemTree.TestItemLeaf retrieveLeaf(TestInfo testInfo, TestItemTree testItemTree) {
		return retrieveLeaf(testInfo.getDisplayName(), testItemTree);
	}

	private static String getTestItemName(ExtensionContext context) {
		String name = context.getDisplayName();
		return name.length() > 1024 ? name.substring(0, 1024) + "..." : name;
	}
}
