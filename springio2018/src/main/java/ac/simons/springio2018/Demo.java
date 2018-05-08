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
package ac.simons.springio2018;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class Demo {
	public static void main(String... args) {
		// Let's start with a registry
		var meterRegistry = new SimpleMeterRegistry();
		// Use the global registry
		// Metrics.addRegistry(new SimpleMeterRegistry());
		// var meterRegistry = Metrics.globalRegistry;


		// Create a counter
		var ehemCounter = meterRegistry.counter("presentation.filler-words", "word", "ehem");
		ehemCounter.increment();

		// Or via the builder
		var sooooCounter = Counter.builder("presentation.filler-words")
			.tag("word", "soooo…")
			.register(meterRegistry);
		sooooCounter.increment(2);

		System.out.println(ehemCounter.count());
		System.out.println(sooooCounter.count());

		// A timer
		var slideTimer = Timer.builder("presentation.slide.timer")
			.description("This is a timer.")
			.tags(
				"conference", "Spring I/O",
				"place", "Barcelona"
			)
			.register(meterRegistry);

		Stream.iterate(1, i -> i <= 3, i -> i + 1).forEach(i ->
			slideTimer.record(() -> {
				try {
					Thread.sleep((long) (1_000 * ThreadLocalRandom.current().nextDouble()));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			})
		);

		System.out.println(slideTimer.max(TimeUnit.SECONDS));
		System.out.println(slideTimer.count());

		// And last but not least the Gauge
		var audience = meterRegistry.gauge("presentation.audience", new ArrayList<String>(), List::size);

		// What is audience?
		// It's the actual list
		audience.add("Mark");
		audience.add("Oliver");

		System.out.println(meterRegistry.find("presentation.audience").gauge().value());

		audience.add("Alice");
		audience.add("Tina");
		audience.remove("Mark");

		System.out.println(meterRegistry.find("presentation.audience").gauge().value());

		// One hardly interacts with a gauge direclty, but it's possible
		Gauge usedMemory = Gauge.builder("jvm.memory.used", Runtime.getRuntime(), r -> r.totalMemory() - r.freeMemory())
			.baseUnit("bytes")
			.tag("host", "chronos")
			.tag("region", "my-desk")
			.register(meterRegistry);

		System.out.println(usedMemory.value());
	}
}
