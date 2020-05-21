package com.epam.reportportal.junit5.features.bug;

import com.epam.reportportal.junit5.bugs.BeforeEachFailedDuplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@ExtendWith(BeforeEachFailedDuplication.BeforeEachFailedDuplicationExtension.class)
public class BeforeEachFailedDuplicate {

	public enum TestParams {
		ONE,
		TWO
	}

	@BeforeAll
	public static void beforeAll() {
		System.out.println("Before all");
	}

	@BeforeEach
	public void beforeEach() throws InterruptedException {
		System.out.println("Before each");
		Thread.sleep(100);
	}

	@ParameterizedTest
	@EnumSource(TestParams.class)
	public void testFirstBeforeAfterAllBeforeAfterEach(TestParams param) throws InterruptedException {
		System.out.println("First Test: " + param.name());
		Thread.sleep(100);
	}

	@ParameterizedTest
	@EnumSource(TestParams.class)
	public void testSecondBeforeAfterAllBeforeAfterEach(TestParams param) throws InterruptedException {
		System.out.println("Second Test: " + param.name());
		Thread.sleep(100);
	}

	@AfterEach
	public void afterEach() throws InterruptedException {
		System.out.println("After each");
		Thread.sleep(100);
	}

	@AfterAll
	public static void afterAll() {
		System.out.println("After all");
	}
}
