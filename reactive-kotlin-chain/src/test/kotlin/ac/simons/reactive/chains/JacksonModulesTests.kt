/*
 * Copyright 2018 michael-simons.eu.
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
package ac.simons.reactive.chains

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.slf4j.LoggerFactory

@DisplayName("Jackson modules")
class JacksonModulesTests {

    private val log = LoggerFactory.getLogger(JacksonModulesTests::class.java)

    private val objectMapper = ObjectMapper()

    init {
        objectMapper.registerModules(KotlinModule(), EventModule())
    }

    @Nested
    @DisplayName("Event")
    inner class EventModuleTest {
        @Test
        fun `Should serialize and deserialize to and from correct event type`() {
            val sourceEvent = NewBlockEvent(1, genesisBlock())
            val serializedEvent = objectMapper.writeValueAsString(sourceEvent)

            log.debug("Serialized event to '{}'", serializedEvent)

            val deserializedEvent : Event<*> = objectMapper.readValue(serializedEvent)

            when(deserializedEvent) {
                is NewBlockEvent -> assertThat(deserializedEvent).isEqualTo(sourceEvent)
                else -> fail { "Unexpected event type: " + deserializedEvent::class }
            }
        }
    }
}