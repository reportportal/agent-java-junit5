package com.epam.reportportal.junit5;

import com.epam.reportportal.junit5.features.coderef.SingleDynamicTest;
import com.epam.reportportal.junit5.features.coderef.SingleTest;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CodeReferenceTest {

	public static class CodeReferenceTestExtension extends ReportPortalExtension {
		static Launch LAUNCH;

		@Override
		protected Launch getLaunch(ExtensionContext context) {
			return LAUNCH;
		}
	}

	@BeforeEach
	public void setupMock() {
		CodeReferenceTestExtension.LAUNCH = mock(Launch.class);
		when(CodeReferenceTestExtension.LAUNCH.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> CommonUtils.createMaybeUuid());
		when(CodeReferenceTestExtension.LAUNCH.startTestItem(any(),
				any()
		)).thenAnswer((Answer<Maybe<String>>) invocation -> CommonUtils.createMaybeUuid());
	}

	@Test
	public void verify_static_test_code_reference_generation() {
		TestUtils.runClasses(SingleTest.class);

		Launch launch = CodeReferenceTestExtension.LAUNCH;

		ArgumentCaptor<StartTestItemRQ> suiteCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(1)).startTestItem(suiteCaptor.capture()); // Start parent Suite

		ArgumentCaptor<StartTestItemRQ> testCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(1)).startTestItem(notNull(), testCaptor.capture()); // Start a test

		String className = SingleTest.class.getCanonicalName();
		StartTestItemRQ suiteRq = suiteCaptor.getValue();
		assertThat(suiteRq.getCodeRef(), equalTo(className));

		StartTestItemRQ testRq = testCaptor.getValue();
		assertThat(testRq.getCodeRef(), equalTo(className + ".singleTest"));
	}

	@Test
	public void verify_dynamic_test_code_reference_generation() {
		TestUtils.runClasses(SingleDynamicTest.class);

		Launch launch = CodeReferenceTestExtension.LAUNCH;

		ArgumentCaptor<StartTestItemRQ> suiteCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(1)).startTestItem(suiteCaptor.capture()); // Start parent Suite

		ArgumentCaptor<StartTestItemRQ> testCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(2)).startTestItem(notNull(), testCaptor.capture()); // Start a test class and a test

		String className = SingleDynamicTest.class.getCanonicalName();
		StartTestItemRQ suiteRq = suiteCaptor.getValue();
		assertThat(suiteRq.getCodeRef(), equalTo(className));

		List<StartTestItemRQ> rqValues = testCaptor.getAllValues();
		String testName = className + ".testForTestFactory";
		assertThat(rqValues.get(0).getCodeRef(), equalTo(testName));
		assertThat(rqValues.get(1).getCodeRef(), equalTo(testName + "$" + SingleDynamicTest.TEST_CASE_DISPLAY_NAME));
	}

}
