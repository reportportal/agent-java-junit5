package com.epam.reportportal.junit5;

import com.epam.reportportal.junit5.features.displayname.*;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:mykola_zakapko@epam.com">Mykola Zakapko</a>
 */
public class DisplayNameTest {
    public static class TestExtension extends ReportPortalExtension {
        static Launch LAUNCH;

        @Override
        protected Launch getLaunch(ExtensionContext context) {
            return LAUNCH;
        }
    }

    @BeforeEach
    public void setupMock() {
        TestExtension.LAUNCH = mock(Launch.class);
        when(TestExtension.LAUNCH.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> CommonUtils.createMaybeUuid());
        when(TestExtension.LAUNCH.startTestItem(any(),
                any()
        )).thenAnswer((Answer<Maybe<String>>) invocation -> CommonUtils.createMaybeUuid());
    }

    @Test
    void displayNameFromMethodAnnotationTest() {
        TestUtils.runClasses(DisplayNameAnnotatedClassTest.class);

        Launch launch = TestExtension.LAUNCH;

        verify(launch, times(1)).startTestItem(any()); // Start parent Suite

        ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
        verify(launch, times(1)).startTestItem(notNull(), captor.capture()); // Start a test

        StartTestItemRQ request = captor.getValue();
        assertThat(request.getName(), equalTo(DisplayNameAnnotatedClassTest.TEST_DISPLAY_NAME_CLASS));
    }

    @Test
    void displayNameFromClassAnnotationTest() {
        TestUtils.runClasses(DisplayNameAnnotatedClassTest.class);

        Launch launch = TestExtension.LAUNCH;

        verify(launch, times(1)).startTestItem(any()); // Start parent Suite

        ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
        verify(launch, times(1)).startTestItem(notNull(), captor.capture()); // Start a test

        StartTestItemRQ request = captor.getValue();
        assertThat(request.getName(), equalTo(DisplayNameAnnotatedClassTest.TEST_DISPLAY_NAME_CLASS));
    }

    @Test
    void displayNameFromBothMethodAndClassAnnotationTest() {
        TestUtils.runClasses(DisplayNameAnnotatedMethodAndClassTest.class);

        Launch launch = TestExtension.LAUNCH;

        verify(launch, times(1)).startTestItem(any()); // Start parent Suite

        ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
        verify(launch, times(1)).startTestItem(notNull(), captor.capture()); // Start a test

        StartTestItemRQ request = captor.getValue();
        //from both annotations the expected one should be taken from the method
        assertThat(request.getName(), equalTo(DisplayNameAnnotatedMethodTest.TEST_DISPLAY_NAME_METHOD));
    }

    @Test
    void displayNameFromMethodAnnotationDynamicTest() {
        TestUtils.runClasses(DisplayNameAnnotatedMethodDynamicTest.class);

        Launch launch = TestExtension.LAUNCH;
        verify(launch, times(1)).startTestItem(any()); // Start parent Suite

        ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
        verify(launch, times(2)).startTestItem(notNull(), captor.capture()); // Start a test

        List<String> testStepsDisplayName = captor.getAllValues()
                .stream()
                .filter(e -> e.getType().equals(ItemType.STEP.name()))
                .map(StartTestItemRQ::getName)
                .collect(Collectors.toList());

        assertThat(testStepsDisplayName, hasItem(DisplayNameAnnotatedMethodDynamicTest.TEST_DISPLAY_NAME_DYNAMIC_METHOD));
    }

    @Test
    void displayNameFromClassAnnotationDynamicTest() {
        TestUtils.runClasses(DisplayNameAnnotatedClassDynamicTest.class);

        Launch launch = TestExtension.LAUNCH;
        verify(launch, times(1)).startTestItem(any()); // Start parent Suite

        ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
        verify(launch, times(2)).startTestItem(notNull(), captor.capture()); // Start a test

        List<String> testStepsDisplayName = captor.getAllValues()
                .stream()
                .filter(e -> e.getType().equals(ItemType.STEP.name()))
                .map(StartTestItemRQ::getName)
                .collect(Collectors.toList());

        assertThat(testStepsDisplayName, hasItem(DisplayNameAnnotatedClassDynamicTest.TEST_DISPLAY_NAME_DYNAMIC_CLASS));
    }

    @Test
    void displayNameFromBothMethodAndClassAnnotationDynamicTest() {
        TestUtils.runClasses(DisplayNameAnnotatedMethodAndClassDynamicTest.class);

        Launch launch = TestExtension.LAUNCH;
        verify(launch, times(1)).startTestItem(any()); // Start parent Suite

        ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
        verify(launch, times(2)).startTestItem(notNull(), captor.capture()); // Start a test

        List<String> testStepsDisplayName = captor.getAllValues()
                .stream()
                .filter(e -> e.getType().equals(ItemType.STEP.name()))
                .map(StartTestItemRQ::getName)
                .collect(Collectors.toList());
        //from both annotations the expected one should be taken from the method
        assertThat(testStepsDisplayName, hasItem(DisplayNameAnnotatedMethodDynamicTest.TEST_DISPLAY_NAME_DYNAMIC_METHOD));
    }

    @Test
    void displayNameJunitAndRPFromMethodAnnotationTest() {
        TestUtils.runClasses(DisplayNameBothJunitAndRPAnnotatedMethodTest.class);

        Launch launch = TestExtension.LAUNCH;

        verify(launch, times(1)).startTestItem(any()); // Start parent Suite

        ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
        verify(launch, times(1)).startTestItem(notNull(), captor.capture()); // Start a test

        StartTestItemRQ request = captor.getValue();
        assertThat(request.getName(), equalTo(DisplayNameBothJunitAndRPAnnotatedMethodTest.TEST_DISPLAY_NAME_METHOD));
    }

