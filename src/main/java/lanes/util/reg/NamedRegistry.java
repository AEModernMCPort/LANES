package lanes.util.reg;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;
import java.util.stream.Stream;

public interface NamedRegistry<T extends NamedRegistry.Entry<T>> {

	default boolean has(@NonNull String name){ return get(name).isPresent(); }

	@NonNull
	Optional<T> get(@NonNull String name);

	@NonNull
	Stream<T> entries();

	interface Entry<T extends Entry<T>> {

		@NonNull
		String getName();

	}

}
