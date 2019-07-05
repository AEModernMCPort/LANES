package lanes.lan;

import lanes.util.reg.NamedRegistry;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.OptionalDouble;

public interface Medium extends NamedRegistry.Entry<Medium> {

	/**
	 * Computes the decay (amplification) through this medium on given layer, if said layer can exist in this medium ({@linkplain OptionalDouble#empty() nothing} otherwise).
	 * @param layer layer
	 * @param dist distance
	 * @return decay over the distance in the layer, or {@linkplain OptionalDouble#empty()} if the layer can't exist in this medium
	 */
	@NonNull
	OptionalDouble decay(@NonNull String layer, @NonNegative double dist);

	default boolean existsInLayer(@NonNull String layer){
		return decay(layer, 0).isPresent();
	}

}
