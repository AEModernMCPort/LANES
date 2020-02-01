package lanes;

import lanes.lan.PhysicalStateManager;
import lanes.lan.mesh.Meshable;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Fully configured LANES.<br>
 * Is in a way a configuration store for constructing {@linkplain Instance instances}, which actually hold active components, on request.<br>
 * The main purpose of such structure is to allow easy creation of multiple separate LANES (or as it is - instances) with the same configuration, without configuring every one of them every time.
 * @param <M> base {@linkplain Meshable meshable} [grouping] type
 */
public class LANES<M extends Meshable> {

	public final LanesConfiguration config;
	public final LanesRegistries registries;

	protected LANES(@NonNull LanesConfiguration config, @NonNull LanesRegistries registries){
		this.config = config;
		this.registries = registries;
	}

	/**
	 * Active instance of {@linkplain LANES} - holds the single group of active and interlinked components.
	 */
	public class Instance {

		public final LanesListeners<M> listeners;
		public final PhysicalStateManager<M> physicalStateManager;

		protected Instance(@NonNull LanesListeners<M> listeners, @NonNull PhysicalStateManager<M> physicalStateManager){
			this.listeners = listeners;
			this.physicalStateManager = physicalStateManager;
		}

	}

	@NonNull
	public InstanceBuilder newInstance(){ return new InstanceBuilder(); }
	public class InstanceBuilder {

		public final LanesListeners.Builder listeners = new LanesListeners.Builder();
		public PhysicalStateManager<M> physicalStateManager;

		protected InstanceBuilder(){}

		@NonNull
		public InstanceBuilder buildListeners(@NonNull Consumer<LanesListeners.Builder> build){
			build.accept(listeners);
			return this;
		}

		@NonNull
		public InstanceBuilder setPhysicalStateManager(@NonNull PhysicalStateManager<M> physicalStateManager){
			this.physicalStateManager = physicalStateManager;
			return this;
		}

		@NonNull
		public Instance build(){
			return new Instance(listeners.build(), Objects.requireNonNull(physicalStateManager));
		}

	}

	public static class Builder {

		public final LanesConfiguration config;
		public final LanesRegistries.Builder registries = new LanesRegistries.Builder();

		public Builder(@NonNull LanesConfiguration config){
			this.config = config;
		}

		@NonNull
		public Builder buildRegistries(@NonNull Consumer<LanesRegistries.Builder> build){
			build.accept(registries);
			return this;
		}

		@NonNull
		public <M extends Meshable> LANES<M> build(){
			return new LANES<>(config, registries.build());
		}

	}

}
