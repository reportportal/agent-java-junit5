
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

import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import org.slf4j.LoggerFactory;
import rp.com.google.common.collect.Sets;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 * Util class that retrieves useful info about agent environment and composes it in Report Portal system attributes
 */
public class SystemAttributesFetcher {

	private static final String SEPARATOR = " ";
	private static final String SKIPPED_ISSUE_KEY = "skippedIssue";
	private static final String JVM_KEY = "jvm";
	private static final String OS_KEY = "os";
	private static final String AGENT_KEY = "agent";

	private static ItemAttributesRQ jvmInfo() {
		String value = System.getProperty("java.vm.name") + SEPARATOR + System.getProperty("java.vm.version");
		return new ItemAttributesRQ(JVM_KEY, value, true);
	}

	private static ItemAttributesRQ osInfo() {
		String value = System.getProperty("os.name") + SEPARATOR + System.getProperty("os.arch") + SEPARATOR + System.getProperty("os.version");
		return new ItemAttributesRQ(OS_KEY, value, true);
	}

	private static ItemAttributesRQ agentInfo() {
		String agent = null;
		try (InputStream inputStream = ClassLoader.getSystemResourceAsStream("agent.properties")) {
			Properties properties = new Properties();
			properties.load(Objects.requireNonNull(inputStream));
			agent = properties.getProperty(AGENT_KEY);
		} catch (IOException ex) {
			LoggerFactory.getLogger(SystemAttributesFetcher.class).warn("Cannot load agent properties", ex);
		}
		return null == agent ? new ItemAttributesRQ(AGENT_KEY, "undefined", true) : new ItemAttributesRQ(AGENT_KEY, agent, true);
	}

	private static ItemAttributesRQ skippedAnIssue(Boolean fromParams) {
		ItemAttributesRQ skippedIssueAttr = new ItemAttributesRQ();
		skippedIssueAttr.setKey(SKIPPED_ISSUE_KEY);
		skippedIssueAttr.setValue(fromParams == null ? "true" : fromParams.toString());
		skippedIssueAttr.setSystem(true);
		return skippedIssueAttr;
	}

	static Set<ItemAttributesRQ> collectSystemAttributes(Boolean skippedAnIssue) {
		return Sets.newHashSet(jvmInfo(), osInfo(), agentInfo(), skippedAnIssue(skippedAnIssue));
	}

}
