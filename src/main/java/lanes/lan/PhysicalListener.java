package lanes.lan;

import lanes.LListener;
import lanes.lan.mesh.Meshable;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * This interface is implemented by the engine (usually by the preprocessor(s)), and should be invoked by the implementation whenever physical changes occur.
 *
 * @param <M> the implementation-specific common meshable type
 */
public interface PhysicalListener<M extends Meshable> extends LListener {

	void onCreated(@NonNull M m);
	void onDestroyed(@NonNull M m);

	void onLoaded(@NonNull M m);
	void onUnloaded(@NonNull M m);

}
