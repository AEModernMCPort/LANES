package lanes.util.ltask;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.function.Consumer;

/**
 * Contextualized concurrent task
 * @param <T> self
 */
public interface LTask<T extends LTask<T>> extends Runnable {

	/**
	 * @return the supertype of this task
	 */
	@NonNull Supertype<T> getSupertype();

	/**
	 * Retrieves the context this task is executed in.<br>
	 * @return context of this task
	 */
	@NonNull LTaskContext getContext();

	/**
	 * Initialize context for this task, usually from {@link LTaskContext#submit(LTask)}.<br>
	 * Errors if the context is already initialized, unless the <code>context</code> passed is the one already assigned (in which case calling this has no effect).
	 * @param context context for this task
	 * @throws UnsupportedOperationException if the task already has a [different] context
	 */
	void setContext(@NonNull LTaskContext context);

	/**
	 * See {@link Runnable#run()}, with support for automatic resume of operations after a {@linkplain InterruptReason#SUSPEND suspension}.
	 */
	@Override
	void run();

	/**
	 * Checks whether this task was completed [successfully].<br>
	 * Must not be called while the task is running.
	 * @return whether the task has completed
	 */
	boolean hasCompleted();

	/**
	 * Interrupts this task
	 * @param reason reason for interrupt
	 * @return a consumer ready to consume the reason for the interrupt, and act accordingly, once the interrupt is over
	 */
	default @NonNull Consumer<InterruptReason> interrupt(@NonNull InterruptReason reason){
		return getContext().interruptTask((T) this);
	}

	/**
	 * Supertype of a task can/is used for identification & registration, classification, and serialization/deserialization.
	 * @param <T> task type
	 */
	interface Supertype<T extends LTask<T>> {

		//TODO: supertype is only (mostly) used for ser/deser

	}

}
