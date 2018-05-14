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

import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import java.util.concurrent.atomic.AtomicInteger

/**
 * Sealed classes allow only a restricted set of subtypes which must be defined in "this" file.
 */
sealed class Event<D> {
    abstract val id: Int
    abstract val data: D
}

data class NewBlockEvent(override val id: Int, override val data: Block) : Event<Block>()

data class NewTransactionEvent(override val id: Int, override val data: Transaction) : Event<Transaction>()

data class NewNodeEvent(override val id: Int, override val data: Node) : Event<Node>()

class EventPublisher() {
    private val idGenerator = AtomicInteger()
    private val eventStream = EmitterProcessor.create<Event<*>>(false)
    private val eventSink = eventStream.sink()

    fun publish(data: Any) {
        val nextId = idGenerator.incrementAndGet()
        val event = when(data) {
            is Block -> NewBlockEvent(nextId, data)
            is Transaction -> NewTransactionEvent(nextId, data)
            is Node -> NewNodeEvent(nextId, data)
            else -> throw IllegalArgumentException()
        }
        eventSink.next(event)
    }

    fun events(): Flux<Event<*>> = eventStream
}