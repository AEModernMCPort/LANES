package lanes.util.ltask.impl;

import lanes.util.ltask.InterruptReason;
import lanes.util.ltask.LTask;
import lanes.util.ltask.LTaskExecutionService;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class LTaskExeSOnThreadPool implements LTaskExecutionService {

	protected final ControlledThreadPool threadPool;
	protected final BlockingQueue<Runnable> taskPool;
	protected final ConcurrentHashMap<Runnable, Thread> runningTasks = new ConcurrentHashMap<>();
	protected final ConcurrentHashMap<Runnable, RunningTasksInterruptSemaphore> interruptSemaphores = new ConcurrentHashMap<>();

	public LTaskExeSOnThreadPool(@NonNegative int corePoolsize, @NonNegative int maxPoolSize){
		this.threadPool = new ControlledThreadPool(corePoolsize, maxPoolSize, 60L, TimeUnit.SECONDS, this.taskPool = new LinkedBlockingQueue<>());
	}

	@Override
	public <T extends LTask<T>> void submit(@NonNull T task){
		threadPool.execute(task);
	}

	@Override
	public @NonNull Consumer<InterruptReason> interruptTasks(@NonNull Collection<LTask> tasks){
		threadPool.pauseStartExe();
		RunningTasksInterruptSemaphore semaphore = null;
		Collection<Runnable> queuedRemoved = new LinkedList<>();
		for(var task : tasks){
			var thread = runningTasks.get(task);
			if(thread != null){
				if(semaphore == null) semaphore = new RunningTasksInterruptSemaphore();
				interruptSemaphores.put(task, semaphore);
				thread.interrupt();
			}
			else if(taskPool.remove(task)) queuedRemoved.add(task);
		}
		threadPool.resumeStartExe();
		final var resumeInterrupted = Optional.ofNullable(semaphore);
		return reason -> {
			resumeInterrupted.ifPresent(s -> s.resumeInterrupted(reason));
			if(reason == InterruptReason.PAUSE) queuedRemoved.forEach(threadPool::execute); //Resubmit not yet started tasks only if it was a pause. Tasks must support suspension unstarted, and we don't care about terminated tasks anyway.
		};
	}

	@Override
	public @NonNull <T extends LTask<T>> InterruptReason interruptedTaskRequestReason(@NonNull T task, @NonNull Thread taskThread){
		if(runningTasks.get(task) != taskThread) throw new IllegalArgumentException();
		return interruptSemaphores.remove(task).interruptedTaskRequestReason();
	}

	protected <T extends LTask<T>> void taskStarted(@NonNull Runnable run, @NonNull Thread t){
		runningTasks.put(run, t);
		if(run instanceof LTask) ((LTask) run).getContext().taskStartedExecution((LTask) run);
	}

	protected void taskEnded(@NonNull Runnable run){
		runningTasks.remove(run);
		if(run instanceof LTask) ((LTask) run).getContext().taskEndedExecution((LTask) run);
	}

	public class ControlledThreadPool extends ThreadPoolExecutor {

		private final PauseWaitingLock pauseLock = new PauseWaitingLock();

		protected ControlledThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue){
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
		}

		protected ControlledThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory){
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
		}

		protected void pauseStartExe(){
			pauseLock.pause();
		}

		protected void resumeStartExe(){
			pauseLock.resume();
		}

		@Override
		protected void beforeExecute(Thread t, Runnable r){
			pauseLock.awaitIfPaused(t);
			taskStarted(r, t);
			super.beforeExecute(t, r);
		}

		@Override
		protected void afterExecute(Runnable r, Throwable t){
			taskEnded(r);
			super.afterExecute(r, t);
		}

	}

	public static class RunningTasksInterruptSemaphore {

		protected final Semaphore semaphore = new Semaphore(0);

		protected InterruptReason reason;

		public void resumeInterrupted(@NonNull InterruptReason reason){
			this.reason = reason;
			semaphore.drainPermits();
		}

		public @NonNull InterruptReason interruptedTaskRequestReason(){
			semaphore.acquireUninterruptibly();
			return reason;
		}

	}

}
