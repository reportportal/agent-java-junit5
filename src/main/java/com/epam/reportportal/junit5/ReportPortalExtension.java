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
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.item.TestCaseIdEntry;
import com.epam.reportportal.service.tree.TestItemTree;
import com.epam.reportportal.utils.AttributeParser;
import com.epam.reportportal.utils.ParameterUtils;
import com.epam.reportportal.utils.TestCaseIdUtils;
import com.epam.ta.reportportal.ws.model.*;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.opentest4j.TestAbortedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.epam.reportportal.junit5.ItemType.*;
import static com.epam.reportportal.junit5.SystemAttributesFetcher.collectSystemAttributes;
import static com.epam.reportportal.junit5.utils.ItemTreeUtils.createItemTreeKey;
import static com.epam.reportportal.listeners.ItemStatus.*;
import static com.epam.reportportal.service.tree.TestItemTree.createTestItemLeaf;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

/*
 * ReportPortal Extension sends the results of test execution to ReportPortal in RealTime
 */
public class ReportPortalExtension
		implements Extension, BeforeAllCallback, BeforeEachCallback, InvocationInterceptor, AfterTestExecutionCallback,
				   AfterAllCallback, TestWatcher {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReportPortalExtension.class);

	private static final Set<String> ASSUMPTION_CLASSES = new HashSet<>(Arrays.asList(TestAbortedException.class.getCanonicalName(),
			"org.junit.AssumptionViolatedException"
	));

	private static final Predicate<Throwable> IS_ASSUMPTION = e -> ofNullable(e).map(Object::getClass).flatMap(c -> {
		Class<?> clazz = c;
		do {
			if (ASSUMPTION_CLASSES.contains(clazz.getCanonicalName())) {
				return of(clazz);
			}
		} while ((clazz = clazz.getSuperclass()) != null);
		return Optional.empty();
	}).isPresent();

	public static final TestItemTree TEST_ITEM_TREE = new TestItemTree();
	public static final ReportPortal REPORT_PORTAL = ReportPortal.builder().build();

	private static final Map<String, Launch> launchMap = new ConcurrentHashMap<>();
	private final Map<ExtensionContext, Maybe<String>> idMapping = new ConcurrentHashMap<>();
	private final Map<ExtensionContext, Maybe<String>> testTemplates = new ConcurrentHashMap<>();
	private final Set<ExtensionContext> failedClassInits = Collections.newSetFromMap(new ConcurrentHashMap<>());

	@Nonnull
	protected Optional<Maybe<String>> getItemId(@Nonnull ExtensionContext context) {
		return ofNullable(idMapping.get(context));
	}

	/**
	 * Finishes all launches for the JVM
	 */
	@SuppressWarnings("unused")
	public void finish() {
		new ArrayList<>(launchMap.keySet()).forEach(this::finish);
	}

	private void finish(String id) {
		ofNullable(launchMap.remove(id)).ifPresent(ReportPortalExtension::finish);
	}

	private static void finish(Launch launch) {
		FinishExecutionRQ rq = new FinishExecutionRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		launch.finish(rq);
	}

	private static Thread getShutdownHook(final String launchId) {
		return new Thread(() -> ofNullable(launchMap.remove(launchId)).ifPresent(ReportPortalExtension::finish));
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
		Set<ItemAttributesRQ> attributes = new HashSet<>(parameters.getAttributes());
		attributes.addAll(collectSystemAttributes(parameters.getSkippedAnIssue()));
		rq.setAttributes(attributes);
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setRerun(parameters.isRerun());
		rq.setRerunOf(StringUtils.isEmpty(parameters.getRerunOf()) ? null : parameters.getRerunOf());
		return rq;
	}

	/**
	 * @return ReportPortal client instance
	 */
	protected ReportPortal getReporter() {
		return REPORT_PORTAL;
	}

	/**
	 * Returns a current launch unique ID
	 *
	 * @param context JUnit's launch context
	 * @return ID of the launch
	 */
	protected String getLaunchId(ExtensionContext context) {
		return context.getRoot().getUniqueId();
	}

	/**
	 * Returns a current launch instance, starts new if no such instance
	 *
	 * @param context JUnit's launch context
	 * @return represents current launch
	 */
	protected Launch getLaunch(ExtensionContext context) {
		return launchMap.computeIfAbsent(getLaunchId(context), id -> {
			ReportPortal rp = getReporter();
			ListenerParameters params = rp.getParameters();
			StartLaunchRQ rq = buildStartLaunchRq(params);

			Launch launch = rp.newLaunch(rq);
			Runtime.getRuntime().addShutdownHook(getShutdownHook(id));
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

	/**
	 * Finish all test templates execution (basically a test class) within a specific context
	 *
	 * @param parentContext JUnit's test context
	 */
	protected void finishTemplates(final ExtensionContext parentContext) {
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
		templates.forEach(this::finishTemplate);
	}

	@Override
	public void afterAll(ExtensionContext context) {
		finishTemplates(context);
		finishTestItem(context);
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		context.getParent().ifPresent(this::startTemplate);
	}

	@Override
	public void interceptBeforeAllMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext parentContext) throws Throwable {
		Maybe<String> id = startBeforeAfter(
				invocationContext.getExecutable(),
				parentContext,
				parentContext,
				BEFORE_CLASS
		);
		finishBeforeAll(invocation, invocationContext, parentContext, id);
	}

	@Override
	public <T> T interceptTestClassConstructor(Invocation<T> invocation,
											   ReflectiveInvocationContext<Constructor<T>> invocationContext,
											   ExtensionContext parentContext) throws Throwable {
		try {
			return invocation.proceed();
		} catch (Throwable cause) {
			failedClassInits.add(parentContext);
			throw cause;
		}
	}

	@Override
	public void interceptBeforeEachMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext context) throws Throwable {
		ExtensionContext parentContext = context.getParent().orElse(context.getRoot());
		Maybe<String> id = startBeforeAfter(invocationContext.getExecutable(), parentContext, context, BEFORE_METHOD);
		finishBeforeEach(invocation, invocationContext, context, id);
	}

	@Override
	public void interceptAfterAllMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext parentContext) throws Throwable {
		Maybe<String> id = startBeforeAfter(
				invocationContext.getExecutable(),
				parentContext,
				parentContext,
				AFTER_CLASS
		);
		finishBeforeAfter(invocation, parentContext, id);
	}

	@Override
	public void interceptAfterEachMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext context) throws Throwable {
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
	public <T> T interceptTestFactoryMethod(Invocation<T> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		startTestItem(extensionContext, invocationContext.getArguments(), SUITE);
		return invocation.proceed();
	}

	/**
	 * Returns a status of a test based on execution exception
	 *
	 * @param throwable test exception
	 * @return an {@link ItemStatus}
	 */
	@Nonnull
	protected ItemStatus getExecutionStatus(@Nullable final Throwable throwable) {
		if (throwable == null) {
			return PASSED;
		}
		sendStackTraceToRP(throwable);
		return IS_ASSUMPTION.test(throwable) ? SKIPPED : FAILED;
	}

	@Override
	public void interceptDynamicTest(Invocation<Void> invocation, DynamicTestInvocationContext invocationContext,
									 ExtensionContext extensionContext) throws Throwable {
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
			finishTestItem(extensionContext, PASSED);
		} catch (Throwable throwable) {
			finishTestItem(extensionContext, getExecutionStatus(throwable));
			throw throwable;
		}
	}

	@Override
	public void interceptTestTemplateMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		startTestItem(extensionContext, invocationContext.getArguments(), STEP);
		invocation.proceed();
	}

	/**
	 * Returns a status of a test based on whether or not it contains an execution exception
	 *
	 * @param context JUnit's test context
	 * @return an {@link ItemStatus}
	 */
	@Nonnull
	protected ItemStatus getExecutionStatus(@Nonnull final ExtensionContext context) {
		return context.getExecutionException().map(this::getExecutionStatus).orElse(PASSED);
	}

	@Override
	public void afterTestExecution(ExtensionContext context) {
		finishTemplates(context);
		finishTestItem(context, getExecutionStatus(context));
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
	public void testFailed(ExtensionContext context, Throwable cause) {
		context.getParent().ifPresent(parent -> {
			if(failedClassInits.contains(parent)) {
				startTestItem(context, STEP);
				sendStackTraceToRP(cause);
				finishTestItem(context, FAILED);
			}
		});
	}

	/**
	 * Finishes a method marked with {@link BeforeAll} annotation, calls {@link ReportPortalExtension#reportSkippedClassTests} method in
	 * case of failures
	 *
	 * @param invocation        the invocation that is being intercepted
	 * @param invocationContext the context of the invocation that is being intercepted
	 * @param context           JUnit's test context
	 * @param id                an ID of the method to finish
	 * @throws Throwable in case of failures
	 */
	protected void finishBeforeAll(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext context, Maybe<String> id) throws Throwable {
		Date startTime = Calendar.getInstance().getTime();
		try {
			finishBeforeAfter(invocation, context, id);
		} catch (Throwable throwable) {
			reportSkippedClassTests(invocationContext, context, startTime);
			throw throwable;
		}
	}

	/**
	 * Finishes a method marked with {@link BeforeEach} annotation, calls {@link ReportPortalExtension#reportSkippedStep} method in case of
	 * failures
	 *
	 * @param invocation        the invocation that is being intercepted
	 * @param invocationContext the context of the invocation that is being intercepted
	 * @param context           JUnit's test context
	 * @param id                an ID of the method to finish
	 * @throws Throwable in case of failures
	 */
	protected void finishBeforeEach(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext context, Maybe<String> id) throws Throwable {
		Date startTime = Calendar.getInstance().getTime();
		try {
			finishBeforeAfter(invocation, context, id);
		} catch (Throwable throwable) {
			reportSkippedStep(invocationContext, context, throwable, startTime);
			throw throwable;
		}
	}

	private void finishBeforeAfter(Invocation<Void> invocation, ExtensionContext context, Maybe<String> id)
			throws Throwable {
		try {
			invocation.proceed();
		} catch (Throwable throwable) {
			finishBeforeAfter(context, id, getExecutionStatus(throwable));
			throw throwable;
		}
		finishBeforeAfter(context, id, PASSED);
	}

	private void finishBeforeAfter(ExtensionContext context, Maybe<String> id, ItemStatus status) {
		Launch launch = getLaunch(context);
		//noinspection ReactiveStreamsUnusedPublisher
		launch.finishTestItem(id, buildFinishTestItemRq(context, status));
	}

	/**
	 * Starts a test template (basically a test class)
	 *
	 * @param parentContext JUnit's test context of a parent entity
	 */
	protected void startTemplate(ExtensionContext parentContext) {
		if (!idMapping.containsKey(parentContext)) {
			startTestItem(parentContext, TEMPLATE);
		}
	}

	/**
	 * Starts a test item of arbitrary type
	 *
	 * @param context JUnit's test context
	 * @param type    a type of the item
	 */
	protected void startTestItem(ExtensionContext context, ItemType type) {
		startTestItem(context, Collections.emptyList(), type);
	}

	/**
	 * Starts a test item of arbitrary type
	 *
	 * @param context   JUnit's test context
	 * @param arguments a list of test parameters
	 * @param type      a type of the item
	 */
	protected void startTestItem(ExtensionContext context, List<Object> arguments, ItemType type) {
		startTestItem(context, arguments, type, createStepDescription(context), Calendar.getInstance().getTime());
	}

	/**
	 * Starts a test item of arbitrary type
	 *
	 * @param context     JUnit's test context
	 * @param arguments   a list of test parameters
	 * @param itemType    a type of the item
	 * @param description the test description
	 * @param startTime   the test start time
	 */
	protected void startTestItem(@Nonnull final ExtensionContext context, @Nonnull final List<Object> arguments,
			@Nonnull final ItemType itemType, @Nonnull final String description, @Nonnull final Date startTime) {
		idMapping.computeIfAbsent(context, c -> {
			StartTestItemRQ rq = buildStartStepRq(c, arguments, itemType, description, startTime);
			Launch launch = getLaunch(c);
			Maybe<String> itemId = c.getParent()
					.flatMap(parent -> Optional.ofNullable(idMapping.get(parent)))
					.map(parentTest -> {
						Maybe<String> item = launch.startTestItem(parentTest, rq);
						if (getReporter().getParameters().isCallbackReportingEnabled()) {
							TEST_ITEM_TREE.getTestItems()
									.put(createItemTreeKey(rq.getName()), createTestItemLeaf(parentTest, item));
						}
						return item;
					})
					.orElseGet(() -> {
						Maybe<String> item = launch.startTestItem(rq);
						if (getReporter().getParameters().isCallbackReportingEnabled()) {
							TEST_ITEM_TREE.getTestItems()
									.put(createItemTreeKey(rq.getName()), createTestItemLeaf(item));
						}
						return item;
					});
			if (TEMPLATE == itemType) {
				testTemplates.put(c, itemId);
			}
			return itemId;
		});
	}

	/**
	 * Starts the following methods: <code>@BeforeEach</code>, <code>@AfterEach</code>, <code>@BeforeAll</code> or <code>@AfterAll</code>
	 *
	 * @param method        a method reference
	 * @param parentContext JUnit's test context of a parent item
	 * @param context       JUnit's test context of a method to start
	 * @param itemType      a method's item type (to display on RP)
	 * @return an ID of the method
	 */
	protected Maybe<String> startBeforeAfter(Method method, ExtensionContext parentContext, ExtensionContext context,
			ItemType itemType) {
		Launch launch = getLaunch(context);
		StartTestItemRQ rq = buildStartConfigurationRq(method, parentContext, context, itemType);
		return getItemId(parentContext).map(pid -> launch.startTestItem(pid, rq))
				.orElseGet(() -> launch.startTestItem(rq));
	}

	/**
	 * Finish a test template execution (basically a test class) with a specific status, builds a finish request based on the status
	 *
	 * @param context JUnit's test context
	 */
	protected void finishTemplate(@Nonnull final ExtensionContext context) {
		Launch launch = getLaunch(context);
		Maybe<String> templateId = testTemplates.remove(context);
		//noinspection ReactiveStreamsUnusedPublisher
		launch.finishTestItem(templateId, buildFinishTestItemRq(context, null));
		idMapping.remove(context);
	}

	/**
	 * Finishes a test item in RP, calculates the item status and builds a finish request based on the status
	 *
	 * @param context JUnit's test context
	 */
	protected void finishTestItem(@Nonnull final ExtensionContext context) {
		finishTestItem(context, buildFinishTestItemRq(context, null));
	}

	/**
	 * Finishes a test item in RP with a specific status, builds a finish request based on the status
	 *
	 * @param context JUnit's test context
	 * @param status  a test execution status
	 */
	protected void finishTestItem(@Nonnull final ExtensionContext context, @Nullable final ItemStatus status) {
		finishTestItem(context, buildFinishTestItemRq(context, status));
	}

	/**
	 * Finishes a test item in RP with a custom request
	 *
	 * @param context JUnit's test context
	 * @param rq      a test item finish request
	 */
	protected void finishTestItem(@Nonnull final ExtensionContext context, @Nonnull final FinishTestItemRQ rq) {
		Launch launch = getLaunch(context);
		Maybe<String> id = idMapping.remove(context);
		Maybe<OperationCompletionRS> finishResponse = launch.finishTestItem(id, rq);
		if (getReporter().getParameters().isCallbackReportingEnabled()) {
			ofNullable(TEST_ITEM_TREE.getTestItems()
					.get(createItemTreeKey(context))).ifPresent(itemLeaf -> itemLeaf.setFinishResponse(finishResponse));
		}
	}

	/**
	 * Calculates a test case ID based on code reference and parameters
	 *
	 * @param method    a test method reference
	 * @param codeRef   a code reference which will be used for the calculation
	 * @param arguments a list of test arguments
	 * @param instance  current test instance
	 * @return a test case ID
	 */
	protected TestCaseIdEntry getTestCaseId(@Nonnull final Method method, @Nonnull final String codeRef,
			@Nonnull final List<Object> arguments, @Nullable Object instance) {
		TestCaseId caseId = method.getAnnotation(TestCaseId.class);
		TestCaseIdEntry id = TestCaseIdUtils.getTestCaseId(caseId, method, codeRef, arguments, instance);
		if (id == null) {
			return null;
		}
		return id.getId().endsWith("[]") ? new TestCaseIdEntry(id.getId().substring(0, id.getId().length() - 2)) : id;
	}

	private static String getCodeRef(@Nonnull final Method method) {
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

	/**
	 * Returns a code reference of a test (static or dynamic). For dynamic tests appends each depth level where level names are display
	 * names separated by `$` symbol
	 *
	 * @param context JUnit's test context
	 * @return a code reference string
	 */
	@Nonnull
	protected String getCodeRef(@Nonnull final ExtensionContext context) {
		return getCodeRef(context, "");
	}

	/**
	 * Recursively returns the first real test method found in test hierarchy
	 *
	 * @param context JUnit's test context
	 * @return an {@link Optional} of a {@link Method}
	 */
	protected Optional<Method> getTestMethod(ExtensionContext context) {
		return ofNullable(context.getTestMethod()
				.orElseGet(() -> context.getParent().flatMap(this::getTestMethod).orElse(null)));
	}

	/**
	 * Extract and returns static attributes of a test method or class (set with {@link Attributes} annotation)
	 *
	 * @param annotatedElement a test method or class reference
	 * @return a set of attributes
	 */
	protected @Nonnull Set<ItemAttributesRQ> getAttributes(@Nonnull final AnnotatedElement annotatedElement) {
		return ofNullable(annotatedElement.getAnnotation(Attributes.class)).map(AttributeParser::retrieveAttributes)
				.orElse(Collections.emptySet());
	}

	/**
	 * Extracts and returns a test parameters, respects {@link ParameterKey} annotation
	 *
	 * @param method    a test method reference
	 * @param arguments a list of parameter values
	 * @return a list of parameters
	 */
	protected @Nonnull List<ParameterResource> getParameters(@Nonnull final Method method,
			final List<Object> arguments) {
		return ParameterUtils.getParameters(method, arguments);
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
	@Nonnull
	protected StartTestItemRQ buildStartStepRq(@Nonnull final ExtensionContext context,
			@Nonnull final List<Object> arguments, @Nonnull final ItemType itemType, @Nonnull final String description,
			@Nonnull final Date startTime) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setStartTime(startTime);
		rq.setName(createStepName(context));
		rq.setDescription(description);
		rq.setUniqueId(context.getUniqueId());
		rq.setType(itemType == TEMPLATE ? SUITE.name() : itemType.name());
		String codeRef = getCodeRef(context);
		rq.setCodeRef(codeRef);
		rq.setAttributes(context.getTags()
				.stream()
				.map(it -> new ItemAttributesRQ(null, it))
				.collect(Collectors.toSet()));
		if (SUITE == itemType) {
			context.getTestClass().ifPresent(c -> rq.getAttributes().addAll(getAttributes(c)));
		}

		Optional<Method> testMethod = getTestMethod(context);
		TestCaseIdEntry caseId = testMethod.map(m -> {
			rq.getAttributes().addAll(getAttributes(m));
			rq.setParameters(getParameters(m, arguments));
			return getTestCaseId(m, codeRef, arguments, context.getTestInstance().orElse(null));
		}).orElseGet(() -> TestCaseIdUtils.getTestCaseId(codeRef, arguments));
		rq.setTestCaseId(ofNullable(caseId).map(TestCaseIdEntry::getId).orElse(null));
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
	@Nonnull
	protected StartTestItemRQ buildStartConfigurationRq(@Nonnull Method method, @Nonnull ExtensionContext parentContext,
			@Nonnull ExtensionContext context, @Nonnull ItemType itemType) {
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
		TestCaseIdEntry caseId = ofNullable(method.getAnnotation(TestCaseId.class)).map(TestCaseId::value)
				.map(TestCaseIdEntry::new)
				.orElseGet(() -> TestCaseIdUtils.getTestCaseId(codeRef, Collections.emptyList()));
		rq.setTestCaseId(ofNullable(caseId).map(TestCaseIdEntry::getId).orElse(null));
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
	@Nonnull
	protected FinishTestItemRQ buildFinishTestItemRq(@Nonnull ExtensionContext context, @Nullable ItemStatus status) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		ofNullable(status).ifPresent(s -> rq.setStatus(s.name()));
		rq.setEndTime(Calendar.getInstance().getTime());
		return rq;
	}

	/**
	 * Extension point to customize test step name
	 *
	 * @param context JUnit's test context
	 * @return Test/Step Name being sent to ReportPortal
	 */
	protected String createStepName(ExtensionContext context) {
		String name = context.getDisplayName();
		return name.length() > 1024 ? name.substring(0, 1021) + "..." : name;
	}

	/**
	 * Extension point to customize beforeXXX step name
	 *
	 * @param testClass JUnit's test class, by which the name will be calculated
	 * @param method    JUnit's test method reference
	 * @return Test/Step Name being sent to ReportPortal
	 */
	@Nonnull
	protected String createConfigurationName(@Nonnull Class<?> testClass, @Nonnull Method method) {
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
	 * @param throwable         An exception which caused the skip
	 * @param eventTime         <code>@BeforeEach</code> start time
	 */
	protected void reportSkippedStep(ReflectiveInvocationContext<Method> invocationContext, ExtensionContext context,
			Throwable throwable, Date eventTime) {
		Date skipStartTime = Calendar.getInstance().getTime();
		if (skipStartTime.after(eventTime)) {
			// to fix item ordering when @AfterEach starts in the same millisecond as skipped test
			skipStartTime = new Date(skipStartTime.getTime() - 1);
		}
		startTestItem(context, invocationContext.getArguments(), STEP, createStepDescription(context), skipStartTime);
		createSkippedSteps(context, throwable);
		FinishTestItemRQ finishRq = buildFinishTestItemRq(context, SKIPPED);
		finishRq.setIssue(Launch.NOT_ISSUE);
		finishTestItem(context, finishRq); // an issue relates to @BeforeEach method in this case
	}

	/**
	 * Extension point to customize test steps skipped in case of a <code>@BeforeAll</code> method failed.
	 *
	 * @param invocationContext JUnit's <code>@BeforeAll</code> invocation context
	 * @param context           JUnit's test context
	 * @param eventTime         <code>@BeforeAll</code> start time
	 */
	@SuppressWarnings("unused")
	protected void reportSkippedClassTests(ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext context, Date eventTime) {
	}

	/**
	 * Formats and reports a {@link Throwable} to Report Portal
	 *
	 * @param cause a {@link Throwable}
	 */
	protected void sendStackTraceToRP(final Throwable cause) {
		ReportPortal.emitLog(itemUuid -> {
			SaveLogRQ rq = new SaveLogRQ();
			rq.setItemUuid(itemUuid);
			rq.setLevel("ERROR");
			rq.setLogTime(Calendar.getInstance().getTime());
			if (cause != null) {
				rq.setMessage(getStackTrace(cause));
			} else {
				rq.setMessage("Test has failed without exception");
			}
			rq.setLogTime(Calendar.getInstance().getTime());
			return rq;
		});
	}
}
