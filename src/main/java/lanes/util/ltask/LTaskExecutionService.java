package lanes.util.ltask;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.function.Consumer;

public interface LTaskExecutionService {

	/**
	 * Submits the task for execution.<br>
	 * Must only be called {@linkplain LTaskContext from the context}.
	 * @param task task to execute
	 * @param <T> task type
	 */
	<T extends LTask<T>> void submit(@NonNull T task);

	/**
	 * Requests the reason for task interruption, blocking accordingly.
	 * @param task interrupted task
	 * @param taskThread the thread of the task
	 * @param <T> task type
	 * @return reason for interrupt
	 * @throws IllegalArgumentException if the task was not suspended, or on a different thread, or it's not running on this service
	 */
	<T extends LTask<T>> @NonNull InterruptReason interruptedTaskRequestReason(@NonNull T task, @NonNull Thread taskThread);

	@NonNull Consumer<InterruptReason> interruptTasks(@NonNull Collection<LTask> tasks);

}
