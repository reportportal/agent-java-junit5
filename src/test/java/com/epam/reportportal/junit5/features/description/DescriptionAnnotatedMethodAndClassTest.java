package com.epam.reportportal.junit5.features.description;

import com.epam.reportportal.annotations.Description;
import com.epam.reportportal.junit5.DescriptionTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DescriptionTest.TestExtension.class)
@Description(DescriptionAnnotatedClassTest.TEST_DESCRIPTION_CLASS)
public class DescriptionAnnotatedMethodAndClassTest {
    @Test
    @Description(DescriptionAnnotatedMethodTest.TEST_DESCRIPTION_METHOD)
    public void testDescriptionTest() {
    }
}
