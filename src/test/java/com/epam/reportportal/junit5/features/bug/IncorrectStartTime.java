package com.epam.reportportal.junit5.features.bug;

import com.epam.reportportal.junit5.bugs.LaunchStartTimeTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(LaunchStartTimeTest.TestExtension.class)
public class IncorrectStartTime {

	@Test
	public void test_start_time() {
		System.out.println("Test");
	}
}
