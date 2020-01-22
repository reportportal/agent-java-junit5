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

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.extension.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Optional.ofNullable;
import static rp.com.google.common.base.Throwables.getStackTraceAsString;

/*
 * ReportPortal Extension sends the results of test execution to ReportPortal in RealTime
 */

public class ReportPortalExtension
		implements Extension, BeforeAllCallback, BeforeEachCallback, BeforeTestExecutionCallback, AfterTestExecutionCallback,
				   AfterEachCallback, AfterAllCallback, TestWatcher, InvocationInterceptor {

	private static final String TEST_TEMPLATE_EXTENSION_CONTEXT = "org.junit.jupiter.engine.descriptor.TestTemplateExtensionContext";
	private static final ConcurrentMap<String, Launch> launchMap = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, Maybe<String>> idMapping = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, Maybe<String>> testTemplates = new ConcurrentHashMap<>();
	private ThreadLocal<Boolean> isDisabledTest = new ThreadLocal<>();

	private static Launch getLaunch(ExtensionContext context) {
		return launchMap.computeIfAbsent(context.getRoot().getUniqueId(), id -> {
			ReportPortal rp = ReportPortal.builder().build();
			ListenerParameters params = rp.getParameters();
			StartLaunchRQ rq = new StartLaunchRQ();
			rq.setMode(params.getLaunchRunningMode());
			rq.setDescription(params.getDescription());
			rq.setName(params.getLaunchName());
			rq.setTags(params.getTags());
			rq.setStartTime(Calendar.getInstance().getTime());
			Launch launch = rp.newLaunch(rq);
			Runtime.getRuntime().addShutdownHook(getShutdownHook(launch));
			launch.start();
			return launch;
		});
	}

	@Override
	public void interceptBeforeAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		String parentId = extensionContext.getUniqueId();
		String uniqueId = startBeforeAfter(invocationContext.getExecutable(), extensionContext, parentId, "BEFORE_CLASS");
		finishBeforeAfter(invocation, extensionContext, uniqueId);
	}

	@Override
	public void interceptBeforeEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		String parentId = extensionContext.getParent().get().getUniqueId();
		String uniqueId = startBeforeAfter(invocationContext.getExecutable(), extensionContext, parentId, "BEFORE_METHOD");
		finishBeforeAfter(invocation, extensionContext, uniqueId);
	}

	@Override
	public void interceptAfterAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		String parentId = extensionContext.getUniqueId();
		String uniqueId = startBeforeAfter(invocationContext.getExecutable(), extensionContext, parentId, "AFTER_CLASS");
		finishBeforeAfter(invocation, extensionContext, uniqueId);
	}

	@Override
	public void interceptAfterEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		String parentId = extensionContext.getParent().get().getUniqueId();
		String uniqueId = startBeforeAfter(invocationContext.getExecutable(), extensionContext, parentId, "AFTER_METHOD");
		finishBeforeAfter(invocation, extensionContext, uniqueId);
	}

	@Override
	public void beforeAll(ExtensionContext context) {
		isDisabledTest.set(false);
		startTestItem(context, "SUITE");
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		isDisabledTest.set(false);
		startTemplate(context);
	}

	@Override
	public void beforeTestExecution(ExtensionContext context) {
		startTestItem(context, "STEP");
	}

	@Override
	public void afterTestExecution(ExtensionContext context) {
		finishTestItem(context);
	}

	@Override
	public void afterEach(ExtensionContext context) {
	}

	@Override
	public void afterAll(ExtensionContext context) {
		finishTestTemplates(context);
		finishTestItem(context);
	}

	@Override
	public void testDisabled(ExtensionContext context, Optional<String> reason) {
		if (Boolean.parseBoolean(System.getProperty("reportDisabledTests"))) {
			isDisabledTest.set(true);
			String description = reason.orElse(context.getDisplayName());
			startTestItem(context, "STEP", description);
			finishTestItem(context);
		}
	}

	@Override
	public void testSuccessful(ExtensionContext context) {
	}

	@Override
	public void testAborted(ExtensionContext context, Throwable throwable) {
	}

	@Override
	public void testFailed(ExtensionContext context, Throwable throwable) {
	}

	private synchronized String startBeforeAfter(Method method, ExtensionContext context, String parentId, String itemType) {
		Launch launch = getLaunch(context);
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setName(method.getName() + "()");
		rq.setDescription(method.getName());
		String uniqueId = parentId + "/[method:" + method.getName() + "()]";
		rq.setUniqueId(uniqueId);
		ofNullable(context.getTags()).ifPresent(rq::setTags);
		rq.setType(itemType);
		rq.setRetry(false);
		Maybe<String> itemId = launch.startTestItem(idMapping.get(parentId), rq);
		idMapping.put(uniqueId, itemId);
		return uniqueId;
	}

	private synchronized void finishBeforeAfter(Invocation<Void> invocation, ExtensionContext context, String uniqueId) throws Throwable {
		try {
			invocation.proceed();
			finishBeforeAfter(context, uniqueId, "PASSED");
		} catch (Throwable throwable) {
			sendStackTraceToRP(throwable);
			finishBeforeAfter(context, uniqueId, "FAILED");
			throw throwable;
		}
	}

	private synchronized void finishBeforeAfter(ExtensionContext context, String uniqueId, String status) {
		Launch launch = getLaunch(context);
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setStatus(status);
		rq.setEndTime(Calendar.getInstance().getTime());
		launch.finishTestItem(idMapping.get(uniqueId), rq);
	}

	private synchronized void startTemplate(ExtensionContext context) {
		Optional<ExtensionContext> parent = context.getParent();
		if ((parent.isPresent() && TEST_TEMPLATE_EXTENSION_CONTEXT.equals(parent.get().getClass().getName()))) {
			if (!idMapping.containsKey(parent.get().getUniqueId())) {
				startTestItem(context.getParent().get(), "TEMPLATE");
			}
		}
	}

	private synchronized void startTestItem(ExtensionContext context, String type) {
		startTestItem(context, type, null);
	}

	private synchronized void startTestItem(ExtensionContext context, String type, String reason) {
		boolean isTemplate = false;
		if ("TEMPLATE".equals(type)) {
			type = "SUITE";
			isTemplate = true;
		}
		TestItem testItem = getTestItem(context);
		Launch launch = getLaunch(context);
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setName(testItem.getName());
		rq.setDescription(null != reason ? reason : testItem.getDescription());
		rq.setUniqueId(context.getUniqueId());
		rq.setType(type);
		rq.setRetry(false);
		ofNullable(testItem.getTags()).ifPresent(rq::setTags);

		Maybe<String> itemId = context.getParent()
				.map(ExtensionContext::getUniqueId)
				.map(parentId -> ofNullable(idMapping.get(parentId)))
				.map(parentTest -> launch.startTestItem(parentTest.orElse(null), rq))
				.orElseGet(() -> launch.startTestItem(rq));
		if (isTemplate) {
			testTemplates.put(context.getUniqueId(), itemId);
		}
		idMapping.put(context.getUniqueId(), itemId);
	}

	private synchronized void finishTestTemplates(ExtensionContext context) {
		getTestTemplateIds().forEach(id -> {
			Launch launch = getLaunch(context);
			FinishTestItemRQ rq = new FinishTestItemRQ();
			rq.setStatus(isDisabledTest.get() ? "SKIPPED" : getExecutionStatus(context));
			rq.setEndTime(Calendar.getInstance().getTime());
			launch.finishTestItem(idMapping.get(id), rq);
			testTemplates.entrySet().removeIf(e -> e.getKey().equals(id));
		});
	}

	private synchronized List<String> getTestTemplateIds() {
		List<String> keys = new ArrayList<>();
		for (Map.Entry<String, Maybe<String>> e : testTemplates.entrySet()) {
			if (e.getKey().contains("/[test-template:") && !e.getKey().contains("-invocation")) {
				keys.add(e.getKey());
			}
		}
		return keys;
	}

	private synchronized void finishTestItem(ExtensionContext context) {
		Launch launch = getLaunch(context);
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setStatus(isDisabledTest.get() ? "SKIPPED" : getExecutionStatus(context));
		rq.setEndTime(Calendar.getInstance().getTime());
		launch.finishTestItem(idMapping.get(context.getUniqueId()), rq);
	}

	private static synchronized String getExecutionStatus(ExtensionContext context) {
		Optional<Throwable> exception = context.getExecutionException();
		if (!exception.isPresent()) {
			return Statuses.PASSED;
		} else {
			sendStackTraceToRP(exception.get());
			return Statuses.FAILED;
		}
	}

	private static Thread getShutdownHook(final Launch launch) {
		return new Thread(() -> {
			FinishExecutionRQ rq = new FinishExecutionRQ();
			rq.setEndTime(Calendar.getInstance().getTime());
			launch.finish(rq);
		});
	}

	private static synchronized void sendStackTraceToRP(final Throwable cause) {
		ReportPortal.emitLog(itemId -> {
			SaveLogRQ rq = new SaveLogRQ();
			rq.setTestItemId(itemId);
			rq.setLevel("ERROR");
			rq.setLogTime(Calendar.getInstance().getTime());
			if (cause != null) {
				rq.setMessage(getStackTraceAsString(cause));
			} else {
				rq.setMessage("Test has failed without exception");
			}
			rq.setLogTime(Calendar.getInstance().getTime());
			return rq;
		});
	}

	protected static class TestItem {

		private String name;
		private String description;
		private Set<String> tags;

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}

		public Set<String> getTags() {
			return tags;
		}

		public TestItem(String name, String description, Set<String> tags) {
			this.name = name;
			this.description = description;
			this.tags = tags;
		}
	}

	protected TestItem getTestItem(ExtensionContext context) {
		String name = context.getDisplayName();
		name = name.length() > 256 ? name.substring(0, 200) + "..." : name;
		String description = context.getDisplayName();
		Set<String> tags = context.getTags();
		return new TestItem(name, description, tags);
	}
}