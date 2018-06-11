/*
 * Copyright 2018 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/commons-model
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.junit5;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import io.reactivex.Maybe;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.opentest4j.TestAbortedException;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static java.util.Optional.ofNullable;

/**
 * ReportPortal Listener sends the results of test execution to ReportPortal in RealTime
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
public class ReportPortalExtension
		implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

	private static final Map<String, Launch> launchMap = new HashMap<>();
	private final ConcurrentMap<String, Maybe<String>> idMapping = new ConcurrentHashMap<>();
	
    /**
     * Create a {@link Thread} object that encapsulated the specified shutdown listener.
     * 
     * @param listener shutdown listener object
     * @return shutdown listener thread object
     */
    private static Thread getShutdownHook(final Launch launch) {
        return new Thread() {
            @Override
            public void run() {
        		FinishExecutionRQ rq = new FinishExecutionRQ();
        		rq.setEndTime(Calendar.getInstance().getTime());
        		launch.finish(rq);
            }
        };
    }
    
	private static synchronized Launch startLaunchIfRequiredFor(ExtensionContext context) {
		String launchId = context.getRoot().getUniqueId();
		if ( ! launchMap.containsKey(launchId)) {
			ReportPortal rp = ReportPortal.builder().build();
			ListenerParameters params = rp.getParameters();
			StartLaunchRQ rq = new StartLaunchRQ();
			rq.setMode(params.getLaunchRunningMode());
			rq.setDescription(params.getDescription());
			rq.setName(params.getLaunchName());
			rq.setTags(params.getTags());
			rq.setStartTime(Calendar.getInstance().getTime());
			Launch launch = rp.newLaunch(rq);
			launchMap.put(launchId, launch);
			Runtime.getRuntime().addShutdownHook(getShutdownHook(launch));		
			launch.start();
		}
		return launchMap.get(launchId);
	}
	
	private static synchronized Launch getLaunchFor(ExtensionContext context) {
		String launchId = context.getRoot().getUniqueId();
		return launchMap.get(launchId);
	}

	@Override
	public void beforeTestExecution(ExtensionContext context) throws Exception {
		Launch launch = startLaunchIfRequiredFor(context);

		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setName(context.getDisplayName());
		rq.setDescription(context.getDisplayName());
		rq.setUniqueId(context.getUniqueId());
		rq.setType("TEST");

		ofNullable(context.getTags()).ifPresent(rq::setTags);

		rq.setRetry(false);
		
		Maybe<String> itemId = context.getParent()
				.map(ExtensionContext::getUniqueId)
				.map(parentId -> ofNullable(idMapping.get(parentId)))
				.map(parentTest -> launch.startTestItem(parentTest.orElse(null), rq))
				.orElseGet(() -> launch.startTestItem(rq));

		this.idMapping.put(context.getUniqueId(), itemId);
	}

	@Override
	public void afterTestExecution(ExtensionContext context) throws Exception {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setStatus(getExecutionStatus(context));
		rq.setEndTime(Calendar.getInstance().getTime());
		getLaunchFor(context).finishTestItem(this.idMapping.get(context.getUniqueId()), rq);
	}

	private static String getExecutionStatus(ExtensionContext context) {
		Optional<Throwable> exception = context.getExecutionException();
		if (!exception.isPresent()) {
			return Statuses.PASSED;
		} else if (exception.get() instanceof TestAbortedException) {
			return Statuses.SKIPPED;
		} else {
			return Statuses.FAILED;
		}
	}

	//	testIdentifier.getSource().ifPresent(source -> {
	//		if (source instanceof MethodSource) {
	//			MethodSource methodSource = (MethodSource) source;
	//			Class<?> testClass = Class.forName(methodSource.getClassName());
	//			Method testMethod = testClass.getDeclaredMethod(methodSource.getMethodName());
	//			Optional<RepeatedTest> annotation = AnnotationUtils.findAnnotation(testMethod, RepeatedTest.class);
	//
	//		}
	//	});
}
