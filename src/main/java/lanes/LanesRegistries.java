package lanes;

import lanes.lan.Medium;
import lanes.util.reg.MutableNamedRegistry;
import lanes.util.reg.NamedRegistry;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.function.Consumer;

/**
 * All registries used by {@linkplain LANES}.<br>
 * In the built state, the registries are guaranteed to be deeply immutable.
 */
public class LanesRegistries {

	protected final NamedRegistry<?> layersRegistry;
	protected final NamedRegistry<Medium> mediumsRegistry;

	protected LanesRegistries(@NonNull NamedRegistry<?> layersRegistry, @NonNull NamedRegistry<Medium> mediumsRegistry){
		this.layersRegistry = layersRegistry;
		this.mediumsRegistry = mediumsRegistry;
	}

	@NonNull
	public <CP extends ConnectParam<CP>, L extends Layer<CP, L>> NamedRegistry<L> getLayersRegistry(){
		return (NamedRegistry<L>) layersRegistry;
	}

	@NonNull
	public NamedRegistry<Medium> getMediumsRegistry(){
		return mediumsRegistry;
	}

	public static class Builder {

		protected final MutableNamedRegistry<?> layersRegistry;
		protected final MutableNamedRegistry<Medium> mediumsRegistry;

		public Builder(@NonNull MutableNamedRegistry<?> layersRegistry, @NonNull MutableNamedRegistry<Medium> mediumsRegistry){
			this.layersRegistry = layersRegistry;
			this.mediumsRegistry = mediumsRegistry;
		}

		public Builder(){
			this(new MutableNamedRegistry<>(), new MutableNamedRegistry<>());
		}

		@NonNull
		public <CP extends ConnectParam<CP>, L extends Layer<CP, L>> MutableNamedRegistry<L> getLayersRegistry(){
			return (MutableNamedRegistry<L>) layersRegistry;
		}
		@NonNull
		public <CP extends ConnectParam<CP>, L extends Layer<CP, L>> Builder buildLayersRegistry(@NonNull Consumer<MutableNamedRegistry<L>> builder){
			builder.accept(getLayersRegistry());
			return this;
		}

		@NonNull
		public MutableNamedRegistry<Medium> getMediumsRegistry(){
			return mediumsRegistry;
		}
		@NonNull
		public Builder buildMediumsRegistry(@NonNull Consumer<MutableNamedRegistry<Medium>> builder){
			builder.accept(mediumsRegistry);
			return this;
		}

		@NonNull
		public LanesRegistries build(){
			return new LanesRegistries(layersRegistry.finaliz(), mediumsRegistry.finaliz());
		}

	}

}
