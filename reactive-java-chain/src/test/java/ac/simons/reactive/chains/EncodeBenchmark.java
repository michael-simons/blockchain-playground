package ac.simons.reactive.chains;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.openjdk.jmh.annotations.Mode.AverageTime;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@BenchmarkMode(AverageTime)
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
