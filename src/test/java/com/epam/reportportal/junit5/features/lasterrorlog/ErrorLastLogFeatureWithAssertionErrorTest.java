package com.epam.reportportal.junit5.features.lasterrorlog;

import com.epam.reportportal.annotations.Description;
import com.epam.reportportal.junit5.ErrorLastLogTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ErrorLastLogTest.ErrorDescriptionTestExtension.class)
public class ErrorLastLogFeatureWithAssertionErrorTest {

	@Test
	@Description("0 and 1 is not equal")
	public void testWithAssertException() {
		Assertions.assertEquals(0, 1);
	}
}
