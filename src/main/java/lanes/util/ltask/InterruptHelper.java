package lanes.util.ltask;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;

/**
 * Interruption helper for {@linkplain LTask tasks}.
 */
public interface InterruptHelper {

	/**
	 * Checks whether the execution of current task has been interrupted
	 * @return whether the task has been interrupted
	 */
	static boolean isInterrupted(){
		return Thread.currentThread().isInterrupted();
	}

	/**
	 * Check to check for interrupt & block until resumed all-in one!<br>
	 * The task should preferably call this directly, instead of checking {@link InterruptHelper#isInterrupted()} first.<br>
	 * <br>
	 * If the task has <i>not</i> been interrupted:<br>
	 * - returns immediately with {@linkplain Optional#empty() nothing}.<br>
	 * If the task has been interrupted:<br>
	 * - Blocks [executing thread] until convenient time<br>
	 * - Returns the reason for the interrupt<br>
	 * <br>
	 * Once the method [unblocks &] returns, and based on the reason of the interrupt, the task <i>must</i> do the following:<br>
	 * <ul>
	 *     <li>- {@link InterruptReason#PAUSE} - resume execution normally, <b>now</b>.</li>
	 *     <li>- {@link InterruptReason#SUSPEND} - prepare the task for ser/deser, and stop execution. The task will be resumed later with a new {@linkplain LTask#run() run} call.<br>
	 *         <b>Unless</b> the task {@linkplain LTask#hasCompleted() completes successfully}, in which case it need not be resumed some time later.</li>
	 *     <li>- {@link InterruptReason#TERMINATE} - clean up used resources, if needed, and stop execution. The task will not be resumed, and whatever it was supposed to do does not matter anymore, sorry ;C .</li>
	 * </ul>
	 *
	 * @return reason for the interrupt, or {@linkplain Optional#empty() none} if the task was not interrupted
	 * @throws IllegalArgumentException basically when called outside {@linkplain LTask#run() task.run} (and/or <code>task</code> argument does not match calling task)
	 */
	static <T extends LTask<T>> @NonNull Optional<InterruptReason> interruptSuspendRequestReason(@NonNull T task){
		var interrupt = Thread.interrupted();
		if(!interrupt) return Optional.empty();
		else return Optional.of(task.getContext().executionService().interruptedTaskRequestReason(task, Thread.currentThread()));
	}

}
