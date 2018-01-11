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
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import rp.com.google.common.collect.ImmutableMap;

import java.util.Calendar;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ReportPortal Listener sends the results of test execution to ReportPortal in RealTime
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
public class ReportPortalExtension implements TestExecutionListener {

	private final ConcurrentMap<String, Maybe<String>> idMapping;
	private final Launch launch;

	public ReportPortalExtension() {
		this.idMapping = new ConcurrentHashMap<>();
		ReportPortal rp = ReportPortal.builder().build();
		ListenerParameters params = rp.getParameters();
		StartLaunchRQ rq = new StartLaunchRQ();
		rq.setMode(params.getLaunchRunningMode());
		rq.setDescription(params.getDescription());
		rq.setName(params.getLaunchName());
		rq.setTags(params.getTags());
		rq.setStartTime(Calendar.getInstance().getTime());
		this.launch = rp.newLaunch(rq);
	}

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		this.launch.start();
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {

		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setName(testIdentifier.getDisplayName());
		rq.setDescription(testIdentifier.getLegacyReportingName());
		rq.setUniqueId(testIdentifier.getUniqueId());
		rq.setType(testIdentifier.isContainer() ? "SUITE" : "TEST");

		rq.setRetry(false);

		Maybe<String> itemId = testIdentifier.getParentId()
				.map(parent -> Optional.ofNullable(idMapping.get(parent)))
				.map(parentId -> this.launch.startTestItem(parentId.orElse(null), rq))
				.orElseGet(() -> this.launch.startTestItem(rq));

		this.idMapping.put(testIdentifier.getUniqueId(), itemId);

	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setStatus(STATUS_MAPPING.get(testExecutionResult.getStatus()));
		rq.setEndTime(Calendar.getInstance().getTime());
		this.launch.finishTestItem(this.idMapping.get(testIdentifier.getUniqueId()), rq);
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		FinishExecutionRQ rq = new FinishExecutionRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		this.launch.finish(rq);
	}

	private static final Map<TestExecutionResult.Status, String> STATUS_MAPPING = ImmutableMap.<TestExecutionResult.Status, String>builder()
			.put(TestExecutionResult.Status.SUCCESSFUL, Statuses.PASSED)
			.put(TestExecutionResult.Status.ABORTED, Statuses.FAILED)
			.put(TestExecutionResult.Status.FAILED, Statuses.FAILED)
			.build();

}
