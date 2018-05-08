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

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.created;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import static org.springframework.web.reactive.function.server.ServerResponse.status;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.core.publisher.Mono;

@SpringBootApplication
public class ReactiveJavaChainApplication {

	@Bean
	public MeterRegistryCustomizer<?> commonTagsCustomizer(@Value("${spring.application.name}") final String applicationName) {
		return registry -> registry.config().commonTags("application", applicationName);
	}

	@Bean
	public Chain chain() {
		return Chain.defaultChain();
	}

	@Bean
	RouterFunction<?> router(final Chain chain) {
		return route(GET("/mine"), request -> status(CREATED).body(chain.mine(), Block.class))
				.and(route(POST("/transactions"), request ->
						request.bodyToMono(String.class)
								.flatMap(chain::queue)
								.flatMap(p -> created(
										UriComponentsBuilder.fromUri(request.uri())
												.pathSegment("{id}").buildAndExpand(Map.of("id", p.getId())).encode().
												toUri())
										.body(Mono.just(p), Transaction.class))))
				.and(route(GET("/blocks"), request -> ok().body(
						chain.getBlocks().map(blocks -> Map.of("blocks", blocks, "blockHeight", blocks.size())), Map.class)));
	}

	public static void main(String[] args) {
		SpringApplication.run(ReactiveJavaChainApplication.class, args);
	}
}