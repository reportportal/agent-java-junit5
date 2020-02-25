
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

import com.epam.reportportal.utils.properties.SystemAttributesExtractor;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;

import java.util.Set;

/**
 * Util class that retrieves useful info about agent environment and composes it in Report Portal system attributes
 */
public class SystemAttributesFetcher {

	private static final String SKIPPED_ISSUE_KEY = "skippedIssue";
	private static final String AGENT_PROPERTIES = "agent.properties";

	private static ItemAttributesRQ skippedAnIssue(Boolean fromParams) {
		ItemAttributesRQ skippedIssueAttr = new ItemAttributesRQ();
		skippedIssueAttr.setKey(SKIPPED_ISSUE_KEY);
		skippedIssueAttr.setValue(fromParams == null ? "true" : fromParams.toString());
		skippedIssueAttr.setSystem(true);
		return skippedIssueAttr;
	}

	static Set<ItemAttributesRQ> collectSystemAttributes(Boolean skippedAnIssue) {

		Set<ItemAttributesRQ> systemAttributes = SystemAttributesExtractor.extract(AGENT_PROPERTIES, SystemAttributesFetcher.class.getClassLoader());
		systemAttributes.add(skippedAnIssue(skippedAnIssue));
		return systemAttributes;
	}

}
