package com.epam.reportportal.junit5.features.coderef;

import com.epam.reportportal.junit5.CodeReferenceTests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(CodeReferenceTests.CodeReferenceTestExtension.class)
public class SingleTest {

	public static final String TEST_CASE_DISPLAY_NAME = "0c6b91e-623d-498d-a56d-fae1f2bbcbe4";

	@Test()
	@DisplayName("0c6b91e-623d-498d-a56d-fae1f2bbcbe4")
	public void singleTest() {
		System.out.println("Test factory test: " + TEST_CASE_DISPLAY_NAME);
	}
}
