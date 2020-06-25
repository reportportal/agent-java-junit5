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

import com.epam.reportportal.annotations.ParameterKey;
import com.epam.reportportal.annotations.TestCaseId;
import com.epam.reportportal.annotations.attribute.Attributes;
import com.epam.reportportal.aspect.StepAspect;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.LaunchImpl;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.item.TestCaseIdEntry;
import com.epam.reportportal.service.tree.TestItemTree;
import com.epam.reportportal.utils.AttributeParser;
import com.epam.reportportal.utils.TestCaseIdUtils;
import com.epam.ta.reportportal.ws.model.*;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.*;
import rp.com.google.common.collect.Sets;

import javax.validation.constraints.NotNull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.epam.reportportal.junit5.ItemType.*;
import static com.epam.reportportal.junit5.Status.*;
import static com.epam.reportportal.junit5.SystemAttributesFetcher.collectSystemAttributes;
import static com.epam.reportportal.junit5.utils.ItemTreeUtils.createItemTreeKey;
import static com.epam.reportportal.service.tree.TestItemTree.createTestItemLeaf;
import static java.util.Optional.ofNullable;
import static rp.com.google.common.base.Throwables.getStackTraceAsString;

/*
 * ReportPortal Extension sends the results of test execution to ReportPortal in RealTime
 */
