package com.epam.reportportal.junit5.features.disabled;

import com.epam.reportportal.junit5.DisabledTestTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DisabledTestTest.DisabledTestExtension.class)
public class OneDisabledTestWithReason {

	public static final String REASON = "My reason to disable test";

	@Disabled(REASON)
	@Test
	void disabledTest() {
		System.out.println("disabled");
	}

}
