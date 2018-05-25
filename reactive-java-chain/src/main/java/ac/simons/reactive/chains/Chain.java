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
package ac.simons.reactive.chains;

import static java.util.stream.Collectors.toList;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class Chain {

	/**
	 * A supplier containing the genesis block.
	 */
	static Supplier<Block> DEFAULT_GENESIS_BLOCK = () -> new Block(1, 0, 1917336, List.of(new Transaction("b3c973e2-db05-4eb5-9668-3e81c7389a6d", 0, "I am Heribert Innoq")), "0");

	/**
	 * Digester wrapping a MessageDigest, which is not thread safe.
	 */
	private static final Function<byte[], byte[]> DIGEST = bytes -> {
		try {
			return MessageDigest.getInstance("SHA-256").digest(bytes);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	};

	/**
	 * Creates a base64 string from a byte array.
	 */
	private static final Function<byte[], String> ENCODE = HashUtils.ENCODE_WITH_GUAVA_ALGORITHM;

	/**
	 * A function to generate JSON from a block.
	 */
	private final Function<Block, byte[]> blockToJson;

	/**
	 * Can be injected to compute blocks in different timezones.
	 */
	private final Clock clock = Clock.systemUTC();

	private final Queue<Mono<Block>> pendingBlocks = new ConcurrentLinkedQueue<>();

	/**
	 * The actual chain.
	 */
	private final List<Block> blocks = Collections.synchronizedList(new ArrayList<>());

	/**
	 * A queue with pending transactions.
	 */
	private final Queue<Transaction> pendingTransactions =
		new PriorityBlockingQueue<>(64, Comparator.comparingLong(Transaction::getTimestamp));

	/**
	 * A meter timing the computation of hashes.
	 */
	private final Timer hashTimer = Metrics.timer("chain.hashes");

	public static Chain defaultChain() {
		final ObjectMapper objectMapper = new ObjectMapper();
		return new Chain(DEFAULT_GENESIS_BLOCK.get(), block -> {
			try {
				return objectMapper.writeValueAsBytes(block);
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private Chain(Block genesisBlock, final Function<Block, byte[]> blockToJson) {
		this.blocks.add(genesisBlock);
		this.blockToJson = blockToJson;
	}

	public int getLength() {
		return this.blocks.size();
	}

	public int getNumberOfPendingTransactions() {
		return this.pendingTransactions.size();
	}

	public Mono<Transaction> queue(final String payload) {
		return Mono.fromSupplier(() -> {
			var pendingTransaction = new Transaction(UUID.randomUUID().toString(), clock.millis(), payload);
			pendingTransactions.add(pendingTransaction);
			return pendingTransaction;
		});
	}

	public Mono<Block> mine() {
		final Function<Block, Block> toTemplate = previousBlock ->
				new Block(previousBlock.getIndex() + 1, clock.millis(), -1, selectTransactions(5), hash(previousBlock));

		final Function<Block, Mono<Block>> toNextBlock = template -> Flux.fromStream(Stream.iterate(0L, i -> i + 1))
				.parallel().runOn(Schedulers.parallel())
				.map(proof -> template.newCandidateOf(proof))
				.filter(newCandidate -> hash(newCandidate).startsWith("000000"))
				.sequential()
				.next();

		final Supplier<Mono<Block>> latestBlock = () -> Mono.just(blocks.get(this.blocks.size() - 1));

		synchronized (pendingBlocks) {
			// Check first if there's a pending block, otherwise use the latest
			var miner = Optional.ofNullable(pendingBlocks.poll()).orElseGet(latestBlock)
					.map(toTemplate)
					.flatMap(toNextBlock)
					.doOnSuccess(blocks::add)
					// This is paramount. The mono gets replayed on each subscription
					.cache();
			// Add it to the pending blocks in any case.
			pendingBlocks.add(miner);
			return miner;
		}
	}

	public Mono<List<Block>> getBlocks() {
		return Mono.just(Collections.unmodifiableList(this.blocks));
	}

	List<Transaction> selectTransactions(final int maxNumberOfTransactions) {
		return Stream.iterate(1, i -> i + 1)
				.limit(maxNumberOfTransactions)
				.map(i -> pendingTransactions.poll())
				.takeWhile(t -> t != null).collect(toList());
	}

	String hash(final Block block) {
		return hashTimer.record(() ->
				blockToJson
						.andThen(DIGEST)
						.andThen(ENCODE)
						.apply(block));
	}
}