public class ReportPortalExtension
		implements Extension, BeforeAllCallback, BeforeEachCallback, InvocationInterceptor, AfterTestExecutionCallback, AfterEachCallback,
				   AfterAllCallback, TestWatcher {

	public static final TestItemTree TEST_ITEM_TREE = new TestItemTree();
	public static ReportPortal REPORT_PORTAL = ReportPortal.builder().build();

	public static final FinishTestItemRQ SKIPPED_NOT_ISSUE;

	static {
		Issue issue = new Issue();
		issue.setIssueType(LaunchImpl.NOT_ISSUE);
		SKIPPED_NOT_ISSUE = new FinishTestItemRQ();
		SKIPPED_NOT_ISSUE.setIssue(issue);
		SKIPPED_NOT_ISSUE.setStatus(SKIPPED.name());
	}

	private static final String TEST_TEMPLATE_EXTENSION_CONTEXT = "org.junit.jupiter.engine.descriptor.TestTemplateExtensionContext";
	private static final Map<String, Launch> launchMap = new ConcurrentHashMap<>();
	private final Map<ExtensionContext, Maybe<String>> idMapping = new ConcurrentHashMap<>();
	private final Map<ExtensionContext, Maybe<String>> testTemplates = new ConcurrentHashMap<>();
	private final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(this);

	ReportPortal getReporter() {
		return REPORT_PORTAL;
	}

	String getLaunchId(ExtensionContext context) {
		return context.getRoot().getUniqueId();
	}

	protected Launch getLaunch(ExtensionContext context) {
		return launchMap.computeIfAbsent(getLaunchId(context), id -> {
			ReportPortal rp = getReporter();
			ListenerParameters params = rp.getParameters();
			StartLaunchRQ rq = new StartLaunchRQ();
			rq.setMode(params.getLaunchRunningMode());
			rq.setDescription(params.getDescription());
			rq.setName(params.getLaunchName());
			Set<ItemAttributesRQ> attributes = Sets.newHashSet(params.getAttributes());
			attributes.addAll(collectSystemAttributes(params.getSkippedAnIssue()));
			rq.setAttributes(attributes);
			rq.setStartTime(Calendar.getInstance().getTime());
			rq.setRerun(params.isRerun());
			rq.setRerunOf(StringUtils.isEmpty(params.getRerunOf()) ? null : params.getRerunOf());

			Launch launch = rp.newLaunch(rq);
			StepAspect.addLaunch(id, launch);
			Runtime.getRuntime().addShutdownHook(getShutdownHook(launch));
			Maybe<String> launchIdResponse = launch.start();
			if (params.isCallbackReportingEnabled()) {
				TEST_ITEM_TREE.setLaunchId(launchIdResponse);
			}
			return launch;
		});
	}

	@Override
	public void beforeAll(ExtensionContext context) {
		startTestItem(context, SUITE);
	}

	@Override
	public void afterAll(ExtensionContext context) {
		finishTemplates(context);
		if (context.getStore(NAMESPACE).get(FAILED) == null) {
			finishTestItem(context);
		} else {
			finishTestItem(context, FAILED);
			context.getParent().ifPresent(p -> p.getStore(NAMESPACE).put(FAILED, Boolean.TRUE));
		}
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		startTemplate(context);
	}

	@Override
	public void afterEach(ExtensionContext context) {

	}

	@Override
	public void interceptBeforeAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext parentContext) throws Throwable {
		Maybe<String> id = startBeforeAfter(invocationContext.getExecutable(), parentContext, parentContext, BEFORE_CLASS);
		finishBeforeAfter(invocation, parentContext, id);
	}

	@Override
	public void interceptBeforeEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext context) throws Throwable {
		ExtensionContext parentContext = context.getParent()
				.orElseThrow(() -> new IllegalStateException("Unable to find parent test for @BeforeEach method"));
		Maybe<String> id = startBeforeAfter(invocationContext.getExecutable(), parentContext, context, BEFORE_METHOD);
		finishBeforeTestSkip(invocation, invocationContext, context, id);
	}

	@Override
	public void interceptAfterAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext parentContext) throws Throwable {
		Maybe<String> id = startBeforeAfter(invocationContext.getExecutable(), parentContext, parentContext, AFTER_CLASS);
		finishBeforeAfter(invocation, parentContext, id);
	}

	@Override
	public void interceptAfterEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext context) throws Throwable {
		ExtensionContext parentContext = context.getParent()
				.orElseThrow(() -> new IllegalStateException("Unable to find parent test for @AfterEach method"));
		Maybe<String> id = startBeforeAfter(invocationContext.getExecutable(), parentContext, context, AFTER_METHOD);
		finishBeforeAfter(invocation, context, id);
	}

	@Override
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		startTestItem(extensionContext, invocationContext.getArguments(), STEP);
		invocation.proceed();
	}

	@Override
	public <T> T interceptTestFactoryMethod(Invocation<T> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		startTestItem(extensionContext, invocationContext.getArguments(), STEP);
		return invocation.proceed();
	}

	@Override
	public void interceptDynamicTest(Invocation<Void> invocation, ExtensionContext extensionContext) throws Throwable {
		startTestItem(extensionContext, STEP);
		try {
			invocation.proceed();
			finishTestItem(extensionContext);
		} catch (Throwable throwable) {
			sendStackTraceToRP(throwable);
			finishTestItem(extensionContext, FAILED);
			throw throwable;
		}
	}

	@Override
	public void interceptTestTemplateMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		startTestItem(extensionContext, invocationContext.getArguments(), STEP);
		invocation.proceed();
	}

	@Override
	public void afterTestExecution(ExtensionContext context) {
		Status status = getExecutionStatus(context);
		if (FAILED.equals(status)) {
			context.getParent().ifPresent(c -> c.getStore(NAMESPACE).put(FAILED, Boolean.TRUE));
		}
		finishTestItem(context, status);
	}

	@Override
	public void testDisabled(ExtensionContext context, Optional<String> reason) {
		if (Boolean.parseBoolean(System.getProperty("reportDisabledTests"))) {
			String description = reason.orElse(context.getDisplayName());
			startTestItem(context, Collections.emptyList(), STEP, description, null);
			finishTestItem(context, SKIPPED);
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

	private static final Function<List<Object>, String> TRANSFORM_PARAMETERS = it -> "[" + it.stream()
			.map(parameter -> Objects.isNull(parameter) ? "NULL" : parameter.toString())
			.collect(Collectors.joining(",")) + "]";

	private void finishBeforeTestSkip(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext context, Maybe<String> id) throws Throwable {
		Date startTime = Calendar.getInstance().getTime();
		try {
			finishBeforeAfter(invocation, context, id);
		} catch (Throwable throwable) {
			Date skipStartTime = Calendar.getInstance().getTime();
			if (skipStartTime.after(startTime)) {
				// to fix item ordering when @AfterEach starts in the same millisecond as skipped test
				skipStartTime = new Date(skipStartTime.getTime() - 1);
			}
			startTestItem(context, invocationContext.getArguments(), STEP, null, skipStartTime);
			finishTestItem(context, SKIPPED_NOT_ISSUE); // an issue relates to @BeforeEach method in this case
			throw throwable;
		}
	}

	private void finishBeforeAfter(Invocation<Void> invocation, ExtensionContext context, Maybe<String> id) throws Throwable {
		try {
			invocation.proceed();
			finishBeforeAfter(context, id, PASSED);
		} catch (Throwable throwable) {
			sendStackTraceToRP(throwable);
			finishBeforeAfter(context, id, FAILED);
			throw throwable;
		}
	}

	private void finishBeforeAfter(ExtensionContext context, Maybe<String> id, Status status) {
		Launch launch = getLaunch(context);
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setStatus(status.name());
		rq.setEndTime(Calendar.getInstance().getTime());
		launch.finishTestItem(id, rq);
	}

	private void startTemplate(ExtensionContext context) {
		context.getParent().ifPresent(parent -> {
			if (TEST_TEMPLATE_EXTENSION_CONTEXT.equals(parent.getClass().getCanonicalName()) && !idMapping.containsKey(parent)) {
				startTestItem(parent, TEMPLATE);
			}
		});
	}

	private void startTestItem(ExtensionContext context, List<Object> arguments, ItemType type) {
		startTestItem(context, arguments, type, null, null);
	}

	private void startTestItem(ExtensionContext context, ItemType type) {
		startTestItem(context, Collections.emptyList(), type, null, null);
	}

	private String getCodeRef(Method method) {
		return method.getDeclaringClass().getCanonicalName() + "." + method.getName();
	}

	private static String appendSuffixIfNotEmpty(String str, String suffix) {
		return str + (suffix.isEmpty() ? "" : "$" + suffix);
	}

	private String getCodeRef(ExtensionContext context, String currentCodeRef) {
		return context.getTestMethod().map(m -> appendSuffixIfNotEmpty(getCodeRef(m), currentCodeRef)).orElseGet(() -> {
			String newCodeRef = appendSuffixIfNotEmpty(context.getDisplayName(), currentCodeRef);
			return context.getParent().map(c -> getCodeRef(c, newCodeRef)).orElse(newCodeRef);
		});
	}

	private Optional<Method> getTestMethod(ExtensionContext context) {
		return ofNullable(context.getTestMethod().orElseGet(() -> context.getParent().flatMap(this::getTestMethod).orElse(null)));
	}

	private static boolean isRetry(ExtensionContext context) {
		return context.getTestMethod().map(it -> Objects.nonNull(it.getAnnotation(RepeatedTest.class))).orElse(false);
	}

	private void startTestItem(@NotNull final ExtensionContext context, @NotNull final List<Object> arguments,
			@NotNull final ItemType itemType, final String description, final Date startTime) {
		idMapping.computeIfAbsent(context, c -> {
			boolean isTemplate = TEMPLATE == itemType;
			ItemType type = isTemplate ? SUITE : itemType;
			boolean retry = isRetry(c);

			TestItem testItem = getTestItem(c, retry);
			Launch launch = getLaunch(c);
			StartTestItemRQ rq = new StartTestItemRQ();
			if (startTime == null) {
				rq.setStartTime(Calendar.getInstance().getTime());
			} else {
				rq.setStartTime(startTime);
			}
			rq.setName(testItem.getName());
			rq.setDescription(null != description ? description : testItem.getDescription());
			rq.setUniqueId(testItem.getUniqueId());
			rq.setType(type.name());
			rq.setRetry(retry);
			if (SUITE.equals(type)) {
				c.getTestClass().map(Class::getCanonicalName).ifPresent(codeRef -> {
					rq.setCodeRef(codeRef);
					TestCaseIdEntry testCaseIdEntry = getTestCaseId(codeRef);
					rq.setTestCaseId(testCaseIdEntry.getId());
				});
			} else {
				String codeRef = getCodeRef(c, "");
				rq.setCodeRef(codeRef);
				Optional<Method> testMethod = getTestMethod(c);
				TestCaseIdEntry caseId = testMethod.map(m -> {
					rq.setAttributes(getAttributes(m));
					rq.setParameters(getParameters(m, arguments));
					return getTestCaseId(m, codeRef, arguments);
				}).orElseGet(() -> getTestCaseId(codeRef, arguments));

				rq.setTestCaseId(caseId.getId());
			}
			ofNullable(testItem.getAttributes()).ifPresent(attributes -> ofNullable(rq.getAttributes()).orElseGet(() -> {
				rq.setAttributes(Sets.newHashSet());
				return rq.getAttributes();
			}).addAll(attributes));

			Maybe<String> itemId = c.getParent().flatMap(parent -> Optional.ofNullable(idMapping.get(parent))).map(parentTest -> {
				Maybe<String> item = launch.startTestItem(parentTest, rq);
				if (getReporter().getParameters().isCallbackReportingEnabled()) {
					TEST_ITEM_TREE.getTestItems().put(createItemTreeKey(testItem.getName()), createTestItemLeaf(parentTest, item, 0));
				}
				return item;
			}).orElseGet(() -> {
				Maybe<String> item = launch.startTestItem(rq);
				if (getReporter().getParameters().isCallbackReportingEnabled()) {
					TEST_ITEM_TREE.getTestItems().put(createItemTreeKey(testItem.getName()), createTestItemLeaf(item, 0));
				}
				return item;
			});
			if (isTemplate) {
				testTemplates.put(c, itemId);
			}
			StepAspect.setParentId(itemId);
			return itemId;
		});
	}

	private @NotNull Set<ItemAttributesRQ> getAttributes(@NotNull final Method method) {
		return ofNullable(method.getAnnotation(Attributes.class)).map(AttributeParser::retrieveAttributes).orElseGet(Sets::newHashSet);
	}

	private @NotNull List<ParameterResource> getParameters(@NotNull final Method method, final List<Object> arguments) {
		Parameter[] params = method.getParameters();
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		return IntStream.range(0, arguments.size()).boxed().map(i -> {
			ParameterResource res = new ParameterResource();
			String parameterName = i < params.length ?
					Arrays.stream(parameterAnnotations[i])
							.filter(a -> ParameterKey.class.equals(a.annotationType()))
							.map(a -> ((ParameterKey) a).value())
							.findFirst()
							.orElseGet(() -> params[i].getType().getName()) :
					arguments.get(i).getClass().getCanonicalName();
			res.setKey(parameterName);
			res.setValue(ofNullable(arguments.get(i)).orElse("NULL").toString());
			return res;
		}).collect(Collectors.toList());
	}

	private Maybe<String> startBeforeAfter(Method method, ExtensionContext parentContext, ExtensionContext context, ItemType itemType) {
		Launch launch = getLaunch(context);
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setName(method.getName() + "()");
		rq.setDescription(method.getName());
		String uniqueId = parentContext.getUniqueId() + "/[method:" + method.getName() + "()]";
		rq.setUniqueId(uniqueId);
		ofNullable(context.getTags()).ifPresent(it -> rq.setAttributes(it.stream()
				.map(tag -> new ItemAttributesRQ(null, tag))
				.collect(Collectors.toSet())));
		rq.setType(itemType.name());
		rq.setRetry(false);
		String codeRef = method.getDeclaringClass().getCanonicalName() + "." + method.getName();
		rq.setCodeRef(codeRef);
		TestCaseIdEntry testCaseIdEntry = ofNullable(method.getAnnotation(TestCaseId.class)).map(TestCaseId::value)
				.map(TestCaseIdEntry::new)
				.orElseGet(() -> getTestCaseId(codeRef));
		rq.setTestCaseId(testCaseIdEntry.getId());
		Maybe<String> itemId = launch.startTestItem(idMapping.get(parentContext), rq);
		StepAspect.setParentId(itemId);
		return itemId;
	}

	private @NotNull TestCaseIdEntry getTestCaseId(@NotNull final String codeRef) {
		return new TestCaseIdEntry(codeRef);
	}

	private @NotNull TestCaseIdEntry getTestCaseId(@NotNull final Method method, final String codeRef, final List<Object> arguments) {
		TestCaseId caseId = method.getAnnotation(TestCaseId.class);
		if (caseId != null) {
			return caseId.parametrized() ?
					TestCaseIdUtils.getParameterizedTestCaseId(method, arguments) :
					new TestCaseIdEntry(caseId.value());
		}
		return getTestCaseId(codeRef, arguments);
	}

	private @NotNull TestCaseIdEntry getTestCaseId(@NotNull final String codeRef, @NotNull final List<Object> arguments) {
		String caseId = arguments.isEmpty() ? codeRef : codeRef + TRANSFORM_PARAMETERS.apply(arguments);
		return new TestCaseIdEntry(caseId);
	}

	private void finishTemplates(final ExtensionContext parentContext) {
		List<ExtensionContext> templates = testTemplates.keySet()
				.stream()
				.filter((c) -> c.getParent().map(myParent -> parentContext == myParent).orElse(false))
				.collect(Collectors.toList());

		templates.forEach(context -> {
			Status status = context.getStore(NAMESPACE).get(FAILED) != null ?
					FAILED :
					context.getStore(NAMESPACE).get(SKIPPED) != null ? SKIPPED : getExecutionStatus(context);
			if (status == SKIPPED) {
				parentContext.getStore(NAMESPACE).put(SKIPPED, Boolean.TRUE);
			}
			if (status == FAILED) {
				parentContext.getStore(NAMESPACE).put(FAILED, Boolean.TRUE);
			}
			finishTemplate(context, status);
		});
	}

	private void finishTemplate(final ExtensionContext context, final Status status) {
		Launch launch = getLaunch(context);
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setStatus(status.name());
		rq.setEndTime(Calendar.getInstance().getTime());
		Maybe<String> templateId = testTemplates.remove(context);
		launch.finishTestItem(templateId, rq);
	}

	private static Status getExecutionStatus(@NotNull final ExtensionContext context) {
		Optional<Throwable> exception = context.getExecutionException();
		if (!exception.isPresent()) {
			return Status.PASSED;
		} else {
			sendStackTraceToRP(exception.get());
			return Status.FAILED;
		}
	}

	private void finishTestItem(@NotNull final ExtensionContext context) {
		finishTestItem(context, getExecutionStatus(context));
	}

	private void finishTestItem(@NotNull final ExtensionContext context, @NotNull final Status status) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setStatus(status.name());
		finishTestItem(context, rq);
	}

	private void finishTestItem(@NotNull final ExtensionContext context, @NotNull final FinishTestItemRQ rq) {
		Launch launch = getLaunch(context);
		if (Objects.isNull(rq.getEndTime())) {
			rq.setEndTime(Calendar.getInstance().getTime());
		}
		Maybe<OperationCompletionRS> finishResponse = launch.finishTestItem(idMapping.remove(context), rq);
		if (getReporter().getParameters().isCallbackReportingEnabled()) {
			ofNullable(TEST_ITEM_TREE.getTestItems().get(createItemTreeKey(context))).ifPresent(itemLeaf -> itemLeaf.setFinishResponse(
					finishResponse));
		}
	}

	private static Thread getShutdownHook(final Launch launch) {
		return new Thread(() -> {
			FinishExecutionRQ rq = new FinishExecutionRQ();
			rq.setEndTime(Calendar.getInstance().getTime());
			launch.finish(rq);
		});
	}

	private static void sendStackTraceToRP(final Throwable cause) {
		ReportPortal.emitLog(itemUuid -> {
			SaveLogRQ rq = new SaveLogRQ();
			rq.setItemUuid(itemUuid);
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

	protected TestItem getTestItem(ExtensionContext context, boolean isRetry) {
		String name;
		String uniqueId;
		Optional<ExtensionContext> parent = context.getParent();
		if (isRetry && parent.isPresent()) {
			ExtensionContext parentContext = parent.get();
			name = parentContext.getDisplayName();
			uniqueId = parentContext.getUniqueId();
		} else {
			name = context.getDisplayName();
			uniqueId = context.getUniqueId();
		}
		name = name.length() > 1024 ? name.substring(0, 1024) + "..." : name;
		String description = context.getDisplayName();
		Set<String> tags = context.getTags();
		return new TestItem(name, description, uniqueId, tags);
	}

	protected static class TestItem {

		private final String name;
		private final String description;
		private final String uniqueId;
		private final Set<ItemAttributesRQ> attributes;

		String getName() {
			return name;
		}

		String getDescription() {
			return description;
		}

		Set<ItemAttributesRQ> getAttributes() {
			return attributes;
		}

		public TestItem(String name, String description, String uniqueId, Set<String> tags) {
			this.name = name;
			this.description = description;
			this.uniqueId = uniqueId;
			this.attributes = tags.stream().map(it -> new ItemAttributesRQ(null, it)).collect(Collectors.toSet());
		}

		public String getUniqueId() {
			return uniqueId;
		}
	}
}
