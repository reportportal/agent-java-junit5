package com.epam.reportportal.junit5;

import com.epam.reportportal.junit5.features.attributes.ClassLevelAttributesTest;
import com.epam.reportportal.junit5.features.attributes.MethodLevelAttributesTest;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import static com.epam.reportportal.junit5.util.TestUtils.extractRequest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AttributesTest {

	public static class AttributesTestExtension extends ReportPortalExtension {
		static Launch LAUNCH;

		@Override
		protected Launch getLaunch(ExtensionContext context) {
			return LAUNCH;
		}
	}

	@BeforeEach
	public void setupMock() {
		AttributesTestExtension.LAUNCH = mock(Launch.class);
		when(AttributesTestExtension.LAUNCH.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> CommonUtils.createMaybeUuid());
		when(AttributesTestExtension.LAUNCH.startTestItem(any(),
				any()
		)).thenAnswer((Answer<Maybe<String>>) invocation -> CommonUtils.createMaybeUuid());
		when(AttributesTestExtension.LAUNCH.finishTestItem(
				any(),
				any()
		)).thenAnswer(invocation -> Maybe.just(new OperationCompletionRS()));
	}

	@Test
	public void verify_class_level_attributes() {
		TestUtils.runClasses(ClassLevelAttributesTest.class);

		Launch launch = AttributesTestExtension.LAUNCH;

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch).startTestItem(captor.capture());  // Start test
		verify(launch).startTestItem(any(), captor.capture()); // Start step

		StartTestItemRQ testRequest = extractRequest(captor, "suite");

		assertThat(testRequest.getAttributes(), hasSize(1));
		ItemAttributesRQ attribute = testRequest.getAttributes().iterator().next();
		assertThat(attribute.getKey(), equalTo(ClassLevelAttributesTest.KEY));
		assertThat(attribute.getValue(), equalTo(ClassLevelAttributesTest.VALUE));

		StartTestItemRQ stepRequest = extractRequest(captor, "step");
		assertThat(stepRequest.getAttributes(), anyOf(nullValue(), emptyIterable()));
	}

	@Test
	public void verify_method_level_attributes() {
		TestUtils.runClasses(MethodLevelAttributesTest.class);

		Launch launch = AttributesTestExtension.LAUNCH;

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch).startTestItem(captor.capture());  // Start test
		verify(launch).startTestItem(any(), captor.capture()); // Start step

		StartTestItemRQ testRequest = extractRequest(captor, "suite");
		assertThat(testRequest.getAttributes(), anyOf(nullValue(), emptyIterable()));

		StartTestItemRQ stepRequest = extractRequest(captor, "step");
		assertThat(stepRequest.getAttributes(), hasSize(1));
		ItemAttributesRQ attribute = stepRequest.getAttributes().iterator().next();
		assertThat(attribute.getKey(), equalTo("myKey"));
		assertThat(attribute.getValue(), equalTo("myValue"));
	}
}
