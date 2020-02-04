package com.epam.reportportal.junit5;

import com.epam.reportportal.junit5.features.coderef.SingleDynamicTest;
import com.epam.reportportal.junit5.features.coderef.SingleTest;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CodeReferenceTests {

	public static class CodeReferenceTestExtension extends ReportPortalExtension {
		static final Launch LAUNCH;

		static {
			LAUNCH = mock(Launch.class);
			when(LAUNCH.startTestItem(any(), any())).thenAnswer((Answer<Maybe<String>>) invocation -> TestUtils.createItemUuidMaybe());
		}

		@Override
		Launch getLaunch(ExtensionContext context) {
			return LAUNCH;
		}
	}

	@Test
	public void verify_static_test_code_reference_generation() {
		TestUtils.runClasses(SingleTest.class);

		Launch launch = CodeReferenceTestExtension.LAUNCH;

		verify(launch, times(1)).startTestItem(isNull(), any()); // Start parent Suite

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(1)).startTestItem(notNull(), captor.capture()); // Start a test

		List<StartTestItemRQ> rqValues = captor.getAllValues();
		String className = SingleTest.class.getCanonicalName();
		assertThat(rqValues.get(0).getCodeRef(), equalTo(className + ".singleTest"));
	}

	public static class DynamicCodeReferenceTestExtension extends ReportPortalExtension {
		static final Launch LAUNCH;

		static {
			LAUNCH = mock(Launch.class);
			when(LAUNCH.startTestItem(any(), any())).thenAnswer((Answer<Maybe<String>>) invocation -> TestUtils.createItemUuidMaybe());
		}

		@Override
		Launch getLaunch(ExtensionContext context) {
			return LAUNCH;
		}
	}

	@Test
	public void verify_dynamic_test_code_reference_generation() {
		TestUtils.runClasses(SingleDynamicTest.class);

		Launch launch = DynamicCodeReferenceTestExtension.LAUNCH;

		verify(launch, times(1)).startTestItem(isNull(), any()); // Start parent Suite

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(2)).startTestItem(notNull(), captor.capture()); // Start a test class and a test

		List<StartTestItemRQ> rqValues = captor.getAllValues();
		String testName = SingleDynamicTest.class.getCanonicalName() + ".testForTestFactory";
		assertThat(rqValues.get(0).getCodeRef(), equalTo(testName));
		assertThat(rqValues.get(1).getCodeRef(), equalTo(testName + "$" + SingleDynamicTest.TEST_CASE_DISPLAY_NAME));
	}

}
