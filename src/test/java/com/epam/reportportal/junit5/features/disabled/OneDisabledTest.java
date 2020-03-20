package com.epam.reportportal.junit5.features.disabled;

import com.epam.reportportal.junit5.DisabledTestTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DisabledTestTest.DisabledTestExtension.class)
public class OneDisabledTest {

	@Disabled
	@Test
	void disabledTest() {
		System.out.println("disabled");
	}

}
