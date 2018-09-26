package com.epam.reportportal.junit5.tests;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.epam.reportportal.junit5.ReportPortalJupiterExtension;
import com.epam.reportportal.junit5.ReportPortalJupiterService;
import com.epam.reportportal.junit5.TestItemType;
import com.epam.reportportal.listeners.Statuses;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.engine.descriptor.MethodExtensionContext;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RPExtensionAllStepsPassedTests {

    @InjectMocks private static ReportPortalJupiterExtension reportPortalJupiterExtension;
    @Mock private static ReportPortalJupiterService reportPortalService;
    @Mock private static MethodExtensionContext context;

    @BeforeAll
    static void setup() {
        reportPortalService = Mockito.mock(ReportPortalJupiterService.class);
        reportPortalJupiterExtension = new ReportPortalJupiterExtension();
        reportPortalJupiterExtension.setReportPortalService(reportPortalService);
    }

    @Test
    void testBeforeAll() {
        reportPortalJupiterExtension.beforeAll(context);
        verify(reportPortalService, times(1)).startLaunchIfRequired();
        verify(reportPortalService, times(1)).startSuite(context);
        verify(reportPortalService, times(1)).startTestItem(context, TestItemType.BEFORE_ALL);
    }

    @Test
    void testBeforeEach() {
        reportPortalJupiterExtension.beforeEach(context);
        verify(reportPortalService, times(1)).startTestItem(context, TestItemType.BEFORE_EACH);
    }

    @Test
    void testStep() {
        reportPortalJupiterExtension.beforeTestExecution(context);
        verify(reportPortalService, times(1)).finishTestItem(context, TestItemType.BEFORE_EACH, Statuses.PASSED);
        verify(reportPortalService, times(1)).startTestItem(context, TestItemType.TEST);
    }

    @Test
    void testFailedStep() {
        when(context.getExecutionException()).thenReturn(Optional.of(new AssertionError("Test failed")));
        reportPortalJupiterExtension.afterTestExecution(context);
        verify(reportPortalService, times(1)).sendStackTraceToRP(context);
        verify(reportPortalService, times(1)).finishTestItem(context, TestItemType.TEST, Statuses.FAILED);
    }

    @Test
    void testAfterEach() {
        reportPortalJupiterExtension.afterEach(context);
        verify(reportPortalService, times(1)).startTestItem(context, TestItemType.AFTER_EACH);
        verify(reportPortalService, times(1)).finishTestItem(context, TestItemType.AFTER_EACH, Statuses.PASSED);
    }

    @Test
    void testAfterAll() {
        reportPortalJupiterExtension.afterAll(context);
        verify(reportPortalService, times(1)).finishTestItem(context, TestItemType.BEFORE_ALL, Statuses.PASSED);
        verify(reportPortalService, times(1)).startTestItem(context, TestItemType.AFTER_ALL);
        verify(reportPortalService, times(1)).finishTestItem(context, TestItemType.AFTER_ALL, Statuses.PASSED);
        verify(reportPortalService, times(1)).finishSuite(context, Statuses.PASSED);
    }

    // ToDo add cases for failed steps
}
