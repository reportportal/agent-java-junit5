package com.epam.reportportal.junit5;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.epam.reportportal.listeners.Statuses;

import rp.com.google.common.base.Suppliers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class ReportPortalJupiterExtension
    implements BeforeAllCallback, BeforeEachCallback, BeforeTestExecutionCallback, AfterTestExecutionCallback, AfterEachCallback, AfterAllCallback {

    private ReportPortalJupiterService reportPortalService;
    private static final Supplier<ReportPortalJupiterService> SERVICE = Suppliers.memoize(ReportPortalJupiterService::new)::get;

    public ReportPortalJupiterExtension() {
        this.reportPortalService = SERVICE.get();
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        reportPortalService.startLaunchIfRequired();
        reportPortalService.startSuite(context);
        reportPortalService.startTestItem(context, TestItemType.BEFORE_ALL);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        reportPortalService.startTestItem(context, TestItemType.BEFORE_EACH);
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        reportPortalService.finishTestItem(context, TestItemType.BEFORE_EACH, Statuses.PASSED);
        reportPortalService.startTestItem(context, TestItemType.TEST);
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (context.getExecutionException().isPresent()) {
            reportPortalService.sendStackTraceToRP(context);
            reportPortalService.finishTestItem(context, TestItemType.TEST, Statuses.FAILED);
        } else {
            reportPortalService.finishTestItem(context, TestItemType.TEST, Statuses.PASSED);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (context.getExecutionException().isPresent()) {
            List<Class> annotations = getAnnotationsOfFailedNonTestMethod(context);
            if (annotations.contains(BeforeEach.class)) {
                reportPortalService.sendStackTraceToRP(context);
                reportPortalService.finishTestItem(context, TestItemType.BEFORE_EACH, Statuses.FAILED);
                reportPortalService.startTestItem(context, TestItemType.TEST);
                reportPortalService.finishTestItem(context, TestItemType.TEST, Statuses.SKIPPED);
            }
        }
        reportPortalService.startTestItem(context, TestItemType.AFTER_EACH);
        if (context.getExecutionException().isPresent()) {
            List<Class> annotations = getAnnotationsOfFailedNonTestMethod(context);
            Throwable[] suppressedExceptions = context.getExecutionException().get().getSuppressed();
            if (annotations.contains(AfterEach.class) || suppressedExceptions.length > 0) {
                reportPortalService.sendStackTraceToRP(context);
                reportPortalService.finishTestItem(context, TestItemType.AFTER_EACH, Statuses.FAILED);
            } else {
                reportPortalService.finishTestItem(context, TestItemType.AFTER_EACH, Statuses.PASSED);
            }
        } else {
            reportPortalService.finishTestItem(context, TestItemType.AFTER_EACH, Statuses.PASSED);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (context.getExecutionException().isPresent()
            && getAnnotationsOfFailedNonTestMethod(context).contains(BeforeAll.class)) {
            reportPortalService.sendStackTraceToRP(context);
            reportPortalService.finishTestItem(context, TestItemType.BEFORE_ALL, Statuses.FAILED);
            skipAllTestInClass(context);
        } else {
            reportPortalService.finishTestItem(context, TestItemType.BEFORE_ALL, Statuses.PASSED);
        }
        addDisabledTestsIfExists(context);
        reportPortalService.startTestItem(context, TestItemType.AFTER_ALL);
        if (context.getExecutionException().isPresent()) {
            if (getAnnotationsOfFailedNonTestMethod(context).contains(AfterAll.class)
                || context.getExecutionException().get().getSuppressed().length > 0) {
                reportPortalService.sendStackTraceToRP(context);
                reportPortalService.finishTestItem(context, TestItemType.AFTER_ALL, Statuses.FAILED);
            } else {
                reportPortalService.finishTestItem(context, TestItemType.AFTER_ALL, Statuses.PASSED);
            }
            reportPortalService.finishSuite(context, Statuses.FAILED);
        } else {
            reportPortalService.finishTestItem(context, TestItemType.AFTER_ALL, Statuses.PASSED);
            reportPortalService.finishSuite(context, Statuses.PASSED);
        }
    }

    private void addDisabledTestsIfExists(ExtensionContext context) {
        if (!context.getTestClass().isPresent()) return;
        for (Method method : context.getTestClass().get().getDeclaredMethods()) {
            List<Class> annotations = Arrays.stream(method.getAnnotations()).map(Annotation::annotationType).collect(Collectors.toList());
            if (annotations.contains(Disabled.class)) {
                String testDescription = method.getAnnotation(Disabled.class).value();
                for (int i = 1; i <= getCountOfTests(annotations, method); i++) {
                    String suffixForTestName = (getCountOfTests(annotations, method) > 1) ? "[" + i + "]" : "";
                    String testName = method.getName() + "()" + suffixForTestName;
                    reportPortalService.startTestItem(context, TestItemType.BEFORE_EACH, testName, testDescription);
                    reportPortalService.finishTestItem(context, TestItemType.BEFORE_EACH, Statuses.SKIPPED);
                    reportPortalService.startTestItem(context, TestItemType.TEST, testName, testDescription);
                    reportPortalService.finishTestItem(context, TestItemType.TEST, Statuses.SKIPPED);
                    reportPortalService.startTestItem(context, TestItemType.AFTER_EACH, testName, testDescription);
                    reportPortalService.finishTestItem(context, TestItemType.AFTER_EACH, Statuses.SKIPPED);
                }
            }
        }
    }

    private void skipAllTestInClass(ExtensionContext context) {
        if (!context.getElement().isPresent()) return;
        for (Method method : ((Class) context.getElement().get()).getDeclaredMethods()) {
            List<Class> annotations = Arrays.stream(method.getAnnotations()).map(Annotation::annotationType).collect(Collectors.toList());
            if ((annotations.contains(Test.class)
                 || annotations.contains(ParameterizedTest.class)
                 || annotations.contains(RepeatedTest.class))
                && !annotations.contains(Disabled.class)) {
                for (int i = 1; i <= getCountOfTests(annotations, method); i++) {
                    String suffixForTestName = (getCountOfTests(annotations, method) > 1) ? "[" + i + "]" : "";
                    String testName = method.getName() + "()" + suffixForTestName;
                    reportPortalService.startTestItem(context, TestItemType.BEFORE_EACH, testName);
                    reportPortalService.finishTestItem(context, TestItemType.BEFORE_EACH, Statuses.SKIPPED);
                    reportPortalService.startTestItem(context, TestItemType.TEST, testName);
                    reportPortalService.finishTestItem(context, TestItemType.TEST, Statuses.SKIPPED);
                    reportPortalService.startTestItem(context, TestItemType.AFTER_EACH, testName);
                    reportPortalService.finishTestItem(context, TestItemType.AFTER_EACH, Statuses.SKIPPED);
                }
            }
        }
    }

    private int getCountOfTests(List<Class> annotations, Method testMethod) {
        int totalRepetitions = 0;
        if (annotations.contains(Test.class)) {
            totalRepetitions = 1;
        } else if (annotations.contains(RepeatedTest.class)) {
            totalRepetitions = testMethod.getAnnotation(RepeatedTest.class).value();
        } else if (annotations.contains(MethodSource.class)) {
            try {
                Object result = findMethodForMethodSourceAnnotation(testMethod).invoke(null);
                if (result instanceof Stream) {
                    totalRepetitions = (int) ((Stream) result).count();
                } else if (result instanceof List) {
                    totalRepetitions = ((List) result).size();
                }
            }
            catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } else if (annotations.contains(EnumSource.class)) {
            totalRepetitions = testMethod.getAnnotation(EnumSource.class).names().length;
        } else if (annotations.contains(ValueSource.class)) {
            ValueSource valueSource = testMethod.getAnnotation(ValueSource.class);
            List<Integer> countsOfValues = Arrays.asList(valueSource.floats().length,
                                                         valueSource.ints().length,
                                                         valueSource.shorts().length,
                                                         valueSource.bytes().length,
                                                         valueSource.chars().length,
                                                         valueSource.doubles().length,
                                                         valueSource.longs().length,
                                                         valueSource.strings().length);
            totalRepetitions = Collections.max(countsOfValues);
        } else if (annotations.contains(CsvSource.class)) {
            totalRepetitions = testMethod.getAnnotation(CsvSource.class).value().length;
        }
        return totalRepetitions;
    }

    private Method findMethodForMethodSourceAnnotation(Method testMethod) {
        String testDataMethodName = testMethod.getAnnotation(MethodSource.class).value()[0];
        Method testDataMethod = null;
        Class<?> clazz = testMethod.getDeclaringClass();
        do {
            try {
                testDataMethod = clazz.getDeclaredMethod(testDataMethodName);
                testDataMethod.setAccessible(true);
                break;
            }
            catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        }
        while (clazz != null);
        return testDataMethod;
    }

    private List<Class> getAnnotationsOfFailedNonTestMethod(ExtensionContext context) {
        Class<?> testClass = context.getTestClass().get();
        String failedMethodName = "";
        search:
        {
            do {
                for (StackTraceElement stackTraceElement : context.getExecutionException().get().getStackTrace()) {
                    if (stackTraceElement.getClassName().equals(testClass.getName())) {
                        failedMethodName = stackTraceElement.getMethodName();
                        break search;
                    }
                }
                testClass = testClass.getSuperclass();
            }
            while (testClass != null);
        }
        try {
            Method failedMethod = testClass.getDeclaredMethod(failedMethodName);
            return Arrays.stream(failedMethod.getAnnotations()).map(Annotation::annotationType).collect(Collectors.toList());
        }
        catch (NoSuchMethodException e) {
            return new ArrayList<>();
        }
    }

    public void setReportPortalService(ReportPortalJupiterService reportPortalService) {
        this.reportPortalService = reportPortalService;
    }
}
