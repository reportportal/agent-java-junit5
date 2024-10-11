package com.epam.reportportal.junit5.features.lasterrorlog;

import com.epam.reportportal.annotations.Description;
import com.epam.reportportal.annotations.Step;
import com.epam.reportportal.junit5.ErrorLastLogTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ErrorLastLogTest.ErrorDescriptionTestExtension.class)
public class ErrorLastLogFeatureWithAssertionPassedTest {

    @Test
    @Description("successful test")
    public void testWithDescriptionAndPassed() {
        login();
        Assertions.assertTrue(true);
    }

    @Step
    public void login() {
        System.out.println("Login successful");
    }
}
