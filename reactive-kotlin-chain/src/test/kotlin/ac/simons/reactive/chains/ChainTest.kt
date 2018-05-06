package ac.simons.reactive.chains

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class ChainTest {
    @Test
    fun `hashing should Work`() {
        val chain = Chain()
        val genesisBlock = genesisBlock()

        assertThat(chain.hash(genesisBlock))
            .isEqualTo("000000b642b67d8bea7cffed1ec990719a3f7837de5ef0f8ede36537e91cdc0e")
    }

    @Test
    fun `select transactions should work`() {
        with(Chain()) {
            Flux.concat<Mono<Transaction>> {
                queue("a")
                queue("b")
                queue("c")
            }.collectList().subscribe {
                (2 downTo 0).forEach {
                    assertThat(selectTransactions(2)).hasSize(it)
                }
            }
        }
    }
}