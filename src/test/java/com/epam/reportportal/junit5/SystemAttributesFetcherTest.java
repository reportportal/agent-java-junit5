/*
 * Copyright 2019 EPAM Systems
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

import com.epam.ta.reportportal.ws.model.attribute.ItemAttributeResource;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
class SystemAttributesFetcherTest {

	private static final List<String> expectedKeys = Arrays.asList("jvm", "os", "agent", "skippedIssue");

	@Test
	void systemAttributesRetrievingTest() {
		Set<ItemAttributesRQ> systemAttributes = SystemAttributesFetcher.collectSystemAttributes(false);
		assertNotNull(systemAttributes);
		assertEquals(expectedKeys.size(), systemAttributes.size());
		systemAttributes.stream().map(ItemAttributesRQ::isSystem).forEach(Assertions::assertTrue);
		assertTrue(systemAttributes.stream().map(ItemAttributeResource::getKey).collect(Collectors.toList()).containsAll(expectedKeys));
		assertEquals(
				expectedKeys.size(),
				(int) systemAttributes.stream().map(ItemAttributeResource::getValue).filter(StringUtils::isNotBlank).count()
		);
	}
}