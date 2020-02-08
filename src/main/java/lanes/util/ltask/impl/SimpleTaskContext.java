package lanes.util.ltask.impl;

import lanes.util.ltask.InterruptReason;
import lanes.util.ltask.LTask;
import lanes.util.ltask.LTaskContext;
import lanes.util.ltask.LTaskExecutionService;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class SimpleTaskContext implements LTaskContext {

	protected final LTaskExecutionService executionService;

	protected Set<LTask> tasks = ConcurrentHashMap.newKeySet();
	protected Set<LTask> runningTasks = ConcurrentHashMap.newKeySet();

	protected ContextState state = ContextState.FREE;
	protected final ReentrantLock stateLock = new ReentrantLock();

	protected Queue<LTask> submittedDuringInterrupt = new LinkedList<>();

	protected Queue<CountDownLatch> multiInterrupts = new LinkedList<>();

	protected Collection<LTask> suspended;
	protected CountDownLatch suspendingRunningLatch;

	public SimpleTaskContext(LTaskExecutionService executionService){
		this.executionService = executionService;
	}

	@Override
	public <T extends LTask<T>> void submit(@NonNull T task){
		stateLock.lock();
		try {
			switch(state){
				// Illegal states
				case TERMINATED:
				case SUSPENDED:
					throw new IllegalStateException();
				// We can handle that - before all running tasks have suspended, we can push submitted tasks directly into suspended; the tasks must support ser ops even before having started running anyway :P
				case SUSPENDING:
					suspended.add(task);
					return;
				// Just throw the task into the interrupt bin
				case INTERRUPTED:
					submittedDuringInterrupt.add(task); //TODO can this be outside of lock?
					return;
			}
		} finally {
			stateLock.unlock();
		}
		//We are not interrupted. woohoo!... hopefully...
		// i still have this doubt we may get there, then trigger the lock for interrupt and then submit the task
		// though if that happens, we will block on submit until the task makes it through. But when it does, it will be in tasks and it will be running . . .
		//TODO most of the time submissions are outside of interrupt -> locking not needed -> check this for safety.
		//So technically, if i move this submit to exe inside the lock, i will avoid that crappy case
		LTaskContext.super.submit(task);
		tasks.add(task);
	}

	@Override
	public @NonNull Consumer<InterruptReason> interrupt(){
		stateLock.lock();
		CountDownLatch multintr = null;
		try {
			switch(state){
				case TERMINATED:
				case SUSPENDED:
				case SUSPENDING:
					throw new IllegalStateException();
				case INTERRUPTED:
					multiInterrupts.add(multintr = new CountDownLatch(1));
					break;
				case FREE:
					state = ContextState.INTERRUPTED;
					break;
			}
		} finally {
			stateLock.unlock();
		}
		while(multintr != null) try {
			multintr.await();
			multintr = null;
			stateLock.lock();
			try {
				switch(state){
					case TERMINATED:
					case SUSPENDED:
					case SUSPENDING:
						throw new IllegalStateException();
				}
				state = ContextState.INTERRUPTED;
			} finally {
				stateLock.unlock();
			}
		} catch(InterruptedException e){}
		var sc = executionService.interruptTasks(tasks);
		return reason -> {
			stateLock.lock();
			Queue<LTask> resubmit = null;
			try {
				switch(reason){
					//Resume like nothing happened if it was just a pause
					case PAUSE:
						state = ContextState.FREE;
						//In case we get interrupted right after leaving a pause, we swap the interrupt submissions with a fresh list, and use the old one for resubmissions
						resubmit = submittedDuringInterrupt;
						submittedDuringInterrupt = new LinkedList<>();
						break;
					case SUSPEND:
						state = ContextState.SUSPENDING;
						suspended = new HashSet<>(tasks);
						if(!submittedDuringInterrupt.isEmpty()){
							suspended.addAll(submittedDuringInterrupt);
							submittedDuringInterrupt = new LinkedList<>();
						}
						suspendingRunningLatch = new CountDownLatch(runningTasks.size());
						break;
					case TERMINATE:
						state = ContextState.TERMINATED;
						break;
				}
			} finally {
				stateLock.unlock();
			}

			sc.accept(reason);
			//It was just a pause - simply resubmit all tasks submitted during interrupt
			if(resubmit != null) resubmit.forEach(this::submit);
			//Multi-interrupt. Yay!
			var milatch = multiInterrupts.poll();
			if(milatch != null) milatch.countDown();
		};
	}

	@Override
	public void awaitSuspension() throws InterruptedException {
		stateLock.lock();
		try {
			switch(state){
				case TERMINATED: throw new IllegalStateException();
				case FREE:
				case INTERRUPTED:
				case SUSPENDED: return;
			}
		} finally {
			stateLock.unlock();
		}
		suspendingRunningLatch.await();
		stateLock.lock();
		try {
			state = ContextState.SUSPENDED;
			suspendingRunningLatch = null; //If we're here, we have suspended and don't need the latch anymore
		} finally {
			stateLock.unlock();
		}
	}

	@Override
	public void resume(){
		boolean fullySuspended = false;
		while(!fullySuspended) try {
			awaitSuspension();
			fullySuspended = true;
		} catch(InterruptedException e){}
		Collection<LTask> suspendeds;
		stateLock.lock();
		try {
			suspendeds = suspended; //Perform a swap, you should know why at this point
			suspended = null;
			state = ContextState.FREE;
		} finally {
			stateLock.unlock();
		}
		if(suspendeds != null) suspendeds.forEach(this::submit); //suspendeds is null when invoked when running or interrupted
	}

	@Override
	public @NonNull LTaskExecutionService executionService(){
		return executionService;
	}

	@Override
	public <T extends LTask<T>> void taskStartedExecution(@NonNull T task){
		//TODO So, what the heck happens if we have been put into suspension when the task was blocking in beforeExecution? Supposedly now it will proceed with execution, will be added to the runningTasks and will NOT be accounted by the semaphore. Ugh >|
		runningTasks.add(task);
		LTaskContext.super.taskStartedExecution(task);
	}

	@Override
	public <T extends LTask<T>> void taskEndedExecution(@NonNull T task){
		stateLock.lock();
		try {
			if(state == ContextState.SUSPENDING){
				//If the task completed, no need to keep it
				if(task.hasCompleted()) suspended.remove(task);
				suspendingRunningLatch.countDown();
			}
		} finally {
			stateLock.unlock();
		}
		LTaskContext.super.taskEndedExecution(task);
		runningTasks.remove(task);
		tasks.remove(task);
	}

	public enum ContextState {

		FREE, INTERRUPTED, SUSPENDING, SUSPENDED, TERMINATED;

	}

}
