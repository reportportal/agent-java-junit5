package com.epam.reportportal.junit5.tests;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.epam.reportportal.junit5.ReportPortalJupiterExtension;
import com.epam.reportportal.junit5.ReportPortalJupiterService;
import com.epam.reportportal.junit5.TestItemType;
import com.epam.reportportal.listeners.Statuses;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.engine.descriptor.MethodExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RPExtensionFailedStepsTests {

    @InjectMocks private static ReportPortalJupiterExtension reportPortalJupiterExtension;
    @Mock private static ReportPortalJupiterService reportPortalService;
    @Mock private static MethodExtensionContext context;
    private static AssertionError exceptionBeforeEach;
    private static AssertionError exceptionAfterEach;
    private static AssertionError exceptionBeforeAll;
    private static int countOfTests = 4;
    private static int countOfDisabledTests = 12;

    @BeforeAll
    static void setup() {
        reportPortalJupiterExtension = new ReportPortalJupiterExtension();
        reportPortalJupiterExtension.setReportPortalService(reportPortalService);
        exceptionBeforeAll = new AssertionError("Before All method failed");
    }

    @BeforeEach
    void beforeEach() {
        exceptionBeforeEach = new AssertionError("Before Each method failed");
    }

    @AfterEach
    void afterEach() {
        exceptionAfterEach = new AssertionError("Before Each method failed");
    }

    @Test
    void testFailedStep() {
        when(context.getExecutionException()).thenReturn(Optional.of(new AssertionError("Test failed")));
        reportPortalJupiterExtension.afterTestExecution(context);
        verify(reportPortalService, times(1)).sendStackTraceToRP(context);
        verify(reportPortalService, times(1)).finishTestItem(context, TestItemType.TEST, Statuses.FAILED);
    }

    @Test
    void testFailedBeforeEachMethod() {
        when(context.getExecutionException()).thenReturn(Optional.of(exceptionBeforeEach));
        when(context.getTestClass()).thenReturn(Optional.of(RPExtensionFailedStepsTests.class));
        reportPortalJupiterExtension.afterEach(context);
        verify(reportPortalService, times(1)).sendStackTraceToRP(context);
        verify(reportPortalService, times(1)).finishTestItem(context, TestItemType.BEFORE_EACH, Statuses.FAILED);
        // The test should be sent to RP as skipped
        verify(reportPortalService, times(1)).startTestItem(context, TestItemType.TEST);
        verify(reportPortalService, times(1)).finishTestItem(context, TestItemType.TEST, Statuses.SKIPPED);
        // After each method should be sent to RP
        verify(reportPortalService, times(1)).startTestItem(context, TestItemType.AFTER_EACH);
        verify(reportPortalService, times(1)).finishTestItem(context, TestItemType.AFTER_EACH, Statuses.PASSED);
    }

    @Test
    void testFailedAfterEachMethod(){
        when(context.getExecutionException()).thenReturn(Optional.of(exceptionAfterEach));
        when(context.getTestClass()).thenReturn(Optional.of(RPExtensionFailedStepsTests.class));
        reportPortalJupiterExtension.afterEach(context);
        verify(reportPortalService, times(1)).startTestItem(context, TestItemType.AFTER_EACH);
        verify(reportPortalService, times(1)).sendStackTraceToRP(context);
        verify(reportPortalService, times(1)).finishTestItem(context, TestItemType.AFTER_EACH, Statuses.FAILED);
    }

    @Test
    void testFailedBeforeAllMethod() {
        when(context.getExecutionException()).thenReturn(Optional.of(exceptionBeforeAll));
        when(context.getTestClass()).thenReturn(Optional.of(RPExtensionFailedStepsTests.class));
        when(context.getElement()).thenReturn(Optional.of(RPExtensionFailedStepsTests.class));
        reportPortalJupiterExtension.afterAll(context);
        verify(reportPortalService, times(1)).sendStackTraceToRP(context);
        verify(reportPortalService, times(1)).finishTestItem(context, TestItemType.BEFORE_ALL, Statuses.FAILED);

        // The following 114 test items from this class should be send to RP as skipped if BeforeAll method is failed
        // Start tests * 3 (BeforeEach, Test, AfterEach)
        verify(reportPortalService, times(countOfTests * 3)).startTestItem(any(), anyString(), anyString());
        // Start disabled tests * 3 (BeforeEach, Test, AfterEach) * 3 (options of parameters)
        verify(reportPortalService, times(countOfDisabledTests * 3 * 3)).startTestItem(any(), anyString(), anyString(), anyString());
        // Finish BeforeEach methods
        verify(reportPortalService, times(countOfTests + (countOfDisabledTests * 3))).finishTestItem(context, TestItemType.BEFORE_EACH, Statuses.SKIPPED);
        // Finish Test methods
        verify(reportPortalService, times(countOfTests + (countOfDisabledTests * 3))).finishTestItem(context, TestItemType.TEST, Statuses.SKIPPED);
        // Finish AfterEach methods
        verify(reportPortalService, times(countOfTests + (countOfDisabledTests * 3))).finishTestItem(context, TestItemType.AFTER_EACH, Statuses.SKIPPED);

        // Start and finish item for AfterAll method
        verify(reportPortalService, times(1)).startTestItem(context, TestItemType.AFTER_ALL);
        verify(reportPortalService, times(1)).finishTestItem(context, TestItemType.AFTER_ALL, Statuses.PASSED);
        // Finish Suite
        verify(reportPortalService, times(1)).finishSuite(context, Statuses.FAILED);
    }

    @AfterAll
    static void testFailedAfterAllMethod() {
        when(context.getExecutionException()).thenReturn(Optional.of(new AssertionError("After All method failed")));
        when(context.getTestClass()).thenReturn(Optional.of(RPExtensionFailedStepsTests.class));
        when(context.getElement()).thenReturn(Optional.of(RPExtensionFailedStepsTests.class));
        reportPortalJupiterExtension.afterAll(context);
        verify(reportPortalService, times(1)).finishTestItem(context, TestItemType.BEFORE_ALL, Statuses.PASSED);
        // Start tests * 3 (BeforeEach, Test, AfterEach) * 3 (options of parameters)
        verify(reportPortalService, times(countOfDisabledTests * 3 * 3)).startTestItem(any(), anyString(), anyString(), anyString());
        // Finish BeforeEach methods
        verify(reportPortalService, times(countOfDisabledTests * 3)).finishTestItem(context, TestItemType.BEFORE_EACH, Statuses.SKIPPED);
        // Finish Test methods
        verify(reportPortalService, times(countOfDisabledTests * 3)).finishTestItem(context, TestItemType.TEST, Statuses.SKIPPED);
        // Finish AfterEach methods
        verify(reportPortalService, times(countOfDisabledTests * 3)).finishTestItem(context, TestItemType.AFTER_EACH, Statuses.SKIPPED);

        verify(reportPortalService, times(1)).startTestItem(context, TestItemType.AFTER_ALL);
        verify(reportPortalService, times(1)).finishTestItem(context, TestItemType.AFTER_ALL, Statuses.FAILED);
        verify(reportPortalService, times(1)).finishSuite(context, Statuses.FAILED);
    }

    private static List<String> testData() {
        return Arrays.asList(
            "first", "second", "third"
        );
    }

    @Disabled("To test addDisabledTestsIfExists and getCountOfTests")
    @ParameterizedTest
    @MethodSource("testData")
    void parameterizedTestWithMethodSource(String value) {
        System.out.println("parameterizedTestWithMethodSource, parameter: " + value);
    }

    @Disabled("To test addDisabledTestsIfExists and getCountOfTests")
    @ParameterizedTest
    @CsvSource({"first", "second", "third"})
    void parameterizedTestWithCsvSource(String value) {
        System.out.println("parameterizedTestWithCsvSource, parameter: " + value);
    }

    @Disabled("To test addDisabledTestsIfExists and getCountOfTests")
    @ParameterizedTest
    @EnumSource(value = DayOfWeek.class, names = {"MONDAY", "WEDNESDAY", "FRIDAY"})
    void parameterizedTestWithEnumSource(DayOfWeek day) {
        System.out.println("parameterizedTestWithEnumSource, parameter: " + day);
    }

    @Disabled("To test addDisabledTestsIfExists and getCountOfTests")
    @ParameterizedTest
    @ValueSource(floats = {1.1f, 2.2f, 3.3f})
    void parameterizedTestWithFloatsValueSource(float value) {
        System.out.println("parameterizedTestWithFloatsValueSource, parameter: " + value);
    }

    @Disabled("To test addDisabledTestsIfExists and getCountOfTests")
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void parameterizedTestWithIntValueSource(int value) {
        System.out.println("parameterizedTestWithIntValueSource, parameter: " + value);
    }

    @Disabled("To test addDisabledTestsIfExists and getCountOfTests")
    @ParameterizedTest
    @ValueSource(shorts = {1, 2, 3})
    void parameterizedTestWithShortsValueSource(short value) {
        System.out.println("parameterizedTestWithShortsValueSource, parameter: " + value);
    }

    @Disabled("To test addDisabledTestsIfExists and getCountOfTests")
    @ParameterizedTest
    @ValueSource(bytes = {1, 2, 3})
    void parameterizedTestWithBytesValueSource(byte value) {
        System.out.println("parameterizedTestWithBytesValueSource, parameter: " + value);
    }

    @Disabled("To test addDisabledTestsIfExists and getCountOfTests")
    @ParameterizedTest
    @ValueSource(chars = {'a', 'b', 'c'})
    void parameterizedTestWithCharsValueSource(char value) {
        System.out.println("parameterizedTestWithCharsValueSource, parameter: " + value);
    }

    @Disabled("To test addDisabledTestsIfExists and getCountOfTests")
    @ParameterizedTest
    @ValueSource(doubles = {1.1, 2.2, 3.3})
    void parameterizedTestWithDoublesValueSource(double value) {
        System.out.println("parameterizedTestWithDoublesValueSource, parameter: " + value);
    }

    @Disabled("To test addDisabledTestsIfExists and getCountOfTests")
    @ParameterizedTest
    @ValueSource(longs = {1147483648L, 2147483648L, 3147483648L})
    void parameterizedTestWithLongsValueSource(long value) {
        System.out.println("parameterizedTestWithLongsValueSource, parameter: " + value);
    }

    @Disabled("To test addDisabledTestsIfExists and getCountOfTests")
    @ParameterizedTest
    @ValueSource(strings = {"a", "b", "c"})
    void parameterizedTestWithStringsValueSource(String value) {
        System.out.println("parameterizedTestWithStringsValueSource, parameter: " + value);
    }

    @Disabled("To test addDisabledTestsIfExists and getCountOfTests")
    @RepeatedTest(value = 3)
    void repeatedTest(String value) {
        System.out.println("repeatedTest, parameter: " + value);
    }
}
