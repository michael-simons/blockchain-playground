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

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class ChainTest {
    @Test
    fun `hashing should work`() {
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