package ac.simons.reactive.chains;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.created;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
public class ReactiveJavaChainApplication {

	@Bean
	public MeterRegistryCustomizer<?> commonTagsCustomizer(@Value("${spring.application.name}") final String applicationName) {
		return registry -> registry.config().commonTags("application", applicationName);
	}

	@Bean
	public Chain chain() {
		return new Chain();
	}

	@Bean
	RouterFunction<?> router(final Chain chain) {
		return route(GET("/mine"), request -> ok().body(chain.mine(), Block.class))
				 .and(route(POST("/transactions"), request ->
						request.bodyToMono(String.class)
								.flatMap(chain::queue)
								.flatMap(p -> created(
										UriComponentsBuilder.fromUri(request.uri()).pathSegment("{id}").buildAndExpand(Map.of("id", p.getId())).encode().toUri())
										.body(Mono.just(p), Transaction.class))))
				 .and(route(GET("/blocks"), request -> ok().body(
				 		chain.getBlocks().map(blocks -> Map.of("blocks", blocks, "blockHeight", blocks.size())), Map.class)));
	}

	public static void main(String[] args) {
		SpringApplication.run(ReactiveJavaChainApplication.class, args);
	}
}