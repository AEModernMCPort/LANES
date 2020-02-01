package lanes;

import lanes.lan.PhysicalListener;
import lanes.lan.mesh.Meshable;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static lanes.util.MiscStaticUtils.*;

/**
 * Priority-ordered main event listeners for a {@linkplain LANES.Instance LANES instance}.<br>
 * Built state is immutable - no listeners can be added here during runtime (but nothing stops you from creating a dynamic re-dispatcher and registering it as a listener).<br>
 * <br>
 * Note: Technically, this is no more than a group-delegating listener, therefore it can be used in other cases than as a main listener in a LANES instance.<br>
 * For example, using this as a same-priority subgroup (grouping multiple listeners whose relative priority matters, and/or global priority is same or does not matter) and registering it in a parent listener group. There is no limit to how deep can you go, besides JVM stack limit. Also, <b>please make sure not to recurse</b> if you dynamically listen to the listeners while listening to the dynamically listened listeners.
 * @param <M> base {@linkplain Meshable meshable} [grouping] type
 */
public class LanesListeners<M extends Meshable> implements PhysicalListener<M> {

	protected final List<LListener> listeners;

	protected LanesListeners(@NonNull List<LListener> listeners){
		this.listeners = List.copyOf(listeners);
	}

	@NonNull
	protected <L extends LListener> Stream<L> listenersOfType(@NonNull Class<? super L> t){
		return StreamFilterCast(listeners.stream(), t);
	}

	@Override
	public void onCreated(@NonNull M m){ this.<PhysicalListener<M>>listenersOfType(PhysicalListener.class).forEach(l -> l.onCreated(m)); }

	@Override
	public void onDestroyed(@NonNull M m){ this.<PhysicalListener<M>>listenersOfType(PhysicalListener.class).forEach(l -> l.onDestroyed(m)); }

	@Override
	public void onLoaded(@NonNull M m){ this.<PhysicalListener<M>>listenersOfType(PhysicalListener.class).forEach(l -> l.onLoaded(m)); }

	@Override
	public void onUnloaded(@NonNull M m){ this.<PhysicalListener<M>>listenersOfType(PhysicalListener.class).forEach(l -> l.onUnloaded(m)); }

	public static class Builder {

		protected final Map<LListener, Double> listeners = new IdentityHashMap<>();

		public Builder(){}

		/**
		 * Adds a listener with a priority.<br>
		 * Priority sorting in decreasing order - <b>higher priorities called first</b>, no specific order for equal priorities.
		 * @param listener a listener
		 * @param priority priority (relative to other listeners of this builder), <b>bigger ⇔ more important ⇔ called earlier</b>
		 */
		public void addListener(@NonNull LListener listener, double priority){
			listeners.put(listener, priority);
		}

		/**
		 * Adds a listener with default priority
		 * @param listener a listener
		 */
		public void addListener(@NonNull LListener listener){
			addListener(listener, 0);
		}

		@NonNull
		public <M extends Meshable> LanesListeners<M> build(){
			return new LanesListeners<>(listeners.entrySet().stream().sorted(Comparator.comparingDouble(e -> -e.getValue())).map(Map.Entry::getKey).collect(Collectors.toList()));
		}

	}

}
