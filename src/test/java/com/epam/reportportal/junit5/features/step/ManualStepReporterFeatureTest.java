/*
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.junit5.features.step;

import com.epam.reportportal.junit5.StepReporterTest;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.step.StepReporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
@ExtendWith(StepReporterTest.TestExtension.class)
public class ManualStepReporterFeatureTest {

	public static final String FIRST_NAME = "I am the first nested step";
	public static final String SECOND_NAME = "I am the second nested step";
	public static final String THIRD_NAME = "I am the third nested step";

	private static final Logger LOGGER = LoggerFactory.getLogger(ManualStepReporterFeatureTest.class);

	@Test
	public void manualStepTest() throws InterruptedException {

		StepReporter stepReporter = Launch.currentLaunch().getStepReporter();
		stepReporter.sendStep(FIRST_NAME);
		LOGGER.info("First info log of the first step");
		LOGGER.info("Second info log of the first step");
		Thread.sleep(100);

		stepReporter.sendStep(SECOND_NAME);
		LOGGER.error("First error log of the second step");

		Thread.sleep(100);
		stepReporter.sendStep(ItemStatus.FAILED, THIRD_NAME, new File("pug/unlucky.jpg"));
		LOGGER.error("Second error log of the second step");

		Thread.sleep(100);
		stepReporter.finishPreviousStep();

		LOGGER.error("Main test method error log");
	}
}
