package com.epam.reportportal.junit5.features.displayname;

import com.epam.reportportal.junit5.DisplayNameTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@com.epam.reportportal.annotations.DisplayName(DisplayNameAnnotatedClassTest.TEST_DISPLAY_NAME_CLASS)
@ExtendWith(DisplayNameTest.TestExtension.class)
public class DisplayNameAnnotatedClassTest {
    public static final String TEST_DISPLAY_NAME_CLASS = "My display name";
    @Test
    public void testDisplayNameTest() {
    }
}
