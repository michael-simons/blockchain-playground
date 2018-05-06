package ac.simons.reactive.chains

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ChainTest {
    @Test
    fun hashingShouldWork() {
        val chain = Chain()
        val genesisBlock = genesisBlock()

        assertThat(chain.hash(genesisBlock))
                .isEqualTo("000000b642b67d8bea7cffed1ec990719a3f7837de5ef0f8ede36537e91cdc0e")
    }
}