package ac.simons.reactive.chains;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class Chain {

   /**
    * A supplier containing the genesis block.
    */
   static Supplier<Block> DEFAULT_GENESIS_BLOCK = () -> new Block(1, 0, 1917336, List.of(new Transaction("b3c973e2-db05-4eb5-9668-3e81c7389a6d", 0, "I am Heribert Innoq")), "0");

   /**
    * Lokal block to json transformer, assuming json is generated via Jackson.
    */
   private static final BiFunction<ObjectMapper, Block, byte[]> TO_JSON = (objectMapper, block) -> {
      try {
         return objectMapper.writeValueAsBytes(block);
      } catch (JsonProcessingException e) {
         throw new RuntimeException(e);
      }
   };

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
    * Fancy way of hex encoding bytes.
    */
   private static final Function<byte[], String> ENCODE = bytes -> String.format("%064x", new BigInteger(1, bytes));

   /**
    * Local instance of an object mapper.
    */
   private final ObjectMapper objectMapper = new ObjectMapper();

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
    * State containing the latest index.
    */
   private final AtomicInteger lastIndex;

   /**
    * A queue with pending transactions.
    */
   private final Queue<Transaction> pendingTransactions;

   /**
    * A meter timing the computation of hashes.
    */
   private final Timer hashTimer;

   /**
    * A meter counting the number of mined blocks.
    */
   private final Counter blockCounter;

   public Chain() {
      this(DEFAULT_GENESIS_BLOCK);
   }

   public Chain(final Supplier<Block> genesisBlockSupplier) {
      this.blocks.add(genesisBlockSupplier.get());
      this.lastIndex = new AtomicInteger(this.blocks.size());

      // Prepare metrics
      // We directly interact with timers and counters, so we need their instances
      this.hashTimer = Metrics.timer("chain.hashes");
      this.blockCounter = Metrics.counter("chain.blocks.computed");

      // The gauge however only observes values and we should not interact with it directly
      this.pendingTransactions = Metrics.gauge(
            "chain.transactions.pending",
            new PriorityBlockingQueue<>(64, Comparator.comparingLong(Transaction::getTimestamp)),
            Queue::size);
   }

   public Mono<Transaction> queue(final String payload)  {
      return Mono.fromSupplier(() -> {
         var pendingTransaction = new Transaction(UUID.randomUUID().toString(), clock.millis(), payload);
         pendingTransactions.add(pendingTransaction);
         return pendingTransaction;
      });
   }

   public Mono<Block> mine() {
      final Consumer<Block> storeBlock = block -> {
         blocks.add(block);
         blockCounter.increment();
      };

      final Function<Block, Mono<Block>> nextBlock = template -> Flux.fromStream(Stream.iterate(0L, i -> i+1))
            .parallel().runOn(Schedulers.parallel())
            .map(proof -> template.newCandidateOf(proof))
            .filter(newCandidate -> hash(newCandidate).startsWith("000000"))
            .sequential()
            .next();

      final Supplier<Mono<Block>> latestBlock = () -> Mono.just(blocks.get(this.blocks.size() - 1));

      // Check first if there's a pending block, otherwise use the latest
      var miner = Optional.ofNullable(pendingBlocks.poll()).orElseGet(latestBlock)
               .map(this::hash)
               .map(hashOfPreviousBlock ->
                     new Block(lastIndex.incrementAndGet(), clock.millis(), -1, selectTransactions(5), hashOfPreviousBlock))
               .flatMap(nextBlock)
               .doOnSuccess(storeBlock)
               // This is paramount. The mono gets replayed on each subscription
               .cache();
      // Add it to the pending blocks in any case.
      pendingBlocks.offer(miner);
      return miner;
   }

   public Mono<List<Block>> getBlocks() {
      return Mono.just(Collections.unmodifiableList(this.blocks));
   }

   List<Transaction> selectTransactions(final int maxNumberOfTransactions) {
      return Stream.iterate(1, i -> i+1)
            .limit(maxNumberOfTransactions)
            .map(i -> pendingTransactions.poll())
            .takeWhile(t -> t!= null).collect(toList());
   }

   String hash(final Block block) {
      return hashTimer.record(() ->
         TO_JSON
            .andThen(DIGEST)
            .andThen(ENCODE)
            .apply(objectMapper, block));
   }
}