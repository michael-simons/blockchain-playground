package ac.simons.reactive.chains;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;

public class ChainTest {
   @Test
   public void hashingShouldWork() {
      var chain = Chain.defaultChain();
      var genesisBlock = Chain.DEFAULT_GENESIS_BLOCK.get();

      assertThat(chain.hash(genesisBlock))
            .isEqualTo("000000b642b67d8bea7cffed1ec990719a3f7837de5ef0f8ede36537e91cdc0e");
   }

   @Test
   public void selectTransactionsShouldWork() {
      var chain = Chain.defaultChain();

      Flux.concat(
         Stream.of("a", "b", "c").map(chain::queue).collect(Collectors.toList())
      ).collectList().subscribe(allTransactions -> {
         assertThat(chain.selectTransactions(2)).hasSize(2);
         assertThat(chain.selectTransactions(2)).hasSize(1);
         assertThat(chain.selectTransactions(2)).hasSize(0);
      });
   }
}