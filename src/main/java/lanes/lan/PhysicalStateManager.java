package lanes.lan;

import lanes.lan.mesh.Meshable;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.stream.Stream;

/**
 * Base interface for exposing physical state to LANES internals.
 * @param <M> base {@linkplain Meshable meshable} [grouping] type
 */
public interface PhysicalStateManager<M extends Meshable> {

	@NonNull
	Stream<M> getAdjacent(@NonNull M meshable);

}
