package ac.simons.reactive.chains

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ReactiveKotlinChainApplication

fun main(args: Array<String>) {
    runApplication<ReactiveKotlinChainApplication>(*args)
}
