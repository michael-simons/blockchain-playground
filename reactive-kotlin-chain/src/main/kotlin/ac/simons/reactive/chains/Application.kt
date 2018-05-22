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

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.boot.web.codec.CodecCustomizer
import org.springframework.context.ApplicationListener
import org.springframework.context.support.beans
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.MediaType.APPLICATION_STREAM_JSON
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.web.reactive.function.server.ServerResponse.*
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.reactive.function.server.router
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.net.DatagramPacket
import java.net.InetAddress
import java.util.logging.Logger

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

    bean("nodeRegistery") { NodeRegistry(ref()) }

    bean("chain") { Chain() }

    bean("eventPublisher") { EventPublisher() }

    bean {
        ApplicationListener<ApplicationReadyEvent> {
            val logger = Logger.getLogger(Application::class.qualifiedName!!)

            val nodeRegistry = ref<NodeRegistry>()

            // Listen for events on other nodes
            ref<EventPublisher>().events().filter { it is NewNodeEvent }
                    .doOnNext { println(it) }
                    .flatMap { nodeRegistry.listenTo((it as NewNodeEvent).data) }
                    .subscribe {
                        when (it) {
                            is NewBlockEvent -> logger.fine { "New block on other node" }
                            is NewTransactionEvent -> ref<Chain>().queue(it.data)
                            is NewNodeEvent -> logger.fine { "New node on other node "}
                        }
                    }

            val serverProperties = ref<ServerProperties>()
            val address = serverProperties.address ?: InetAddress.getLocalHost()
            val socket = openMulticastSocket(address)

            with(ref<Chain>()) {
                // Publish local port and address of this instance on the network and also
                socket.send(createSSDPAlivePacket(address, serverProperties.port))

                // register with nodes that are announced against this node
                Flux.create<DatagramPacket> { emitter ->
                    while (true) {
                        val dp = DatagramPacket(ByteArray(8192), 8129)
                        socket.receive(dp)
                        emitter.next(dp)
                    }
                }.subscribeOn(Schedulers.elastic())
                        .flatMap { readSSDPAlivePacket(it) }
                        .filter { it.isNotBlank() }
                        .flatMap { nodeRegistry.register(it) }
                        .doOnNext{ ref<EventPublisher>().publish(it) }
                        .subscribe { logger.info { "Registered to new node ${it}" } }
            }
        }
    }

    // Routing and control flow
    bean {
        val eventPublisher = ref<EventPublisher>()

        router {
            with(ref<Chain>()) {
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

            with(ref<NodeRegistry>()) {
                POST("/nodes/register", { request ->
                    request.bodyToMono<String>()
                            .flatMap { register(it) }
                            .doOnNext(eventPublisher::publish)
                            .doOnNext { println("what the fuck?!") }
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
