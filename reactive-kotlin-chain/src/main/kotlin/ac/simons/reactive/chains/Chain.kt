package ac.simons.reactive.chains

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.Metrics
import reactor.core.publisher.Mono
import java.time.Clock
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.PriorityBlockingQueue
import java.util.function.Supplier
import java.util.function.ToDoubleFunction
import java.util.function.ToLongFunction

/**
 * A transaction with a payload.
 */
data class Transaction(val id: String, val timestamp: Long, val payload: String)

/**
 * A block inside the chain having a list of transactions.
 */
data class Block(val index: Long, val timestamp: Long, val proof: Long, val transactions: List<Transaction>, val previousBlockHash: String)

/**
 * The genesis block supplier.
 */
fun genesisBlock() = Block(1, 0, 1917336, listOf(Transaction("b3c973e2-db05-4eb5-9668-3e81c7389a6d", 0, "I am Heribert Innoq")), "0")

class Chain(
        private val blockToJson: (block: Block) -> ByteArray,
        private val clock: Clock = Clock.systemUTC()
) {
    companion object {
        operator fun invoke(): Chain {
            val objectMapper = ObjectMapper()
            return Chain({ block -> objectMapper.writeValueAsBytes(block) })
        }
    }

    /**
     * Blocks that are currently being mined.
     */
    private val pendingBlocks = ConcurrentLinkedQueue<Mono<Block>>()

    /**
     * The actual chain.
     */
    private val blocks = mutableListOf<Block>()

    private val pendingTransactions: Queue<Transaction> = Metrics.gauge(
            "chain.transactions.pending",
            PriorityBlockingQueue(64, Comparator.comparingLong<Transaction>{ it.timestamp }),
            { it.size.toDouble() })!!

    private val hashTimer = Metrics.timer("chain.hashes")

    fun hash(block: Block) = hashTimer.record(Supplier {
        blockToJson(block)
                .run(::digest)
                .run(::encode)
    })
}