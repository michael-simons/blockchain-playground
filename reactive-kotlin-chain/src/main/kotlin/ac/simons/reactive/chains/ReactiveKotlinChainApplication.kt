package ac.simons.reactive.chains

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.runApplication
import org.springframework.context.support.beans
import org.springframework.core.env.Environment
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.reactive.function.server.router
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono

@SpringBootApplication
class ReactiveKotlinChainApplication

fun beans() = beans {
    bean {
        MeterRegistryCustomizer<MeterRegistry> { registry ->
            registry.config().commonTags("application", ref<Environment>().getProperty("spring.application.name"))
        }
    }

    bean {
        with(Chain()) {
            router {
                GET("/mine", {
                    ok().body(mine())
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
    runApplication<ReactiveKotlinChainApplication>(*args){
        addInitializers(beans())
    }
}
