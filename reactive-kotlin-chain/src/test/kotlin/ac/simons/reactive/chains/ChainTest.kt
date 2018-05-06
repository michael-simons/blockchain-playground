package ac.simons.reactive.chains

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.stream.Collectors
import java.util.stream.Stream

class ChainTest {
    @Test
    fun hashingShouldWork() {
        val chain = Chain()
        val genesisBlock = genesisBlock()

        assertThat(chain.hash(genesisBlock))
                .isEqualTo("000000b642b67d8bea7cffed1ec990719a3f7837de5ef0f8ede36537e91cdc0e")
    }

    @Test
    fun selectTransactionsShouldWork() {
        val chain = Chain()
        Flux.concat<Mono<Transaction>> {
            chain.queue("a")
            chain.queue("b")
            chain.queue("c")
        }.collectList().subscribe {
            assertThat(chain.selectTransactions(2)).hasSize(2)
            assertThat(chain.selectTransactions(2)).hasSize(1)
            assertThat(chain.selectTransactions(2)).hasSize(0)
        }
    }
}