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
import io.micrometer.core.instrument.Metrics
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.scheduler.Schedulers
import java.time.Clock
import java.util.Queue
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.PriorityBlockingQueue
import java.util.function.Supplier

/**
 * A transaction with a payload.
 */
data class Transaction(val id: String, val timestamp: Long, val payload: String)

/**
 * A block inside the chain having a list of transactions.
 */
data class Block(
    val index: Long,
    val timestamp: Long,
    val proof: Long,
    val transactions: List<Transaction>,
    val previousBlockHash: String
)

/**
 * This chains status
 */
data class Status(val nodeId: String, val currentBlockHeight: Int)

/**
 * The genesis block supplier.
 */
fun genesisBlock() =
    Block(1, 0, 1917336, listOf(Transaction("b3c973e2-db05-4eb5-9668-3e81c7389a6d", 0, "I am Heribert Innoq")), "0")

class Chain(
    genesisBlock: Block = genesisBlock(),
    private val clock: Clock = Clock.systemUTC(),
    private val blockToJson: (block: Block) -> ByteArray
) {

    constructor() : this(blockToJson = {
        objectMapper.writeValueAsBytes(it)
    })

    companion object {
        val objectMapper = ObjectMapper()
    }

    /**
     * This chains id.
     */
    private val nodeId = UUID.randomUUID().toString()

    /**
     * Blocks that are currently being mined.
     */
    private val pendingBlocks = ConcurrentLinkedQueue<Mono<Block>>()

    /**
     * The actual chain.
     */
    private val blocks = mutableListOf(genesisBlock)

    private val pendingTransactions: Queue<Transaction> = Metrics.gauge(
        "chain.transactions.pending",
        PriorityBlockingQueue(64, compareBy<Transaction> { it.timestamp }),
        { it.size.toDouble() })!!

    private val hashTimer = Metrics.timer("chain.hashes")

    private val blockCounter = Metrics.counter("chain.blocks.computed")

    fun queue(payload: String) = Mono.fromSupplier {
        val pendingTransaction = Transaction(UUID.randomUUID().toString(), clock.millis(), payload)
        pendingTransactions.add(pendingTransaction)
        pendingTransaction
    }

    fun mine(): Mono<Block> {
        val storeBlock = { it: Block ->
            blocks += it
            blockCounter.increment()
        }

        val toTemplate = { it: Block ->
            it.copy(
                index = it.index + 1,
                timestamp = clock.millis(),
                transactions = selectTransactions(5),
                previousBlockHash = hash(it)
            )
        }

        val toNextBlock = { template: Block ->
            generateSequence(0L) { it + 1 }.toFlux()
                .parallel().runOn(Schedulers.parallel())
                .map { newProof -> template.copy(proof = newProof) }
                .filter { hash(it).startsWith("000000") }
                .sequential()
                .next()
        }

        synchronized(pendingBlocks) {
            // Check first if there's a pending block, otherwise use the latest
            val miner = (pendingBlocks.poll() ?: Mono.just(blocks.last()))
                .map(toTemplate)
                .flatMap(toNextBlock)
                .doOnSuccess(storeBlock)
                // This is paramount. The mono gets replayed on each subscription
                .cache()
            // Add it to the pending blocks in any case.
            pendingBlocks.add(miner)
            return miner
        }
    }

    fun getBlocks() = Mono.just(this.blocks.toList())

    fun getStatus() = Mono.just(Status(this.nodeId, this.blocks.size))

    internal fun selectTransactions(maxNumberOfTransactions: Int) = (1..maxNumberOfTransactions)
        .map { pendingTransactions.poll() }
        .takeWhile { it != null }
        .toList()

    internal fun hash(block: Block) = hashTimer.record(Supplier {
        blockToJson(block)
            .run(::digest)
            .run(::encode)
    })
}