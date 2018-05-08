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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.openjdk.jmh.annotations.Mode.AverageTime;
import static org.openjdk.jmh.annotations.Mode.Throughput;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * For interpreting the result, read <a href="https://blog.codecentric.de/en/2017/10/performance-measurement-with-jmh-java-microbenchmark-harness/">Performance measurement with JMH – Java Microbenchmark Harness</a>
 * by <a href="https://twitter.com/_Atze">Kevin</a>.
 *
 * <blockquote>"Mode.AverageTime a lower score is preferred, while using Mode.Throughput a higher value points to better performance."</blockquote>
 */
@BenchmarkMode(Throughput)
@OutputTimeUnit(NANOSECONDS)
@Fork(3)
public class EncodeBenchmark {
	public static void main(String[] args) throws Exception {
		final Options opt = new OptionsBuilder()
				.include(EncodeBenchmark.class.getSimpleName())
				.build();
		new Runner(opt).run();
	}

	@State(Scope.Benchmark)
	public static class BytesState {
		public byte[] value = "000000b642b67d8bea7cffed1ec990719a3f7837de5ef0f8ede36537e91cdc0e".getBytes(UTF_8);
	}

	@Benchmark
	public String encodeByFormat(BytesState state) {
		return HashUtils.ENCODE_BY_FORMAT.apply(state.value);
	}

	@Benchmark
	public String encodeWithGuavaAlgorith(BytesState state) {
		return HashUtils.ENCODE_WITH_GUAVA_ALGORITHM.apply(state.value);
	}

	@Benchmark
	public String encodeWithToHexString(BytesState state) {
		return HashUtils.ENCODE_WITH_TO_HEX_STRING.apply(state.value);
	}
}
