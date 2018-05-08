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