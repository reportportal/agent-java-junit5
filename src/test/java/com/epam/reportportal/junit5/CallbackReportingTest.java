/*
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.junit5;

import com.epam.reportportal.junit5.features.callback.CallbackFeatureTest;
import com.epam.reportportal.junit5.features.callback.CallbackLogFeatureTest;
import com.epam.reportportal.junit5.util.TestUtils;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.service.step.StepReporter;
import com.epam.ta.reportportal.ws.model.EntryCreatedAsyncRS;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.epam.reportportal.junit5.CallbackReportingTest.CallbackReportingExtension.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class CallbackReportingTest {

	public static final String ITEM_CALLBACK_FINISH_STATUS = "FAILED";

	public static final String ERROR_LOG_LEVEL = "Error";
	public static final String LOG_MESSAGE = "Error log message";
	public static final Date LOG_TIME = Calendar.getInstance().getTime();

	public static class CallbackReportingExtension extends ReportPortalExtension {

		static final ThreadLocal<Maybe<String>> ROOT_ITEM_ID = new ThreadLocal<>();
		static final ThreadLocal<Maybe<String>> TEST_METHOD_ID = new ThreadLocal<>();

		static final ThreadLocal<Launch> LAUNCH = new ThreadLocal<>();
		final static ThreadLocal<Maybe<String>> LAUNCH_MAYBE_ID = new ThreadLocal<>();
		final static ThreadLocal<String> LAUNCH_ID = new ThreadLocal<>();
		static final ThreadLocal<ListenerParameters> LISTENER_PARAMETERS = new ThreadLocal<>();
		static final ThreadLocal<ReportPortalClient> REPORT_PORTAL_CLIENT = new ThreadLocal<>();
		static final ThreadLocal<ReportPortal> REPORT_PORTAL = new ThreadLocal<>();

		public CallbackReportingExtension() {
			LAUNCH.set(mock(Launch.class));
			when(LAUNCH.get().getStepReporter()).thenReturn(StepReporter.NOOP_STEP_REPORTER);
			ROOT_ITEM_ID.set(Maybe.just("Root item id"));
			when(LAUNCH.get().startTestItem(any())).thenReturn(ROOT_ITEM_ID.get());

			Maybe<String> launchId = Maybe.just("Launch " + UUID.randomUUID());
			LAUNCH_MAYBE_ID.set(launchId);
			LAUNCH_ID.set(launchId.blockingGet());
			when(LAUNCH.get().start()).thenReturn(LAUNCH_MAYBE_ID.get());

			TEST_METHOD_ID.set(Maybe.just("Test method id"));
			when(LAUNCH.get().startTestItem(eq(ROOT_ITEM_ID.get()), any())).thenReturn(TEST_METHOD_ID.get());

			REPORT_PORTAL.set(mock(ReportPortal.class));
			when(REPORT_PORTAL.get().newLaunch(any())).thenReturn(LAUNCH.get());

			LISTENER_PARAMETERS.set(mock(ListenerParameters.class));
			when(LISTENER_PARAMETERS.get().isCallbackReportingEnabled()).thenReturn(true);
			when(REPORT_PORTAL.get().getParameters()).thenReturn(LISTENER_PARAMETERS.get());

			REPORT_PORTAL_CLIENT.set(mock(ReportPortalClient.class));
			when(REPORT_PORTAL_CLIENT.get()
					.finishTestItem(eq(TEST_METHOD_ID.get().blockingGet()), any())).thenReturn(Maybe.just(new OperationCompletionRS(
					"Success")));

			when(REPORT_PORTAL_CLIENT.get().log(any(SaveLogRQ.class))).thenReturn(Maybe.just(new EntryCreatedAsyncRS(UUID.randomUUID()
					.toString())));

			when(REPORT_PORTAL.get().getClient()).thenReturn(REPORT_PORTAL_CLIENT.get());

			try {
				Field rpField = ReportPortalExtension.class.getDeclaredField("REPORT_PORTAL");
				rpField.setAccessible(true);
				Field modifiersField = Field.class.getDeclaredField("modifiers");
				modifiersField.setAccessible(true);
				modifiersField.setInt(rpField, rpField.getModifiers() & ~Modifier.FINAL);
				rpField.set(null, REPORT_PORTAL.get());
			} catch (NoSuchFieldException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		@Override
		protected ReportPortal getReporter() {
			return REPORT_PORTAL.get();
		}

		@Override
		protected String getLaunchId(ExtensionContext context) {
			return LAUNCH_ID.get();
		}
	}

	@Test
	void changeStatusToFailedUsingCallbackTest() {

		TestUtils.runClasses(CallbackFeatureTest.class);

		ArgumentCaptor<FinishTestItemRQ> finishItemCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(LAUNCH.get(), times(2)).startTestItem(eq(ROOT_ITEM_ID.get()), any());
		verify(LAUNCH.get(), times(2)).finishTestItem(eq(TEST_METHOD_ID.get()), finishItemCaptor.capture());

		verify(REPORT_PORTAL_CLIENT.get(), times(1)).finishTestItem(eq(TEST_METHOD_ID.get().blockingGet()), finishItemCaptor.capture());
		List<FinishTestItemRQ> finishedItems = finishItemCaptor.getAllValues();

		FinishTestItemRQ finishMethod = finishedItems.get(0);
		FinishTestItemRQ afterMethod = finishedItems.get(1);
		FinishTestItemRQ finishMethodCallback = finishedItems.get(2);

		assertThat(finishMethod.getStatus(), equalTo("PASSED"));
		assertThat(afterMethod.getStatus(), equalTo("PASSED"));
		assertThat(finishMethodCallback.getStatus(), equalTo(ITEM_CALLBACK_FINISH_STATUS));
	}

	@Test
	void attachLogUsingCallbackTest() {

		TestUtils.runClasses(CallbackLogFeatureTest.class);

		verify(LAUNCH.get(), times(2)).startTestItem(eq(ROOT_ITEM_ID.get()), any());
		verify(LAUNCH.get(), times(2)).finishTestItem(eq(TEST_METHOD_ID.get()), any());

		ArgumentCaptor<SaveLogRQ> logCaptor = ArgumentCaptor.forClass(SaveLogRQ.class);
		verify(REPORT_PORTAL_CLIENT.get(), times(1)).log(logCaptor.capture());

		SaveLogRQ log = logCaptor.getValue();

		assertThat(log.getMessage(), equalTo(LOG_MESSAGE));
		assertThat(log.getLevel(), equalTo(ERROR_LOG_LEVEL));
		assertThat(log.getLogTime(), equalTo(LOG_TIME));
	}

}
