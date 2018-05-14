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

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.support.beans
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus.CREATED
import org.springframework.web.reactive.function.server.ServerResponse.*
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.reactive.function.server.router
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono

@SpringBootApplication
class Application

fun beans() = beans {
    bean {
        MeterRegistryCustomizer<MeterRegistry> { registry ->
            registry.config().commonTags("application", ref<Environment>().getProperty("spring.application.name", "unknown"))
        }
    }

    bean {
        with(Chain()) {
            router {
                GET("/", { ok().body(getStatus()) })
                GET("/mine", {
                    status(CREATED).body(mine())
                })
                POST("/transactions", { request ->
                    request.bodyToMono<String>()
                        .flatMap { queue(it) }
                        .flatMap {
                            created(
                                UriComponentsBuilder.fromUri(request.uri())
                                    .pathSegment("{id}")
                                    .buildAndExpand(mapOf("id" to it.id)).encode().toUri()
                            )
                                .body(Mono.just(it))
                        }
                })
                GET("/blocks", {
                    ok().body(getBlocks().map { mapOf("blocks" to it, "blockHeight" to it.size) })
                })
            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args){
        addInitializers(beans())
    }
}
