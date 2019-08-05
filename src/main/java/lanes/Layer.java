package lanes;

import lanes.lan.mesh.Meshable;
import lanes.lan.mesh.PhysicalMesher;
import lanes.util.reg.NamedRegistry;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.function.Function;

public abstract class Layer<CP extends ConnectParam<CP>, L extends Layer<CP, L>> implements NamedRegistry.Entry<L> {

	public final String name;

	public Layer(@NonNull String name){
		this.name = name;
	}

	@NonNull
	@Override
	public String getName(){
		return name;
	}

	public <M extends Meshable> InstanceBuilder<M> newInstance(){ return new InstanceBuilder<>(); }
	public class Instance<M extends Meshable> {

		private PhysicalMesher<CP, L, M> physicalMesher;

		protected Instance(){}

		@NonNull
		public L getLayer(){
			return (L) Layer.this;
		}

		@NonNull
		public PhysicalMesher<CP, L, M> getPhysicalMesher(){
			return physicalMesher;
		}

	}
	public class InstanceBuilder<M extends Meshable> {

		protected Function<Instance<M>, PhysicalMesher<CP, L, M>> physicalMesher;

		protected InstanceBuilder(){}

		@NonNull
		public InstanceBuilder<M> setPhysicalMesher(@NonNull Function<Instance<M>, PhysicalMesher<CP, L, M>> physicalMesher){
			this.physicalMesher = physicalMesher;
			return this;
		}

		@NonNull
		public Instance<M> build(){
			var instance = new Instance<M>();
			instance.physicalMesher = physicalMesher.apply(instance);
			return instance;
		}

	}

	//TODO Access to (0,1) constants, in a more proper-"static" way
	@Deprecated public abstract CP zeroCP(); //0: ∀cp, 0⋂cp=0 & 0⋃cp=cp
	//@Deprecated public abstract CP oneCP(); //1:
	@Deprecated public abstract CP infCP(); //∞: ∀cp, cp⋂∞=cp & cp⋃∞=∞

}
