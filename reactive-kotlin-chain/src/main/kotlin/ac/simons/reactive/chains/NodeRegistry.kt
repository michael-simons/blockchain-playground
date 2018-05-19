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

import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

/**
 * Representation of a node.
 */
data class Node(val id: String, val host: String, private val webClient: WebClient) {
    fun listenTo() = webClient.get().uri("/events").retrieve().bodyToFlux<Event<*>>()
}

class NodeRegistry(private val webClientBuilder: WebClient.Builder = WebClient.builder()) {
    private val nodes = mutableSetOf<Node>()

    fun register(host: String) = Mono.fromSupplier {
        val newNode = Node(UUID.randomUUID().toString(), host, webClientBuilder.baseUrl(host).build())
        nodes += newNode
        newNode
    }
}