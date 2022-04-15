package com.epam.reportportal.junit5.features.bug;

import com.epam.reportportal.junit5.bugs.LaunchStartTimeTest;
import com.epam.reportportal.util.test.CommonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(LaunchStartTimeTest.TestExtension.class)
public class TestIncorrectStartTime {

	@Test
	public void test_start_time() throws InterruptedException {
		Thread.sleep(CommonUtils.MINIMAL_TEST_PAUSE);
	}
}