    @Test
    void displayNameJunitAndRPFromClassAnnotationAnnotationTest() {
        TestUtils.runClasses(DisplayNameBothJunitAndRPAnnotatedClassTest.class);

        Launch launch = TestExtension.LAUNCH;

        verify(launch, times(1)).startTestItem(any()); // Start parent Suite

        ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
        verify(launch, times(1)).startTestItem(notNull(), captor.capture()); // Start a test

        StartTestItemRQ request = captor.getValue();
        assertThat(request.getName(), equalTo(DisplayNameBothJunitAndRPAnnotatedClassTest.TEST_DISPLAY_NAME_CLASS));
    }

    @Test
    void displayNameJunitAndRPFromMethodAnnotationDynamicTest() {
        TestUtils.runClasses(DisplayNameBothJunitAndRPAnnotatedMethodDynamicTest.class);

        Launch launch = TestExtension.LAUNCH;
        verify(launch, times(1)).startTestItem(any()); // Start parent Suite

        ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
        verify(launch, times(2)).startTestItem(notNull(), captor.capture()); // Start a test

        List<String> testStepsDisplayName = captor.getAllValues()
                .stream()
                .filter(e -> e.getType().equals(ItemType.STEP.name()))
                .map(StartTestItemRQ::getName)
                .collect(Collectors.toList());

        assertThat(testStepsDisplayName, hasItem(DisplayNameBothJunitAndRPAnnotatedMethodDynamicTest.TEST_DISPLAY_NAME_DYNAMIC_METHOD));
    }

    @Test
    void displayNameJunitAndRPFromClassAnnotationDynamicTest() {
        TestUtils.runClasses(DisplayNameBothJunitAndRPAnnotatedClassDynamicTest.class);

        Launch launch = TestExtension.LAUNCH;
        verify(launch, times(1)).startTestItem(any()); // Start parent Suite

        ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
        verify(launch, times(2)).startTestItem(notNull(), captor.capture()); // Start a test

        List<String> testStepsDisplayName = captor.getAllValues()
                .stream()
                .filter(e -> e.getType().equals(ItemType.STEP.name()))
                .map(StartTestItemRQ::getName)
                .collect(Collectors.toList());

        assertThat(testStepsDisplayName, hasItem(DisplayNameBothJunitAndRPAnnotatedClassDynamicTest.TEST_DISPLAY_NAME_DYNAMIC_CLASS));
    }

    @Test
    void displayNameJunitAndRPFromBothMethodAndClassAnnotationTest() {
        TestUtils.runClasses(DisplayNameBothJunitAndRPAnnotatedMethodAndClassTest.class);

        Launch launch = TestExtension.LAUNCH;

        verify(launch, times(1)).startTestItem(any()); // Start parent Suite

        ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
        verify(launch, times(1)).startTestItem(notNull(), captor.capture()); // Start a test

        StartTestItemRQ request = captor.getValue();
        //from both annotations the expected one should be taken from the method
        assertThat(request.getName(), equalTo(DisplayNameAnnotatedMethodTest.TEST_DISPLAY_NAME_METHOD));
    }

    @Test
    void displayNameJunitAndRPFromBothMethodAndClassAnnotationDynamicTest() {
        TestUtils.runClasses(DisplayNameBothJunitAndRPAnnotatedMethodAndClassDynamicTest.class);

        Launch launch = TestExtension.LAUNCH;
        verify(launch, times(1)).startTestItem(any()); // Start parent Suite

        ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
        verify(launch, times(2)).startTestItem(notNull(), captor.capture()); // Start a test

        List<String> testStepsDisplayName = captor.getAllValues()
                .stream()
                .filter(e -> e.getType().equals(ItemType.STEP.name()))
                .map(StartTestItemRQ::getName)
                .collect(Collectors.toList());
        //from both annotations the expected one should be taken from the method
        assertThat(testStepsDisplayName, hasItem(DisplayNameAnnotatedMethodDynamicTest.TEST_DISPLAY_NAME_DYNAMIC_METHOD));
    }

    @Test
    void displayNameFromClassAnnotationWithEmptyValueTest() {
        TestUtils.runClasses(DisplayNameAnnotatedClassWithEmptyValueTest.class);

        Launch launch = TestExtension.LAUNCH;

        verify(launch, times(1)).startTestItem(any()); // Start parent Suite

        ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
        verify(launch, times(1)).startTestItem(notNull(), captor.capture()); // Start a test

        StartTestItemRQ request = captor.getValue();
        assertThat(request.getName(), equalTo(DisplayNameAnnotatedClassWithEmptyValueTest.TEST_DISPLAY_NAME_CLASS));
    }

    @Test
    void displayNameFromMethodAnnotationWithEmptyValueTest() {
        TestUtils.runClasses(DisplayNameAnnotatedMethodWithEmptyValueTest.class);

        Launch launch = TestExtension.LAUNCH;

        verify(launch, times(1)).startTestItem(any()); // Start parent Suite

        ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
        verify(launch, times(1)).startTestItem(notNull(), captor.capture()); // Start a test

        StartTestItemRQ request = captor.getValue();
        assertThat(request.getName(), equalTo(DisplayNameAnnotatedMethodWithEmptyValueTest.TEST_DISPLAY_NAME_METHOD));
    }
}
