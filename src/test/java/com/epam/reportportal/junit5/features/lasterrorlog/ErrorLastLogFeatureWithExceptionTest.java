package com.epam.reportportal.junit5.features.lasterrorlog;

import com.epam.reportportal.junit5.ErrorLastLogTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ErrorLastLogTest.ErrorDescriptionTestExtension.class)
public class ErrorLastLogFeatureWithExceptionTest {

	@Test
	public void testWithException() {
		throw new RuntimeException("Critical error");
	}

}
