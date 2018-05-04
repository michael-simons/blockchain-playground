package ac.simons.reactive.chains;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class ChainTest {
   @Test
   public void hashingShouldWork() {
      var chain = new Chain();
      var genesisBlock = Chain.DEFAULT_GENESIS_BLOCK.get();

      assertThat(chain.hash(genesisBlock))
            .isEqualTo("000000b642b67d8bea7cffed1ec990719a3f7837de5ef0f8ede36537e91cdc0e");
   }

   @Test
   public void selectTransactionsShouldWork() {
      var chain = new Chain();

      Flux.concat(
         Stream.of("a", "b", "c").map(chain::queue).collect(Collectors.toList())
      ).collectList().subscribe(allTransactions -> {
         assertThat(chain.selectTransactions(2)).hasSize(2);
         assertThat(chain.selectTransactions(2)).hasSize(1);
         assertThat(chain.selectTransactions(2)).hasSize(0);
      });
   }
}