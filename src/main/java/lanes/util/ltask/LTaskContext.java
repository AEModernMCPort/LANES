package lanes.util.ltask;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collections;
import java.util.function.Consumer;

//TODO Write a chunky paragraph on context and its' states, because oh boy aren't they just fun
// Just remember that transition susp..ing➡ed is not automatic, and multi-interrupt blocks, and sometimes it catches thread interrupt and sometimes it doesn't, and  basically everything
public interface LTaskContext {

	/**
	 * Execution service in which this context executes.
	 * @return execution service for this context
	 */
	@NonNull LTaskExecutionService executionService();

	/**
	 * Submits the task for execution.
	 * @param task task to execute
	 * @param <T> task type
	 * @throws IllegalStateException if the execution service is terminated or suspended
	 */
	default <T extends LTask<T>> void submit(@NonNull T task){
		task.setContext(this);
		executionService().submit(task);
	}

	/**
	 * Interrupts this context.<br>
	 * Blocking, returns once all tasks have been interrupted.<br>
	 * If the context is in an interrupt by a different thread(s), blocks until the other interrupt(s) has(ve) resolved.
	 * @return a consumer ready to consume the reason for the interrupt, and act accordingly, once the interrupt is over
	 * @throws IllegalStateException if the context was terminated, suspended, or is being suspended.
	 */
	@NonNull Consumer<InterruptReason> interrupt();

	/**
	 * After {@linkplain InterruptReason#SUSPEND suspension} interrupt, awaits until all tasks have fully suspended.
	 * Calling this on a running, as well as interrupted context has no effect.<br>
	 * @throws IllegalStateException if the context was terminated
	 */
	void awaitSuspension() throws InterruptedException;

	/**
	 * Resume this context after {@linkplain InterruptReason#SUSPEND suspension}.<br>
	 * Calling this on a running, as well as interrupted context has no effect.<br>
	 * Calling this on a context that is in the process of suspension will {@linkplain LTaskContext#awaitSuspension() block until it has suspended}, then resume normally.
	 * @throws IllegalStateException if the context was terminated
	 */
	void resume();

	/**
	 * Interrupts the task in this context
	 * @param task task to interrupt
	 * @param <T> task type
	 * @return a consumer ready to consume the reason for the interrupt, and act accordingly, once the interrupt is over
	 */
	default <T extends LTask<T>> @NonNull Consumer<InterruptReason> interruptTask(@NonNull T task){
		return executionService().interruptTasks(Collections.singletonList(task));
	}

	default <T extends LTask<T>> void taskStartedExecution(@NonNull T task){}
	default <T extends LTask<T>> void taskEndedExecution(@NonNull T task){}

}
