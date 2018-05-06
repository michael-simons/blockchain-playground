package ac.simons.reactive.chains

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
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
        MeterRegistryCustomizer<MeterRegistry> { registry -> registry.config().commonTags("application", ref<Environment>().getProperty("spring.application.name")) }
    }

    bean {
        val chain = Chain()
        router {
            GET("/mine", {
                ok().body(chain.mine())
            })
            POST("/transactions", { request ->
                request.bodyToMono<String>()
                        .flatMap { chain.queue(it) }
                        .flatMap {
                            created(UriComponentsBuilder.fromUri(request.uri())
                                    .pathSegment("{id}")
                                    .buildAndExpand(mapOf("id" to it.id)).encode().toUri())
                                    .body(Mono.just(it))
                        }
            })
            GET("/blocks", {
                ok().body(chain.getBlocks().map { blocks -> mapOf("blocks" to blocks, "blockHeight" to blocks.size) })
            })
        }
    }
}


fun main(args: Array<String>) {
    SpringApplicationBuilder()
            .sources(ReactiveKotlinChainApplication::class.java)
            .initializers(beans())
            .run(*args)
}
