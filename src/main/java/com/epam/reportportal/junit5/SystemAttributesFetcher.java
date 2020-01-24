package com.epam.reportportal.junit5;

import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import org.slf4j.LoggerFactory;
import rp.com.google.common.collect.Sets;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

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
		String value =
				System.getProperty("os.name") + SEPARATOR + System.getProperty("os.arch") + SEPARATOR + System.getProperty("os.version");
		return new ItemAttributesRQ(OS_KEY, value, true);
	}

	private static ItemAttributesRQ agentInfo() {
		String agent = null;
		try (InputStream inputStream = new FileInputStream("agent.properties")) {
			Properties properties = new Properties();
			properties.load(inputStream);
			agent = properties.getProperty("agent");
		} catch (IOException ex) {
			LoggerFactory.getLogger(SystemAttributesFetcher.class).warn("Cannot load agent properties", ex);
		}
		return null == agent ? null : new ItemAttributesRQ(AGENT_KEY, agent, true);
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
