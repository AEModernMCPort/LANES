package lanes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ConnectParamTest {

	@Nested
	class TestUsingDoubleSample {

		private final LayerDoubleSample layer = new LayerDoubleSample("sample-layer");

		@Test
		public void testSampleImpl(){
			//const
			assertEquals(0, ConnectParamDoubleSample.ZERO.val, "0≠0");
			assertEquals(1, ConnectParamDoubleSample.ONE.val, "1≠1");
			assertEquals(Double.POSITIVE_INFINITY, ConnectParamDoubleSample.INF.val, "∞≠∞");
			//inst
			assertDoesNotThrow(() -> {
				assertEquals(0, new ConnectParamDoubleSample(0).val, "0≠0");
				assertEquals(2.5, new ConnectParamDoubleSample(2.5).val, "2.5≠2.5");
				assertEquals(7.5E9, new ConnectParamDoubleSample(7.5E9).val, "7.5E9≠7.5E9");
				assertEquals(1.25E-41, new ConnectParamDoubleSample(1.25E-41).val, "1.25E-41≠1.25E-41");
			});
			assertThrows(IllegalArgumentException.class, () -> new ConnectParamDoubleSample(-1), "-1 is valid");
			assertThrows(IllegalArgumentException.class, () -> new ConnectParamDoubleSample(Double.NaN), "NaN is valid");
			assertThrows(IllegalArgumentException.class, () -> new ConnectParamDoubleSample(Double.POSITIVE_INFINITY * 0.0), "NaN is valid");
			//⊃
			assertTrue(ConnectParamDoubleSample.ZERO.isSufficientFor(ConnectParamDoubleSample.ZERO), "0 >= 0 is false!");
			assertTrue(ConnectParamDoubleSample.ONE.isSufficientFor(ConnectParamDoubleSample.ZERO), "1 >= 0 is false!");
			assertTrue(ConnectParamDoubleSample.ONE.isSufficientFor(ConnectParamDoubleSample.ONE), "1 >= 1 is false!");
			assertTrue(ConnectParamDoubleSample.INF.isSufficientFor(ConnectParamDoubleSample.ZERO), "∞ >= 0 is false!");
			assertTrue(ConnectParamDoubleSample.INF.isSufficientFor(ConnectParamDoubleSample.ONE), "∞ >= 1 is false!");
			assertTrue(ConnectParamDoubleSample.INF.isSufficientFor(ConnectParamDoubleSample.INF), "∞ >= ∞ is false!");
			//+
			assertEquals(0, ConnectParamDoubleSample.ZERO.add(ConnectParamDoubleSample.ZERO).val, "0+0≠0");
			assertEquals(1, ConnectParamDoubleSample.ZERO.add(ConnectParamDoubleSample.ONE).val, "0+1≠1");
			assertEquals(1, ConnectParamDoubleSample.ONE.add(ConnectParamDoubleSample.ZERO).val, "1+0≠1");
			assertEquals(2, ConnectParamDoubleSample.ONE.add(ConnectParamDoubleSample.ONE).val, "1+1≠2");

			assertEquals(Double.POSITIVE_INFINITY, ConnectParamDoubleSample.INF.add(ConnectParamDoubleSample.ZERO).val, "∞+0≠∞");
			assertEquals(Double.POSITIVE_INFINITY, ConnectParamDoubleSample.INF.add(ConnectParamDoubleSample.ONE).val, "∞+1≠∞");
			assertEquals(Double.POSITIVE_INFINITY, ConnectParamDoubleSample.INF.add(ConnectParamDoubleSample.INF).val, "∞+∞≠∞");
			//-
			assertEquals(0, ConnectParamDoubleSample.ZERO.subtract(ConnectParamDoubleSample.ZERO).val, "0-0≠0");
			assertEquals(1, ConnectParamDoubleSample.ONE.subtract(ConnectParamDoubleSample.ZERO).val, "1-0≠1");
			assertEquals(0, ConnectParamDoubleSample.ONE.subtract(ConnectParamDoubleSample.ONE).val, "1-1≠0");
			assertThrows(IllegalArgumentException.class, () -> ConnectParamDoubleSample.ZERO.subtract(ConnectParamDoubleSample.ONE), "0-1 is valid");
			//⋃
			assertEquals(0, ConnectParamDoubleSample.ZERO.union(ConnectParamDoubleSample.ZERO).val, "0⋃0≠0");
			assertEquals(1, ConnectParamDoubleSample.ZERO.union(ConnectParamDoubleSample.ONE).val, "0⋃1≠1");
			assertEquals(Double.POSITIVE_INFINITY, ConnectParamDoubleSample.ZERO.union(ConnectParamDoubleSample.INF).val, "0⋃∞≠∞");
			assertEquals(Double.POSITIVE_INFINITY, ConnectParamDoubleSample.ONE.union(ConnectParamDoubleSample.INF).val, "1⋃∞≠∞");
			assertEquals(1, ConnectParamDoubleSample.ONE.union(ConnectParamDoubleSample.ONE).val, "1⋃1≠1");
			assertEquals(15, new ConnectParamDoubleSample(10).union(new ConnectParamDoubleSample(15)).val, "10⋃15≠15");
			//⋂
			assertEquals(0, ConnectParamDoubleSample.ZERO.intersection(ConnectParamDoubleSample.ZERO).val, "0⋂0≠0");
			assertEquals(0, ConnectParamDoubleSample.ZERO.intersection(ConnectParamDoubleSample.ONE).val, "0⋂1≠0");
			assertEquals(0, ConnectParamDoubleSample.ZERO.intersection(ConnectParamDoubleSample.INF).val, "0⋂∞≠0");
			assertEquals(1, ConnectParamDoubleSample.ONE.intersection(ConnectParamDoubleSample.INF).val, "1⋂∞≠1");
			assertEquals(1, ConnectParamDoubleSample.ONE.intersection(ConnectParamDoubleSample.ONE).val, "1⋂1≠1");
			assertEquals(10, new ConnectParamDoubleSample(10).intersection(new ConnectParamDoubleSample(15)).val, "10⋂15≠10");
			//∗
			assertEquals(0, ConnectParamDoubleSample.ZERO.amplify(0).val, "0*0≠0");
			assertEquals(0, ConnectParamDoubleSample.ZERO.amplify(10).val, "0*10≠0");
			assertEquals(1, ConnectParamDoubleSample.ONE.amplify(1).val, "1*1≠1");
			assertEquals(10, ConnectParamDoubleSample.ONE.amplify(10).val, "1*10≠10");
			assertEquals(Double.POSITIVE_INFINITY, ConnectParamDoubleSample.INF.amplify(1).val, "∞*1≠∞");
			assertEquals(Double.POSITIVE_INFINITY, ConnectParamDoubleSample.INF.amplify(Double.POSITIVE_INFINITY).val, "∞*∞≠∞");
		}

		private static final int COUNTMIN = 10, COUNTMAX = 64000, COUNTRANGE = COUNTMAX-COUNTMIN;
		private static final double VALMIN = 1E-3, VALMAX = 1E9, VALRANGE = VALMAX-VALMIN;
		private Collection<Double> genVals(Random rng){
			return Stream.generate(() -> VALMIN + rng.nextDouble()*VALRANGE).limit(COUNTMIN + rng.nextInt(COUNTRANGE)).collect(Collectors.toList());
		}
		@Test
		public void testDefaults(){
			//⊂
			assertTrue(ConnectParamDoubleSample.ZERO.canBeIn(ConnectParamDoubleSample.ZERO), "0 <= 0 is false!");
			assertTrue(ConnectParamDoubleSample.ZERO.canBeIn(ConnectParamDoubleSample.ONE), "0 <= 1 is false!");
			assertTrue(ConnectParamDoubleSample.ONE.canBeIn(ConnectParamDoubleSample.ONE), "1 <= 1 is false!");
			assertTrue(ConnectParamDoubleSample.ZERO.canBeIn(ConnectParamDoubleSample.INF), "0 <= ∞ is false!");
			assertTrue(ConnectParamDoubleSample.ONE.canBeIn(ConnectParamDoubleSample.INF), "1 <= ∞ is false!");
			assertTrue(ConnectParamDoubleSample.INF.canBeIn(ConnectParamDoubleSample.INF), "∞ <= ∞ is false!");

			//streams
			var random = new Random();
			for(int i = 0; i < 50; i++){
				var n = genVals(random);
				Supplier<DoubleStream> ns = () -> n.stream().mapToDouble(Double::doubleValue);
				Supplier<Stream<ConnectParamDoubleSample>> cps = () -> ns.get().mapToObj(ConnectParamDoubleSample::new);
				final double sum = ns.get().sum(), max = ns.get().max().orElseThrow(), min = ns.get().min().orElseThrow();
				//+
				assertEquals(sum, ConnectParam.sum(layer, cps.get()).val, Math.pow(1, Math.log10(sum)-5),"Streamed sum failed");
				//⋃
				assertEquals(max, ConnectParam.union(layer, cps.get()).val, "Streamed union failed");
				//⋂
				assertEquals(min, ConnectParam.intersection(layer, cps.get()).val, "Streamed intersect failed");
			}
		}

	}

}
