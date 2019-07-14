package lanes.lan.mesh;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * The baseline interface for all physical things that should be pre-processed.<br>
 * It is extended by specific meshable things, and by implementation-specific grouping interface for parametrization; meaning that the implementations of these meshable things implement both the thing-interface and the grouping-interface. This is mainly to ensure that meshables from different implementations can't "accidentally" get mixed (unless explicitly allowed to).
 */
public interface Meshable {

	boolean existsIn(@NonNull String layer);

}
