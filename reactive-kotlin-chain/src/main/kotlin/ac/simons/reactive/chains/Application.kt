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
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.codec.CodecCustomizer
import org.springframework.context.support.beans
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_STREAM_JSON
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.server.ServerResponse.*
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.reactive.function.server.router
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono

@SpringBootApplication
class Application

fun beans() = beans {

    // Customize metrics by adding the applications name
    bean {
        MeterRegistryCustomizer<MeterRegistry> { registry ->
            registry.config().commonTags("application", ref<Environment>().getProperty("spring.application.name", "unknown"))
        }
    }

    // Some JSON customization
    bean<EventModule>()
    bean {
        CodecCustomizer { customizer ->
            customizer.customCodecs().decoder(Jackson2JsonDecoder(ref()))
        }
    }

    // Routing and control flow
    bean {
        val chain = Chain()

        // Events are not published from within the chain but from within this application
        // So no need to dependency inject anything into the chain.
        val eventPublisher = EventPublisher()

        // Listen for events on other nodes
        eventPublisher.events().filter { it is NewNodeEvent }
                .flatMap { (it as NewNodeEvent).data.listenTo() }
                .subscribe {
                    when(it) {
                        is NewBlockEvent -> println("New block on other node")
                        is NewTransactionEvent -> chain.queue(it.data)
                        is NewNodeEvent -> throw IllegalArgumentException()
                    }
                }
        // Create node registry with Boots WebClientBuilder
        val nodeRegistry = NodeRegistry(ref())

        router {
            with(chain) {
                GET("/", { ok().body(getStatus()) })
                GET("/mine", {
                    status(CREATED).body(mine().doOnNext(eventPublisher::publish))
                })
                POST("/transactions", { request ->
                    request.bodyToMono<String>()
                            .flatMap { queue(it) }
                            .doOnNext(eventPublisher::publish)
                            .flatMap {
                                created(UriComponentsBuilder.fromUri(request.uri())
                                        .pathSegment("{id}")
                                        .buildAndExpand(mapOf("id" to it.id)).encode().toUri()
                                ).body(Mono.just(it))
                            }
                })
                GET("/blocks", {
                    ok().body(getBlocks().map { mapOf("blocks" to it, "blockHeight" to it.size) })
                })
            }

            with(nodeRegistry) {
                POST("/nodes/register", { request ->
                    request.bodyToMono<String>()
                            .flatMap { register(it) }
                            .doOnNext(eventPublisher::publish)
                            .flatMap { ok().body(Mono.just(it)) }
                })
            }

            with(eventPublisher) {
                GET("/events", {
                    ok().contentType(APPLICATION_STREAM_JSON).body(events())
                })
            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args) {
        addInitializers(beans())
    }
}
