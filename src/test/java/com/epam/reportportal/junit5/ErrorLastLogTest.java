package com.epam.reportportal.junit5;

import com.epam.reportportal.junit5.features.lasterrorlog.ErrorLastLogFeatureWithAssertionErrorTest;
import com.epam.reportportal.junit5.features.lasterrorlog.ErrorLastLogFeatureWithAssertionPassedTest;
import com.epam.reportportal.junit5.features.lasterrorlog.ErrorLastLogFeatureWithExceptionTest;
import com.epam.reportportal.junit5.features.lasterrorlog.ErrorLastLogFeatureWithStepTest;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.mockito.ArgumentCaptor;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import static com.epam.reportportal.util.test.CommonUtils.namedId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ErrorLastLogTest {

    private final String testErrorMessagePattern = "%s\nError: \n%s";
    private final String assertErrorMessage = "org.opentest4j.AssertionFailedError: expected: <0> but was: <1>";
    private final String exceptionStepErrorMessage = "java.util.NoSuchElementException: Error message";
    private final String testExceptionMessage = "java.lang.RuntimeException: Critical error";
    private final String assertDescriptionMessage = "0 and 1 is not equal";
    private final String stepDescriptionMessage = "Login issue";
    private final String failedStatus = "FAILED";
    private final String passedStatus = "PASSED";
    static final String testClassUuid = namedId("class");
    static final String testMethodUuid = namedId("test");
    public static class ErrorDescriptionTestExtension extends ReportPortalExtension {

        static final ThreadLocal<ReportPortalClient> client = new ThreadLocal<>();
        static final ThreadLocal<Launch> launch = new ThreadLocal<>();
        public static void init() {
            client.set(mock(ReportPortalClient.class));
            TestUtils.mockLaunch(client.get(), "launchUuid", testClassUuid, testMethodUuid);
            TestUtils.mockLogging(client.get());
            ReportPortal reportPortal = ReportPortal.create(client.get(), TestUtils.standardParameters());
            launch.set(reportPortal.newLaunch(TestUtils.launchRQ(reportPortal.getParameters())));

        }

        @Override
        protected Launch getLaunch(ExtensionContext context) {
            return launch.get();
        }

    }

    public static class Listener implements TestExecutionListener {
        public Deque<TestExecutionResult> results = new ConcurrentLinkedDeque<>();

        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            if (testIdentifier.isTest()) {
                results.add(testExecutionResult);
            }
        }
    }

    @Test
    public void verify_last_error_log_exception() {
        ErrorDescriptionTestExtension.init();
        Listener listener = new Listener();
        TestUtils.runClasses(listener, ErrorLastLogFeatureWithExceptionTest.class);

        ReportPortalClient client = ErrorDescriptionTestExtension.client.get();

        ArgumentCaptor<FinishTestItemRQ> finishTestCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
        verify(client, timeout(1000).atLeastOnce()).finishTestItem(same(testMethodUuid), finishTestCaptor.capture());


        List<FinishTestItemRQ> finishTests = finishTestCaptor.getAllValues();

        FinishTestItemRQ testCaseWithException = finishTests.get(0);

        assertThat(testCaseWithException.getDescription(), startsWith(String.format(testErrorMessagePattern, "", testExceptionMessage)));
        assertThat(testCaseWithException.getStatus(), equalTo(failedStatus));

    }

    @Test
    public void verify_last_error_log_step() {
        ErrorDescriptionTestExtension.init();
        Listener listener = new Listener();
        TestUtils.runClasses(listener, ErrorLastLogFeatureWithStepTest.class);

        ReportPortalClient client = ErrorDescriptionTestExtension.client.get();

        ArgumentCaptor<FinishTestItemRQ> finishTestCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
        verify(client, timeout(1000).atLeastOnce()).finishTestItem(same(testMethodUuid), finishTestCaptor.capture());

        List<FinishTestItemRQ> finishTests = finishTestCaptor.getAllValues();

        FinishTestItemRQ testCaseWithDescriptionAndStepError = finishTests.get(0);

        assertThat(testCaseWithDescriptionAndStepError.getDescription(), startsWith(String.format(testErrorMessagePattern, stepDescriptionMessage, exceptionStepErrorMessage)));
        assertThat(testCaseWithDescriptionAndStepError.getStatus(), equalTo(failedStatus));
    }

    @Test
    public void verify_last_error_log_assertion_error() {
        ErrorDescriptionTestExtension.init();
        Listener listener = new Listener();
        TestUtils.runClasses(listener, ErrorLastLogFeatureWithAssertionErrorTest.class);

        ReportPortalClient client = ErrorDescriptionTestExtension.client.get();

        ArgumentCaptor<FinishTestItemRQ> finishTestCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
        verify(client, timeout(1000).atLeastOnce()).finishTestItem(same(testMethodUuid), finishTestCaptor.capture());

        List<FinishTestItemRQ> finishTests = finishTestCaptor.getAllValues();

        FinishTestItemRQ testCaseAssertException = finishTests.get(0);

        assertThat(testCaseAssertException.getDescription(), startsWith(String.format(testErrorMessagePattern, assertDescriptionMessage, assertErrorMessage)));
        assertThat(testCaseAssertException.getStatus(), equalTo(failedStatus));
    }

    @Test
    public void verify_last_error_log_assertion_passed() {
        ErrorDescriptionTestExtension.init();
        Listener listener = new Listener();
        TestUtils.runClasses(listener, ErrorLastLogFeatureWithAssertionPassedTest.class);

        ReportPortalClient client = ErrorDescriptionTestExtension.client.get();

        ArgumentCaptor<FinishTestItemRQ> finishTestCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
        verify(client, timeout(1000).atLeastOnce()).finishTestItem(same(testMethodUuid), finishTestCaptor.capture());

        List<FinishTestItemRQ> finishTests = finishTestCaptor.getAllValues();

        FinishTestItemRQ testCaseWithDescriptionAndPassed = finishTests.get(0);

        assertThat(testCaseWithDescriptionAndPassed.getStatus(), equalTo(passedStatus));

    }
}
