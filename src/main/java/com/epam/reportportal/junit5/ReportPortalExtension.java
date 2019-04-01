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

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

/*
 * ReportPortal Listener sends the results of test execution to ReportPortal in RealTime
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.platform.commons.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.reactivex.Maybe;

import static java.util.Optional.ofNullable;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotatedMethods;
import static rp.com.google.common.base.Throwables.getStackTraceAsString;

public class ReportPortalExtension
    implements Extension, BeforeAllCallback, BeforeEachCallback, BeforeTestExecutionCallback,
               AfterTestExecutionCallback, AfterEachCallback, AfterAllCallback, TestWatcher {

    private static final String TEST_TEMPLATE_EXTENSION_CONTEXT =
        "org.junit.jupiter.engine.descriptor.TestTemplateExtensionContext";
    private static final ConcurrentMap<String, Launch> launchMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Maybe<String>> idMapping = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Maybe<String>> testTemplates = new ConcurrentHashMap<>();
    private ThreadLocal<Boolean> isDisabledTest = new ThreadLocal<>();

    private static synchronized Launch getLaunch(ExtensionContext context) {
        String launchId = context.getRoot().getUniqueId();
        if (!launchMap.containsKey(launchId)) {
            ReportPortal rp = ReportPortal.builder().build();
            ListenerParameters params = rp.getParameters();
            StartLaunchRQ rq = new StartLaunchRQ();
            rq.setMode(params.getLaunchRunningMode());
            rq.setDescription(params.getDescription());
            rq.setName(params.getLaunchName());
            rq.setTags(params.getTags());
            rq.setStartTime(Calendar.getInstance().getTime());
            Launch launch = rp.newLaunch(rq);
            launchMap.put(launchId, launch);
            Runtime.getRuntime().addShutdownHook(getShutdownHook(launch));
            launch.start();
        }
        return launchMap.get(launchId);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        startTestItem(context, "SUITE");
        reportBeforeAfter(context, BeforeAll.class, "BEFORE_CLASS");
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        isDisabledTest.set(false);
        startTemplate(context);
        reportBeforeAfter(context, BeforeEach.class, "BEFORE_METHOD");
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
        reportBeforeAfter(context, AfterEach.class, "AFTER_METHOD");
    }

    @Override
    public void afterAll(ExtensionContext context) {
        finishTestTemplates(context);
        finishTestItem(context);
        reportBeforeAfter(context, AfterAll.class, "AFTER_CLASS");
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        isDisabledTest.set(true);
        String description = reason.orElse(context.getDisplayName());
        startTestItem(context, "STEP", description);
        finishTestItem(context);
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

    private synchronized void startTemplate(ExtensionContext context) {
        Optional<ExtensionContext> parent = context.getParent();
        if ((parent.isPresent() && TEST_TEMPLATE_EXTENSION_CONTEXT.equals(parent.get().getClass().getName()))) {
            if (!idMapping.containsKey(parent.get().getUniqueId())) {
                startTestItem(context.getParent().get(), "TEMPLATE");
                System.out.println(">>>>>Start repeated method");
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
        Launch launch = getLaunch(context);
        StartTestItemRQ rq = new StartTestItemRQ();
        rq.setStartTime(Calendar.getInstance().getTime());
        rq.setName(context.getDisplayName());
        rq.setDescription(null != reason ? reason : context.getDisplayName());
        rq.setUniqueId(context.getUniqueId());
        rq.setType(type);
        rq.setRetry(false);
        ofNullable(context.getTags()).ifPresent(rq::setTags);

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

    private synchronized void reportBeforeAfter(
        ExtensionContext context, Class<? extends Annotation> annotationType, String itemType) {
        Launch launch = getLaunch(context);

        List<Method> methods = findAnnotatedMethods(context.getTestClass().get(),
            annotationType, ReflectionUtils.HierarchyTraversalMode.TOP_DOWN);
        for (Method method : methods) {
            StartTestItemRQ rq = new StartTestItemRQ();
            rq.setStartTime(Calendar.getInstance().getTime());
            rq.setName(method.getName());
            rq.setDescription(method.getName());

            String uniqueId;
            String parentId;
            if (itemType.equals("AFTER_METHOD") | itemType.equals("BEFORE_METHOD")) {
                parentId = context.getParent().get().getUniqueId();
                uniqueId = parentId + "/[method:" + method.getName() + "()]";
            } else {
                parentId = context.getUniqueId();
                uniqueId = parentId + "/[method:" + method.getName() + "()]";
            }

            rq.setUniqueId(uniqueId);
            ofNullable(context.getTags()).ifPresent(rq::setTags);
            rq.setType(itemType);
            rq.setRetry(false);

            Maybe<String> itemId = launch.startTestItem(idMapping.get(parentId), rq);
            idMapping.put(uniqueId, itemId);

            FinishTestItemRQ rq1 = new FinishTestItemRQ();
            rq1.setStatus(getExecutionStatus(context));
            rq1.setEndTime(Calendar.getInstance().getTime());
            launch.finishTestItem(idMapping.get(uniqueId), rq1);
        }
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
}
