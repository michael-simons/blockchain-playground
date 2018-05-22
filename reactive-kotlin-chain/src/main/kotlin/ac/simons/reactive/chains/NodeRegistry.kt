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
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.logging.Logger

/**
 * Representation of a node.
 */
data class Node(val id: String, val host: String)

/**
 * Used for retrieving events from a node
 */
class NodeClient(base: WebClient) : WebClient by base {
    var active = false

    fun retrieveEvents() : Flux<Event<*>> {
        active = true
        return get().uri("/events").retrieve().bodyToFlux<Event<*>>()
    }
}

class NodeRegistry(
        private val webClientBuilder: WebClient.Builder = WebClient.builder()
) {
    private val nodes = mutableSetOf<Node>()

    private val nodeClients = mutableMapOf<String, NodeClient>()

    companion object {
        val log = Logger.getLogger(NodeRegistry::class.qualifiedName!!)
    }

    /**
     * Registers the given host to the registry and creates a webclient for it.
     */
    fun register(host: String) = if (nodes.find { it.host == host } != null) Mono.empty() else
        Mono.just(webClientBuilder.baseUrl(host).build())
                .flatMap { client ->
                    client.get().retrieve().bodyToMono<Status>()
                            .map { Node(it.nodeId, host) }
                            .doOnSuccess {
                                nodes += it
                                nodeClients += it.id to NodeClient(client)
                            }
                }.doOnError { Mono.empty<Node>() }

    fun listenTo(node: Node) = nodeClients[node.id]
            ?.takeUnless { it.active }
            ?.let { it.retrieveEvents().doOnError { unregister(node) } } ?: Flux.empty()

    internal fun unregister(node: Node): Flux<Node> {
        nodeClients -= node.id
        nodes -= node
        log.info{ "Removed node ${node}" }
        return Flux.empty()
    }
}
