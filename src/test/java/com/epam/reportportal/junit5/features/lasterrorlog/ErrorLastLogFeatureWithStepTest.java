package com.epam.reportportal.junit5.features.lasterrorlog;

import com.epam.reportportal.annotations.Description;
import com.epam.reportportal.annotations.Step;
import com.epam.reportportal.junit5.ErrorLastLogTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.NoSuchElementException;

@ExtendWith(ErrorLastLogTest.ErrorDescriptionTestExtension.class)
public class ErrorLastLogFeatureWithStepTest {

    @Test
    @Description("Login issue")
    public void testWithStepError() {
        enterCredentials();
        System.out.println("Username is not correct");
        loginWithException();
        Assertions.assertTrue(Boolean.TRUE);
    }

    @Step
    @Description("Credentials entered")
    public void enterCredentials() {
        Assertions.assertTrue(Boolean.TRUE);
    }
    @Step
    public void loginWithException() {
        throw new NoSuchElementException("Error message");
    }
}
