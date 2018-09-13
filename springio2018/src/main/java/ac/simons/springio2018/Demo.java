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

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.dropwizard.DropwizardConfig;
import io.micrometer.core.instrument.dropwizard.DropwizardMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;

public class Demo {
	public static void main(String... args) {
		Metrics.addRegistry(new SimpleMeterRegistry());

		// We can use Dropwizard Console Reporter and Dropwizard Registries as well
		var dropwizardsMetricRegistry = new MetricRegistry();
		Metrics.addRegistry(consoleLoggingRegistry(dropwizardsMetricRegistry));
		var consoleReporter = consoleReporter(dropwizardsMetricRegistry);

		var meterRegistry = Metrics.globalRegistry;

		// Create a counter
		var ehemCounter = meterRegistry.counter("presentation.filler.words", "word", "ehem");
		ehemCounter.increment();

		// Or via the builder
		var sooooCounter = Counter.builder("presentation.filler.words")
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

		Supplier<String> slideSupplier = () -> {
			try {
				Thread.sleep((long) (1_000 * ThreadLocalRandom.current().nextDouble()));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return "Next slide";
		};

		Stream.iterate(1, i -> i <= 3, i -> i + 1).forEach(i ->
			System.out.println(slideTimer.record(slideSupplier))
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
		consoleReporter.report();
	}

	static MeterRegistry consoleLoggingRegistry(MetricRegistry dropwizardRegistry) {
		DropwizardConfig consoleConfig = new DropwizardConfig() {

			@Override
			public String prefix() {
				return "console";
			}

			@Override
			public String get(String key) {
				return null;
			}
		};

		return new DropwizardMeterRegistry(consoleConfig, dropwizardRegistry, HierarchicalNameMapper.DEFAULT,
			Clock.SYSTEM) {
			@Override
			protected Double nullGaugeValue() {
				return null;
			}
		};
	}

	static ConsoleReporter consoleReporter(MetricRegistry dropwizardRegistry) {
		ConsoleReporter reporter = ConsoleReporter.forRegistry(dropwizardRegistry)
			.convertRatesTo(TimeUnit.SECONDS)
			.convertDurationsTo(TimeUnit.MILLISECONDS)
			.build();
		return reporter;
	}
}
