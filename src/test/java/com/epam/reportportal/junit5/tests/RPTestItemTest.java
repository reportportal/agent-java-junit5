package com.epam.reportportal.junit5.tests;

import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Optional;

import com.epam.reportportal.junit5.RPTestItem;
import com.epam.reportportal.junit5.TestItemType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.engine.descriptor.MethodExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RPTestItemTest {

    @Mock private static MethodExtensionContext context;

    @ParameterizedTest
    @ValueSource(strings = "first")
    void parameterizedTest(String value, TestInfo testInfo) {
        Method thisMethod = testInfo.getTestMethod().get();
        when(context.getRequiredTestMethod()).thenReturn(thisMethod);
        when(context.getTestClass()).thenReturn(Optional.of(RPTestItemTest.class));
        when(context.getElement()).thenReturn(Optional.of(thisMethod));
        when(context.getUniqueId()).thenReturn("[engine:junit-jupiter]/[class:com.epam.reportportal.junit5.tests.RPTestItemTest]/[test-template:parameterizedTest(java.lang.String, org.junit.jupiter.api.TestInfo)]/[test-template-invocation:#1]");
        when(context.getDisplayName()).thenReturn(testInfo.getDisplayName());
        RPTestItem rpTestItem = new RPTestItem(context, TestItemType.TEST);
        Assertions.assertEquals(rpTestItem.type, TestItemType.TEST);
        Assertions.assertEquals(rpTestItem.description, "[1] " + value);
        Assertions.assertEquals(rpTestItem.id, "com.epam.reportportal.junit5.tests.RPTestItemTest.parameterizedTest()[1].STEP");
        Assertions.assertEquals(rpTestItem.name, "parameterizedTest()[1]");
    }

    @ParameterizedTest(name = "Test with value ''{0}''")
    @ValueSource(strings = "first")
    void parameterizedTestWithDescription(String value, TestInfo testInfo) {
        Method thisMethod = testInfo.getTestMethod().get();
        when(context.getRequiredTestMethod()).thenReturn(thisMethod);
        when(context.getTestClass()).thenReturn(Optional.of(RPTestItemTest.class));
        when(context.getElement()).thenReturn(Optional.of(thisMethod));
        when(context.getUniqueId()).thenReturn("[engine:junit-jupiter]/[class:com.epam.reportportal.junit5.tests.RPTestItemTest]/[test-template:parameterizedTest(java.lang.String, org.junit.jupiter.api.TestInfo)]/[test-template-invocation:#1]");
        when(context.getDisplayName()).thenReturn(testInfo.getDisplayName());
        RPTestItem rpTestItem = new RPTestItem(context, TestItemType.TEST);
        Assertions.assertEquals(rpTestItem.type, TestItemType.TEST);
        Assertions.assertEquals(rpTestItem.description, "Test with value '" + value + "'");
        Assertions.assertEquals(rpTestItem.id, "com.epam.reportportal.junit5.tests.RPTestItemTest.parameterizedTestWithDescription()[1].STEP");
        Assertions.assertEquals(rpTestItem.name, "parameterizedTestWithDescription()[1]");
    }

    @RepeatedTest(value = 1)
    void repeatedTest(TestInfo testInfo) {
        Method thisMethod = testInfo.getTestMethod().get();
        when(context.getRequiredTestMethod()).thenReturn(thisMethod);
        when(context.getTestClass()).thenReturn(Optional.of(RPTestItemTest.class));
        when(context.getElement()).thenReturn(Optional.of(thisMethod));
        when(context.getUniqueId()).thenReturn("[engine:junit-jupiter]/[class:com.epam.reportportal.junit5.tests.RPTestItemTest]/[test-template:repeatedTest(org.junit.jupiter.api.TestInfo)]/[test-template-invocation:#1]");
        when(context.getDisplayName()).thenReturn(testInfo.getDisplayName());
        RPTestItem rpTestItem = new RPTestItem(context, TestItemType.TEST);
        Assertions.assertEquals(rpTestItem.type, TestItemType.TEST);
        Assertions.assertEquals(rpTestItem.description, "repetition 1 of 1");
        Assertions.assertEquals(rpTestItem.id, "com.epam.reportportal.junit5.tests.RPTestItemTest.repeatedTest()[1].STEP");
        Assertions.assertEquals(rpTestItem.name, "repeatedTest()[1]");
    }

    @Test
    void baseTest(TestInfo testInfo) {
        Method thisMethod = testInfo.getTestMethod().get();
        when(context.getRequiredTestMethod()).thenReturn(thisMethod);
        when(context.getTestClass()).thenReturn(Optional.of(RPTestItemTest.class));
        when(context.getElement()).thenReturn(Optional.of(thisMethod));
        when(context.getDisplayName()).thenReturn(testInfo.getDisplayName());
        RPTestItem rpTestItem = new RPTestItem(context, TestItemType.TEST);
        Assertions.assertEquals(rpTestItem.type, TestItemType.TEST);
        Assertions.assertEquals(rpTestItem.description, "");
        Assertions.assertEquals(rpTestItem.id, "com.epam.reportportal.junit5.tests.RPTestItemTest.baseTest(TestInfo).STEP");
        Assertions.assertEquals(rpTestItem.name, testInfo.getDisplayName());
    }

    @Test
    void beforeAfterMethodsTestItem(TestInfo testInfo) {
        Method thisMethod = testInfo.getTestMethod().get();
        when(context.getRequiredTestMethod()).thenReturn(thisMethod);
        when(context.getTestClass()).thenReturn(Optional.of(RPTestItemTest.class));
        when(context.getElement()).thenReturn(Optional.of(thisMethod));
        when(context.getDisplayName()).thenReturn(testInfo.getDisplayName());
        RPTestItem rpTestItem = new RPTestItem(context, TestItemType.BEFORE_EACH);
        Assertions.assertEquals(rpTestItem.type, TestItemType.BEFORE_EACH);
        Assertions.assertEquals(rpTestItem.description, "");
        Assertions.assertEquals(rpTestItem.id, "com.epam.reportportal.junit5.tests.RPTestItemTest.beforeAfterMethodsTestItem(TestInfo).BEFORE_METHOD");
        Assertions.assertEquals(rpTestItem.name, testInfo.getDisplayName());
    }

    @Test
    void beforeAfterClassTestItem() {
        when(context.getTestClass()).thenReturn(Optional.of(RPTestItemTest.class));
        when(context.getElement()).thenReturn(Optional.of(RPTestItemTest.class));
        when(context.getDisplayName()).thenReturn(RPTestItemTest.class.getSimpleName());
        RPTestItem rpTestItem = new RPTestItem(context, TestItemType.BEFORE_ALL);
        Assertions.assertEquals(rpTestItem.type, TestItemType.BEFORE_ALL);
        Assertions.assertEquals(rpTestItem.description, "");
        Assertions.assertEquals(rpTestItem.id, "com.epam.reportportal.junit5.tests.RPTestItemTest.BEFORE_CLASS");
        Assertions.assertEquals(rpTestItem.name, RPTestItemTest.class.getSimpleName());
    }
}
