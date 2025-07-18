/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.webclient.observation;

import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.web.reactive.function.client.ClientRequestObservationConvention;
import org.springframework.web.reactive.function.client.DefaultClientRequestObservationConvention;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ObservationWebClientCustomizer}
 *
 * @author Brian Clozel
 */
class ObservationWebClientCustomizerTests {

	private static final String TEST_METRIC_NAME = "http.test.metric.name";

	private final TestObservationRegistry observationRegistry = TestObservationRegistry.create();

	private final ClientRequestObservationConvention observationConvention = new DefaultClientRequestObservationConvention(
			TEST_METRIC_NAME);

	private final ObservationWebClientCustomizer customizer = new ObservationWebClientCustomizer(
			this.observationRegistry, this.observationConvention);

	private final WebClient.Builder clientBuilder = WebClient.builder();

	@Test
	void shouldCustomizeObservationConfiguration() {
		this.customizer.customize(this.clientBuilder);
		assertThat(this.clientBuilder).hasFieldOrPropertyWithValue("observationRegistry", this.observationRegistry);
		assertThat(this.clientBuilder).extracting("observationConvention")
			.isInstanceOf(DefaultClientRequestObservationConvention.class)
			.hasFieldOrPropertyWithValue("name", TEST_METRIC_NAME);
	}

}
