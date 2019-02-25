/*
 * Copyright 2018 EPAM Systems
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

import static java.util.Optional.ofNullable;
import static rp.com.google.common.base.Throwables.getStackTraceAsString;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.opentest4j.TestAbortedException;

/**
 * ReportPortal Listener sends the results of test execution to ReportPortal in RealTime
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
public class ReportPortalExtension
    implements BeforeAllCallback, BeforeTestExecutionCallback, AfterTestExecutionCallback, AfterAllCallback {

    /** map to associate root execution contexts with launches */
    private static final Map<String, Launch> launchMap = new HashMap<>();
    /** map to associate context IDs with test item IDs */
    private final ConcurrentMap<String, Maybe<String>> idMapping = new ConcurrentHashMap<>();
    /** map to associate template test parent IDs to template test suite objects */
    private final ConcurrentMap<String, TemplateTestSuite> templateTestSuites = new ConcurrentHashMap<>();
    /** fully-qualified class name for test template extension context */
    private static final String TEST_TEMPLATE_EXTENSION_CONTEXT = "org.junit.jupiter.engine.descriptor.TestTemplateExtensionContext";

    /**
     * Create a {@link Thread} object that encapsulates the implementation to finish the specified launch object.
     *
     * @param launch launch object
     * @return thread object that will finish the specified launch object
     */
    private static Thread getShutdownHook(final Launch launch) {
        return new Thread(() -> {
            FinishExecutionRQ rq = new FinishExecutionRQ();
            rq.setEndTime(Calendar.getInstance().getTime());
            launch.finish(rq);
        });
    }

    /**
     * If not already done, start launch for the root context of the specified extension context.
     *
     * @param context extension context
     * @return launch ID associated with the corresponding root context
     */
    private static synchronized Launch startLaunchIfRequiredFor(ExtensionContext context) {
        // get unique ID for root context
        String launchId = context.getRoot().getUniqueId();
        // if no launch exists for this unique ID
        if (!launchMap.containsKey(launchId)) {
            // instantiate a new ReportPortal object
            ReportPortal rp = ReportPortal.builder().build();
            // get ReportPortal configuration parameters
            ListenerParameters params = rp.getParameters();
            // instantiate "start launch" request
            StartLaunchRQ rq = new StartLaunchRQ();
            // set launch running mode
            rq.setMode(params.getLaunchRunningMode());
            // set launch description
            rq.setDescription(params.getDescription());
            // set launch name
            rq.setName(params.getLaunchName());
            // set launch tags
            rq.setTags(params.getTags());
            // set launch start time
            rq.setStartTime(Calendar.getInstance().getTime());
            // instantiate specified launch
            Launch launch = rp.newLaunch(rq);
            // store context launch mapping
            launchMap.put(launchId, launch);
            // add shutdown hook to finish launch when tests complete
            Runtime.getRuntime().addShutdownHook(getShutdownHook(launch));
            // start launch
            launch.start();
        }
        // return launch for context
        return launchMap.get(launchId);
    }

    /**
     * Get launch object for the specified extension context.
     *
     * @param context extension context
     * @return launch object for the specified extension context
     */
    private static synchronized Launch getLaunchFor(ExtensionContext context) {
        // get unique ID for root context
        String launchId = context.getRoot().getUniqueId();
        // return launch object for context
        return launchMap.get(launchId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // if not already done, start launch for context
        Launch launch = startLaunchIfRequiredFor(context);
        // start suite item for this context
        startTestItem(context, launch, "SUITE");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        // if not a test template initialization hook
        if (!isTestTemplateInitializationHook(context)) {
            // start test item for this context
            startTestItem(context, getLaunchFor(context), "STEP");
        }
    }

    /**
     * Start a test item of the specified type.
     *
     * @param context test item context
     * @param launch  launch for test item
     * @param type    test item type (either {@code TEST} or {@code SUITE})
     * @return test item ID
     */
    private Maybe<String> startTestItem(ExtensionContext context, Launch launch, String type) {
        // instantiate "start test item" request
        StartTestItemRQ rq = new StartTestItemRQ();
        String name = context.getDisplayName();
        // The maximum length of TestItem name is 256 characters
        rq.setName(name.length() > 256 ? name.substring(0, 200) + "..." : name);
        // set test item description
        rq.setDescription(context.getDisplayName());
        // set test item unique ID
        rq.setUniqueId(context.getUniqueId());
        // set test item type
        rq.setType(type);
        // test item is not a retry
        rq.setRetry(false);

        // if present, set test item tags
        ofNullable(context.getTags()).ifPresent(rq::setTags);

        Maybe<String> itemId;
        // if template test invocation
        if (isTemplateTestInvocation(context)) {
            // if not yet done, start containing suite
            Maybe<String> suiteId = startSuiteIfRequiredFor(context);
            // set test item start time
            rq.setStartTime(Calendar.getInstance().getTime());
            // start test item for this invocation
            itemId = launch.startTestItem(suiteId, rq);
            // otherwise (not template test invocation)
        } else {
            // set test item start time
            rq.setStartTime(Calendar.getInstance().getTime());
            // get context parent
            itemId = context.getParent()
                            // get unique ID of context parent
                            .map(ExtensionContext::getUniqueId)
                            // get test item ID for parent ID (may be empty)
                            .map(parentId -> ofNullable(idMapping.get(parentId)))
                            // if non-empty, start child test item
                            .map(parentItemId -> launch.startTestItem(parentItemId.orElse(null), rq))
                            // otherwise, start root test item
                            .orElseGet(() -> launch.startTestItem(rq));
        }
        // store association: context => test item ID
        idMapping.put(context.getUniqueId(), itemId);
        return itemId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        // if not test template
        if (!isTestTemplateInitializationHook(context)) {
            // finish test item
            finishTestItem(context);
            // if template test invocation
            if (isTemplateTestInvocation(context)) {
                // if this is the final repetition
                if (getTestSuiteFor(context).registerRepetition()) {
                    // finish containing suite
                    finishTestSuiteFor(context);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        // finish context suite
        finishTestItem(context);
    }

    /**
     * Finish test item for the specified extension context
     *
     * @param context extension context
     */
    private void finishTestItem(ExtensionContext context) {
        // instantiate "finish test item" request
        FinishTestItemRQ rq = new FinishTestItemRQ();
        // set test item status
        rq.setStatus(getExecutionStatus(context));
        // set test item end time
        rq.setEndTime(Calendar.getInstance().getTime());
        // finish test item for extension context, remove mapping [context => test item]
        getLaunchFor(context).finishTestItem(idMapping.remove(context.getUniqueId()), rq);
    }

    /**
     * Get execution status for the specified extension context.
     *
     * @param context extension context
     * @return {@link Statuses status constant}
     */
    private static String getExecutionStatus(ExtensionContext context) {
        Optional<Throwable> exception = context.getExecutionException();
        if (!exception.isPresent()) {
            return Statuses.PASSED;
        } else if (exception.get() instanceof TestAbortedException) {
            return Statuses.SKIPPED;
        } else {
            sendStackTraceToRP(exception.get());
            return Statuses.FAILED;
        }
    }

    private static void sendStackTraceToRP(final Throwable cause) {

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

    /**
     * If not already done, start a containing suite for the specified template test invocation.
     *
     * @param context extension context for template test invocation
     * @return test item ID for containing suite (may be {@code empty})
     */
    private synchronized Maybe<String> startSuiteIfRequiredFor(ExtensionContext context) {
        TemplateTestSuite testSuite;
        Maybe<String> suiteId = Maybe.empty();
        // if template test invocation
        if (isTemplateTestInvocation(context)) {
            // get unique ID of context parent
            String parentId = context.getParent().get().getUniqueId();
            // if containing suite already started
            if (templateTestSuites.containsKey(parentId)) {
                // get containing template test suite object
                testSuite = templateTestSuites.get(parentId);
                // otherwise (suite not yet started)
            } else {
                // start containing template test suite
                testSuite = TemplateTestSuite.startTestSuiteFor(context, this);
                // store association: parent context => suite
                templateTestSuites.put(parentId, testSuite);
            }
            // get test item ID for suite
            suiteId = testSuite.getSuiteId();
        }
        return suiteId;
    }

    /**
     * Get containing suite for the specified template test invocation.
     *
     * @param context extension context for template test invocation
     * @return containing template test suite object (may be {@code null})
     */
    private TemplateTestSuite getTestSuiteFor(ExtensionContext context) {
        // if template test invocation
        if (isTemplateTestInvocation(context)) {
            // return containing template test suite object
            return templateTestSuites.get(context.getParent().get().getUniqueId());
        }
        return null;
    }

    /**
     * Finish containing suite for the specified template test invocation.
     *
     * @param context extension context for template test invocation
     * @return containing template test suite object (may be {@code null})
     */
    private synchronized TemplateTestSuite finishTestSuiteFor(ExtensionContext context) {
        // if template test invocation
        if (isTemplateTestInvocation(context)) {
            // get context parent
            ExtensionContext parent = context.getParent().get();
            // finish containing template test suite
            finishTestItem(parent);
            // remove/return containing template test suite object
            return templateTestSuites.remove(parent.getUniqueId());
        }
        return null;
    }

    /**
     * Determine if the specified extension context is a test template initialization hook.
     *
     * @param context extension context
     * @return {@code true} if specified context is a test template initialization hook; otherwise {@code false}
     */
    private static boolean isTestTemplateInitializationHook(ExtensionContext context) {
        // if specified context is method extension
        if (context.getTestMethod().isPresent()) {
            // get context parent
            Optional<ExtensionContext> parent = context.getParent();
            // if this is a test template initialization hook
            if (parent.isPresent() && !isTemplateTestInvocation(context)) {
                // get method object for test template method
                Method testMethod = context.getRequiredTestMethod();
                // get RepeatedTest annotation, if present
                RepeatedTest repeated = testMethod.getDeclaredAnnotation(RepeatedTest.class);
                // get ParameterizedTest annotation, if present
                ParameterizedTest parameterized = testMethod.getDeclaredAnnotation(ParameterizedTest.class);
                // indicate if test is repeated or parameterized
                return ((repeated != null) || (parameterized != null));
            }
        }
        return false;
    }

    /**
     * Determine if the specified extension context is a test template invocation.
     *
     * @param context extension context
     * @return {@code true} if specified context is a template test invocation; otherwise {@code false}
     */
    private static boolean isTemplateTestInvocation(ExtensionContext context) {
        // if specified context is method extension
        if (context.getTestMethod().isPresent()) {
            // get context parent
            Optional<ExtensionContext> parent = context.getParent();
            // indicate if this is a test template invocation
            return (parent.isPresent() && TEST_TEMPLATE_EXTENSION_CONTEXT.equals(parent.get().getClass().getName()));
        }
        return false;
    }

    /**
     * Instances of this class encapsulate the suite ID and repetition info for a collection of repeated tests.
     */
    private static class TemplateTestSuite {

        private Maybe<String> suiteId;
        private int totalRepetitions;
        private int totalCompletions = 0;

        /**
         * Constructor: Instantiate suite info object for the specified test template and suite ID.
         *
         * @param context test template context
         * @param suiteId ID of containing template test suite
         */
        TemplateTestSuite(ExtensionContext context, Maybe<String> suiteId) {
            this.suiteId = suiteId;
            totalRepetitions = getRepetitionsCount(context);
        }

        /**
         * Returns repetitions count for different types of {@code ArgumentsSource}
         *
         * @param context template test context
         * @return int repetitions count
         */
        private int getRepetitionsCount(ExtensionContext context) {
            int repetitionsCount = 0;
            Method testMethod = context.getRequiredTestMethod();

            List<Annotation> sourceAnnotations =
                Arrays.stream(testMethod.getAnnotations())
                      .filter(annotation -> annotation.annotationType().isAnnotationPresent(ArgumentsSource.class))
                      .collect(Collectors.toList());

            if (sourceAnnotations.isEmpty()) {
                repetitionsCount = 1;
            }
            if (testMethod.isAnnotationPresent(NullAndEmptySource.class)) {
                repetitionsCount = 2;
            }

            for (Annotation annotation : sourceAnnotations) {
                String providerClassName = annotation.annotationType()
                                                     .getAnnotation(ArgumentsSource.class)
                                                     .value().getName();
                try {
                    Constructor providerConstructor = Class.forName(providerClassName).getDeclaredConstructor();
                    providerConstructor.setAccessible(true);
                    Object provider = providerConstructor.newInstance();
                    ((Consumer) provider).accept(annotation);
                    repetitionsCount += (int) ((ArgumentsProvider) provider).provideArguments(context).count();
                } catch (Exception e) {
                    repetitionsCount += 1;
                }
            }

            if (testMethod.isAnnotationPresent(RepeatedTest.class)) {
                repetitionsCount *= testMethod.getAnnotation(RepeatedTest.class).value();
            }
            return repetitionsCount;
        }

        /**
         * Start containing suite for the specified template test context for the specified Report Portal extension.
         *
         * @param context   template test context
         * @param extension Report Portal extension
         * @return TemplateTestSuite object
         */
        public static TemplateTestSuite startTestSuiteFor(ExtensionContext context, ReportPortalExtension extension) {
            // get context parent
            ExtensionContext parent = context.getParent().get();
            Maybe<String> suiteId = extension.startTestItem(parent, getLaunchFor(parent), "SUITE");
            return new TemplateTestSuite(context, suiteId);
        }

        /**
         * Get ID of this template test suite object.
         *
         * @return ID of this template test suite object
         */
        public Maybe<String> getSuiteId() {
            return suiteId;
        }

        /**
         * Register the completion of a template test invocation.
         *
         * @return {@code true} if all repetitions have completed; otherwise {@code false}
         */
        public synchronized boolean registerRepetition() {
            if (totalCompletions < totalRepetitions) {
                totalCompletions++;
            }
            return totalCompletions >= totalRepetitions;
        }
    }
}
