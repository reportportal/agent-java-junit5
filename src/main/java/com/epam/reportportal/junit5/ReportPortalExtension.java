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
import com.epam.reportportal.utils.ParameterUtils;
import com.epam.reportportal.utils.TestCaseIdUtils;
import com.epam.ta.reportportal.ws.model.*;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.extension.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rp.com.google.common.collect.Sets;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

	private static final Logger LOGGER = LoggerFactory.getLogger(ReportPortalExtension.class);

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

	private static final Map<String, Launch> launchMap = new ConcurrentHashMap<>();
	private final Map<ExtensionContext, Maybe<String>> idMapping = new ConcurrentHashMap<>();
	private final Map<ExtensionContext, Maybe<String>> testTemplates = new ConcurrentHashMap<>();
	private final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(this);

	protected ReportPortal getReporter() {
		return REPORT_PORTAL;
	}

	protected String getLaunchId(ExtensionContext context) {
		return context.getRoot().getUniqueId();
	}

	protected Launch getLaunch(ExtensionContext context) {
		return launchMap.computeIfAbsent(getLaunchId(context), id -> {
			ReportPortal rp = getReporter();
			ListenerParameters params = rp.getParameters();
			StartLaunchRQ rq = buildStartLaunchRq(params);

			Launch launch = rp.newLaunch(rq);
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
		getLaunch(context); // Trigger launch start
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
		context.getParent().ifPresent(this::startTemplate);
	}

	@Override
	public void afterEach(ExtensionContext context) {

	}

	@Override
	public void interceptBeforeAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext parentContext) throws Throwable {
		Maybe<String> id = startBeforeAfter(invocationContext.getExecutable(), parentContext, parentContext, BEFORE_CLASS);
		finishBeforeAll(invocation, invocationContext, parentContext, id);
	}

	@Override
	public void interceptBeforeEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext context) throws Throwable {
		ExtensionContext parentContext = context.getParent().orElse(context.getRoot());
		Maybe<String> id = startBeforeAfter(invocationContext.getExecutable(), parentContext, context, BEFORE_METHOD);
		finishBeforeEach(invocation, invocationContext, context, id);
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
		ExtensionContext parentContext = context.getParent().orElse(context.getRoot());
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
		startTestItem(extensionContext, invocationContext.getArguments(), SUITE);
		return invocation.proceed();
	}

	@Override
	public void interceptDynamicTest(Invocation<Void> invocation, ExtensionContext extensionContext) throws Throwable {
		Optional<ExtensionContext> parent = extensionContext.getParent();
		if (parent.map(p -> !idMapping.containsKey(p)).orElse(false)) {
			List<ExtensionContext> parents = new ArrayList<>();
			parents.add(parent.get());
			while ((parent = parents.get(parents.size() - 1).getParent()).isPresent()) {
				ExtensionContext p = parent.get();
				if (idMapping.containsKey(p)) {
					break;
				}
				parents.add(p);
			}
			Collections.reverse(parents);
			parents.forEach(this::startTemplate);
		}
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
		finishTemplates(context);
		Status status = getExecutionStatus(context);
		if (FAILED.equals(status)) {
			context.getParent().ifPresent(c -> c.getStore(NAMESPACE).put(FAILED, Boolean.TRUE));
		}
		finishTestItem(context, status);
	}

	@Override
	public void testDisabled(ExtensionContext context, Optional<String> reason) {
		if (Boolean.parseBoolean(System.getProperty("reportDisabledTests"))) {
			String description = reason.orElse(createStepDescription(context));
			startTestItem(context, Collections.emptyList(), STEP, description, Calendar.getInstance().getTime());
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

	private void finishBeforeAll(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext context, Maybe<String> id) throws Throwable {
		Date startTime = Calendar.getInstance().getTime();
		try {
			finishBeforeAfter(invocation, context, id);
		} catch (Throwable throwable) {
			reportSkippedClassTests(invocationContext, context, startTime);
			throw throwable;
		}
	}

	private void finishBeforeEach(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext context, Maybe<String> id) throws Throwable {
		Date startTime = Calendar.getInstance().getTime();
		try {
			finishBeforeAfter(invocation, context, id);
		} catch (Throwable throwable) {
			reportSkippedStep(invocationContext, context, throwable, startTime);
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
		launch.getStepReporter().finishPreviousStep();
		launch.finishTestItem(id, buildFinishTestItemRq(context, status));
	}

	private void startTemplate(ExtensionContext context) {
		if (!idMapping.containsKey(context)) {
			startTestItem(context, TEMPLATE);
		}
	}

	private void startTestItem(ExtensionContext context, ItemType type) {
		startTestItem(context, Collections.emptyList(), type);
	}

	private void startTestItem(ExtensionContext context, List<Object> arguments, ItemType type) {
		startTestItem(context, arguments, type, createStepDescription(context), Calendar.getInstance().getTime());
	}

	private String getCodeRef(@Nonnull final Method method) {
		return method.getDeclaringClass().getCanonicalName() + "." + method.getName();
	}

	private static String appendSuffixIfNotEmpty(final String str, @Nonnull final String suffix) {
		return str + (suffix.isEmpty() ? "" : "$" + suffix);
	}

	@Nonnull
	private String getCodeRef(@Nonnull final ExtensionContext context, @Nonnull final String currentCodeRef) {
		return context.getTestMethod()
				.map(m -> appendSuffixIfNotEmpty(getCodeRef(m), currentCodeRef))
				.orElseGet(() -> context.getTestClass()
						.map(c -> appendSuffixIfNotEmpty(c.getCanonicalName(), currentCodeRef))
						.orElseGet(() -> {
							String newCodeRef = appendSuffixIfNotEmpty(context.getDisplayName(), currentCodeRef);
							return context.getParent().map(c -> getCodeRef(c, newCodeRef)).orElse(newCodeRef);
						}));
	}

	private void startTestItem(@Nonnull final ExtensionContext context, @Nonnull final List<Object> arguments,
			@Nonnull final ItemType itemType, @Nonnull final String description, @Nonnull final Date startTime) {
		idMapping.computeIfAbsent(context, c -> {
			boolean isTemplate = TEMPLATE == itemType;
			ItemType type = isTemplate ? SUITE : itemType;

			StartTestItemRQ rq = buildStartStepRq(c, arguments, type, description, startTime);
			Launch launch = getLaunch(c);
			Maybe<String> itemId = c.getParent().flatMap(parent -> Optional.ofNullable(idMapping.get(parent))).map(parentTest -> {
				Maybe<String> item = launch.startTestItem(parentTest, rq);
				if (getReporter().getParameters().isCallbackReportingEnabled()) {
					TEST_ITEM_TREE.getTestItems().put(createItemTreeKey(rq.getName()), createTestItemLeaf(parentTest, item, 0));
				}
				return item;
			}).orElseGet(() -> {
				Maybe<String> item = launch.startTestItem(rq);
				if (getReporter().getParameters().isCallbackReportingEnabled()) {
					TEST_ITEM_TREE.getTestItems().put(createItemTreeKey(rq.getName()), createTestItemLeaf(item, 0));
				}
				return item;
			});
			if (isTemplate) {
				testTemplates.put(c, itemId);
			}
			StepAspect.setParentId(launch, itemId);
			return itemId;
		});
	}

	private @Nonnull
	Set<ItemAttributesRQ> getAttributes(@Nonnull final Method method) {
		return ofNullable(method.getAnnotation(Attributes.class)).map(AttributeParser::retrieveAttributes).orElseGet(Sets::newHashSet);
	}

	private @Nonnull
	List<ParameterResource> getParameters(@Nonnull final Method method, final List<Object> arguments) {
		return ParameterUtils.getParameters(method, arguments);
	}

	private Maybe<String> startBeforeAfter(Method method, ExtensionContext parentContext, ExtensionContext context, ItemType itemType) {
		Launch launch = getLaunch(context);
		StartTestItemRQ rq = buildStartConfigurationRq(method, parentContext, context, itemType);
		Maybe<String> itemId = launch.startTestItem(idMapping.get(parentContext), rq);
		StepAspect.setParentId(launch, itemId);
		return itemId;
	}

	private TestCaseIdEntry getTestCaseId(@Nonnull final Method method, @Nonnull final String codeRef, @Nonnull final List<Object> arguments) {
		TestCaseId caseId = method.getAnnotation(TestCaseId.class);
		TestCaseIdEntry id = TestCaseIdUtils.getTestCaseId(caseId, method, codeRef, arguments);
		if (id == null) {
			return null;
		}
		return id.getId().endsWith("[]") ? new TestCaseIdEntry(id.getId().substring(0, id.getId().length() - 2)) : id;
	}

	private void finishTemplates(final ExtensionContext parentContext) {
		final Collection<ExtensionContext> parents = new HashSet<>();
		parents.add(parentContext);
		List<ExtensionContext> templates = new ArrayList<>();
		Collection<ExtensionContext> children;
		while (!(children = testTemplates.keySet()
				.stream()
				.filter((c) -> c.getParent().map(parents::contains).orElse(false))
				.collect(Collectors.toSet())).isEmpty()) {
			templates.addAll(children);
			parents.clear();
			parents.addAll(children);
		}
		Collections.reverse(templates);
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
		Maybe<String> templateId = testTemplates.remove(context);
		launch.finishTestItem(templateId, buildFinishTestItemRq(context, status));
		idMapping.remove(context);
	}

	private static Status getExecutionStatus(@Nonnull final ExtensionContext context) {
		Optional<Throwable> exception = context.getExecutionException();
		if (!exception.isPresent()) {
			return Status.PASSED;
		} else {
			sendStackTraceToRP(exception.get());
			return Status.FAILED;
		}
	}

	private void finishTestItem(@Nonnull final ExtensionContext context) {
		finishTestItem(context, getExecutionStatus(context));
	}

	private void finishTestItem(@Nonnull final ExtensionContext context, @Nonnull final Status status) {
		finishTestItem(context, buildFinishTestItemRq(context, status));
	}

	private void finishTestItem(@Nonnull final ExtensionContext context, @Nonnull final FinishTestItemRQ rq) {
		Launch launch = getLaunch(context);
		launch.getStepReporter().finishPreviousStep();
		Maybe<String> id = idMapping.remove(context);
		Maybe<OperationCompletionRS> finishResponse = launch.finishTestItem(id, rq);
		if (getReporter().getParameters().isCallbackReportingEnabled()) {
			ofNullable(TEST_ITEM_TREE.getTestItems().get(createItemTreeKey(context))).ifPresent(itemLeaf -> itemLeaf.setFinishResponse(
					finishResponse));
		}
	}

	private Optional<Method> getTestMethod(ExtensionContext context) {
		return ofNullable(context.getTestMethod().orElseGet(() -> context.getParent().flatMap(this::getTestMethod).orElse(null)));
	}

	/**
	 * Extension point to customize test step creation event/request
	 *
	 * @param context     JUnit's test context
	 * @param arguments   a test arguments list
	 * @param itemType    a test method item type
	 * @param description a test method description
	 * @param startTime   a start time of the test
	 * @return Request to ReportPortal
	 */
	protected StartTestItemRQ buildStartStepRq(@Nonnull final ExtensionContext context, @Nonnull final List<Object> arguments,
			@Nonnull final ItemType itemType, @Nonnull final String description, @Nonnull final Date startTime) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setStartTime(startTime);
		rq.setName(createStepName(context));
		rq.setDescription(description);
		rq.setUniqueId(context.getUniqueId());
		rq.setType(itemType.name());
		String codeRef = getCodeRef(context, "");
		rq.setCodeRef(codeRef);
		rq.setAttributes(context.getTags().stream().map(it -> new ItemAttributesRQ(null, it)).collect(Collectors.toSet()));

		Optional<Method> testMethod = getTestMethod(context);
		TestCaseIdEntry caseId = testMethod.map(m -> {
			rq.getAttributes().addAll(getAttributes(m));
			rq.setParameters(getParameters(m, arguments));
			return getTestCaseId(m, codeRef, arguments);
		}).orElseGet(() -> TestCaseIdUtils.getTestCaseId(codeRef, arguments));
		rq.setTestCaseId(caseId.getId());
		return rq;
	}

	/**
	 * Extension point to customize beforeXXX creation event/request
	 *
	 * @param method        JUnit's test method reference
	 * @param parentContext JUnit's context of a parent item
	 * @param context       JUnit's test context
	 * @param itemType      a type of the item to build
	 * @return Request to ReportPortal
	 */
	protected StartTestItemRQ buildStartConfigurationRq(Method method, ExtensionContext parentContext, ExtensionContext context,
			ItemType itemType) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setStartTime(Calendar.getInstance().getTime());
		Optional<Class<?>> testClass = context.getTestClass();
		if (testClass.isPresent()) {
			rq.setName(createConfigurationName(testClass.get(), method));
			rq.setDescription(createConfigurationDescription(testClass.get(), method));
		} else {
			rq.setName(createConfigurationName(method.getDeclaringClass(), method));
			rq.setDescription(createConfigurationDescription(method.getDeclaringClass(), method));
		}
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
				.orElseGet(() -> TestCaseIdUtils.getTestCaseId(codeRef, Collections.emptyList()));
		rq.setTestCaseId(testCaseIdEntry.getId());
		return rq;
	}

	/**
	 * Extension point to customize skipped test insides
	 *
	 * @param context JUnit's test context
	 * @param cause   an error thrown by skip culprit
	 */
	@SuppressWarnings("unused")
	protected void createSkippedSteps(ExtensionContext context, Throwable cause) {
	}

	/**
	 * Extension point to customize a test item result on it's finish
	 *
	 * @param context JUnit's test context
	 * @param status  a test item execution result
	 * @return Request to ReportPortal
	 */
	@SuppressWarnings("unused")
	protected FinishTestItemRQ buildFinishTestItemRq(ExtensionContext context, Status status) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setStatus(status.name());
		rq.setEndTime(Calendar.getInstance().getTime());
		return rq;
	}

	/**
	 * Extension point to customize launch creation event/request
	 *
	 * @param parameters Launch Configuration parameters
	 * @return Request to ReportPortal
	 */
	protected StartLaunchRQ buildStartLaunchRq(ListenerParameters parameters) {
		StartLaunchRQ rq = new StartLaunchRQ();
		rq.setMode(parameters.getLaunchRunningMode());
		rq.setDescription(parameters.getDescription());
		rq.setName(parameters.getLaunchName());
		Set<ItemAttributesRQ> attributes = Sets.newHashSet(parameters.getAttributes());
		attributes.addAll(collectSystemAttributes(parameters.getSkippedAnIssue()));
		rq.setAttributes(attributes);
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setRerun(parameters.isRerun());
		rq.setRerunOf(StringUtils.isEmpty(parameters.getRerunOf()) ? null : parameters.getRerunOf());
		return rq;
	}

	/**
	 * Extension point to customize test step name
	 *
	 * @param context JUnit's test context
	 * @return Test/Step Name being sent to ReportPortal
	 */
	@SuppressWarnings("unused")
	protected String createStepName(ExtensionContext context) {
		String name = context.getDisplayName();
		return name.length() > 1024 ? name.substring(0, 1024) + "..." : name;
	}

	/**
	 * Extension point to customize beforeXXX step name
	 *
	 * @param testClass JUnit's test class, by which the name will be calculated
	 * @param method    JUnit's test method reference
	 * @return Test/Step Name being sent to ReportPortal
	 */
	protected String createConfigurationName(Class<?> testClass, Method method) {
		DisplayName displayName = method.getDeclaredAnnotation(DisplayName.class);
		if (displayName != null) {
			return displayName.value();
		}
		DisplayNameGeneration displayNameGenerator = method.getDeclaredAnnotation(DisplayNameGeneration.class);
		if (displayNameGenerator == null) {
			displayNameGenerator = testClass.getDeclaredAnnotation(DisplayNameGeneration.class);
		}
		if (displayNameGenerator != null) {
			Class<? extends DisplayNameGenerator> generatorClass = displayNameGenerator.value();
			try {
				DisplayNameGenerator generator = generatorClass.getConstructor().newInstance();
				return generator.generateDisplayNameForMethod(testClass, method);
			} catch (Exception e) {
				LOGGER.error("Unable instantiate a display name generator. Name generation skipped.", e);
			}
		}
		return method.getName() + "()";
	}

	/**
	 * Extension point to customize test step description
	 *
	 * @param context JUnit's test context
	 * @return Test/Step Description being sent to ReportPortal
	 */
	@SuppressWarnings("unused")
	protected String createStepDescription(ExtensionContext context) {
		return "";
	}

	/**
	 * Extension point to customize beforeXXX step description
	 *
	 * @param testClass JUnit's test class, by which the name will be calculated
	 * @param method    JUnit's test method reference
	 * @return Test/Step Description being sent to ReportPortal
	 */
	@SuppressWarnings("unused")
	protected String createConfigurationDescription(Class<?> testClass, Method method) {
		return "";
	}

	/**
	 * Extension point to customize test steps skipped in case of a <code>@BeforeEach</code> method failed.
	 *
	 * @param invocationContext JUnit's <code>@BeforeAll</code> invocation context
	 * @param context           JUnit's test context
	 * @param eventTime         <code>@BeforeAll</code> start time
	 */
	@SuppressWarnings("unused")
	protected void reportSkippedStep(ReflectiveInvocationContext<Method> invocationContext, ExtensionContext context, Throwable throwable,
			Date eventTime) {
		Date skipStartTime = Calendar.getInstance().getTime();
		if (skipStartTime.after(eventTime)) {
			// to fix item ordering when @AfterEach starts in the same millisecond as skipped test
			skipStartTime = new Date(skipStartTime.getTime() - 1);
		}
		startTestItem(context, invocationContext.getArguments(), STEP, createStepDescription(context), skipStartTime);
		createSkippedSteps(context, throwable);
		finishTestItem(context, SKIPPED_NOT_ISSUE); // an issue relates to @BeforeEach method in this case
	}

	/**
	 * Extension point to customize test steps skipped in case of a <code>@BeforeAll</code> method failed.
	 *
	 * @param invocationContext JUnit's <code>@BeforeAll</code> invocation context
	 * @param context           JUnit's test context
	 * @param eventTime         <code>@BeforeAll</code> start time
	 */
	@SuppressWarnings("unused")
	protected void reportSkippedClassTests(ReflectiveInvocationContext<Method> invocationContext, ExtensionContext context,
			Date eventTime) {
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
}
