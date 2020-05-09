/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.bindings;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.bindings.CassandraBindingsPropertiesProcessor.KIND;

@DisplayName("Cassandra BindingsPropertiesProcessor")
final class CassandraBindingsPropertiesProcessorTest {

    @Test
    @DisplayName("contributes properties")
    void test() {
        HashMap<String, Object> properties = new HashMap<>();

        new CassandraBindingsPropertiesProcessor().process(new Bindings(
                new Binding("test-name", Paths.get("test-path"),
                        Collections.singletonMap("kind", KIND),
                        new FluentMap()
                                .withEntry("node_ips", "test-node-ips")
                                .withEntry("password", "test-password")
                                .withEntry("port", "test-port")
                                .withEntry("username", "test-username")
                )
        ), properties);

        assertThat(properties)
                .containsEntry("spring.data.cassandra.contact-points", "test-node-ips")
                .containsEntry("spring.data.cassandra.password", "test-password")
                .containsEntry("spring.data.cassandra.port", "test-port")
                .containsEntry("spring.data.cassandra.username", "test-username");
    }

}