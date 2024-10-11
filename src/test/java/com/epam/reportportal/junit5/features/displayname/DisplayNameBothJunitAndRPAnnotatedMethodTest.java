package com.epam.reportportal.junit5.features.displayname;

import com.epam.reportportal.annotations.DisplayName;
import com.epam.reportportal.junit5.DisplayNameTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DisplayNameTest.TestExtension.class)
public class DisplayNameBothJunitAndRPAnnotatedMethodTest {
    public static final String TEST_DISPLAY_NAME_METHOD = "My test displayName on the method";
    @Test
    @DisplayName(TEST_DISPLAY_NAME_METHOD)
    @org.junit.jupiter.api.DisplayName("Junit")
    public void testDisplayNameTest() {
    }
}
