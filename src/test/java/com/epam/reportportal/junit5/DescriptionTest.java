package com.epam.reportportal.junit5;

import com.epam.reportportal.junit5.features.description.*;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:oleksandr_fomenko@epam.com">Oleksandr Fomenko</a>
 */
public class DescriptionTest {
	public static class TestExtension extends ReportPortalExtension {
		static Launch LAUNCH;

		@Override
		protected Launch getLaunch(ExtensionContext context) {
			return LAUNCH;
		}
	}

	@BeforeEach
	public void setupMock() {
		TestExtension.LAUNCH = mock(Launch.class);
		when(TestExtension.LAUNCH.startTestItem(any())).thenAnswer((Answer<Maybe<String>>) invocation -> CommonUtils.createMaybeUuid());
		when(TestExtension.LAUNCH.startTestItem(
				any(),
				any()
		)).thenAnswer((Answer<Maybe<String>>) invocation -> CommonUtils.createMaybeUuid());
	}

	@Test
	void descriptionFromMethodAnnotationTest() {
		TestUtils.runClasses(DescriptionAnnotatedMethodTest.class);

		Launch launch = TestExtension.LAUNCH;

		verify(launch, times(1)).startTestItem(any()); // Start parent Suite

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(1)).startTestItem(notNull(), captor.capture()); // Start a test

		StartTestItemRQ request = captor.getValue();
		assertThat(request.getDescription(), equalTo(DescriptionAnnotatedMethodTest.TEST_DESCRIPTION_METHOD));
	}

	@Test
	void descriptionFromClassAnnotationTest() {
		TestUtils.runClasses(DescriptionAnnotatedClassTest.class);

		Launch launch = TestExtension.LAUNCH;

		verify(launch, times(1)).startTestItem(any()); // Start parent Suite

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(1)).startTestItem(notNull(), captor.capture()); // Start a test

		StartTestItemRQ request = captor.getValue();
		assertThat(request.getDescription(), equalTo(DescriptionAnnotatedClassTest.TEST_DESCRIPTION_CLASS));
	}

	@Test
	void descriptionFromBothMethodAndClassAnnotationTest() {
		TestUtils.runClasses(DescriptionAnnotatedMethodAndClassTest.class);

		Launch launch = TestExtension.LAUNCH;

		verify(launch, times(1)).startTestItem(any()); // Start parent Suite

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(1)).startTestItem(notNull(), captor.capture()); // Start a test

		StartTestItemRQ request = captor.getValue();
		//from both annotations the expected one should be taken from the method
		assertThat(request.getDescription(), equalTo(DescriptionAnnotatedMethodTest.TEST_DESCRIPTION_METHOD));
	}

	@Test
	void descriptionFromMethodAnnotationDynamicTest() {
		TestUtils.runClasses(DescriptionAnnotatedMethodDynamicTest.class);

		Launch launch = TestExtension.LAUNCH;
		verify(launch, times(1)).startTestItem(any()); // Start parent Suite

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(2)).startTestItem(notNull(), captor.capture()); // Start a test

		List<String> testStepsDescription = captor.getAllValues()
				.stream()
				.filter(e -> e.getType().equals(ItemType.STEP.name()))
				.map(StartTestItemRQ::getDescription)
				.collect(Collectors.toList());

		assertThat(testStepsDescription, hasItem(DescriptionAnnotatedMethodDynamicTest.TEST_DESCRIPTION_DYNAMIC_METHOD));
	}

	@Test
	void descriptionFromClassAnnotationDynamicTest() {
		TestUtils.runClasses(DescriptionAnnotatedClassDynamicTest.class);

		Launch launch = TestExtension.LAUNCH;
		verify(launch, times(1)).startTestItem(any()); // Start parent Suite

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(2)).startTestItem(notNull(), captor.capture()); // Start a test

		List<String> testStepsDescription = captor.getAllValues()
				.stream()
				.filter(e -> e.getType().equals(ItemType.STEP.name()))
				.map(StartTestItemRQ::getDescription)
				.collect(Collectors.toList());

		assertThat(testStepsDescription, hasItem(DescriptionAnnotatedClassDynamicTest.TEST_DESCRIPTION_DYNAMIC_CLASS));
	}

	@Test
	void descriptionFromBothMethodAndClassAnnotationDynamicTest() {
		TestUtils.runClasses(DescriptionAnnotatedMethodAndClassDynamicTest.class);

		Launch launch = TestExtension.LAUNCH;
		verify(launch, times(1)).startTestItem(any()); // Start parent Suite

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(launch, times(2)).startTestItem(notNull(), captor.capture()); // Start a test

		List<String> testStepsDescription = captor.getAllValues()
				.stream()
				.filter(e -> e.getType().equals(ItemType.STEP.name()))
				.map(StartTestItemRQ::getDescription)
				.collect(Collectors.toList());
		//from both annotations the expected one should be taken from the method
		assertThat(testStepsDescription, hasItem(DescriptionAnnotatedMethodDynamicTest.TEST_DESCRIPTION_DYNAMIC_METHOD));
	}
}
