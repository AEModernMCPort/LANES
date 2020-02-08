package lanes.util.ltask;

import lanes.util.ltask.impl.LTaskExeSOnThreadPool;
import lanes.util.ltask.impl.SimpleTaskContext;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class LTaskExecutionNoSerTest {

	private static void mainTSleepFor(long sleep){
		long slept = 0;
		while(slept < sleep + sleep/8) try{
			Thread.sleep(sleep/8);
			slept += sleep/8;
		} catch(InterruptedException e){}
	}

	@ParameterizedTest
	@ValueSource(ints = {2, 4, 8, 16})
	public void testJustParallelExecution(int pool){
		final long sleep = 250;
		var exes = new LTaskExeSOnThreadPool(pool, pool);
		var ctxt = new SimpleTaskContext(exes);
		AtomicBoolean[] ress = Stream.generate(AtomicBoolean::new).limit(pool).toArray(AtomicBoolean[]::new);
		IntStream.range(0, pool).forEach(i -> ctxt.submit(new ParamSleeperTask(() -> ress[i].set(true), sleep)));
		mainTSleepFor(sleep);
		assertTrue(Arrays.stream(ress).allMatch(AtomicBoolean::get), "Not all tasks executed! - " + Arrays.toString(ress));
		exes.shutdown();
	}

	@ParameterizedTest
	@ValueSource(ints = {2, 4, 8, 16})
	public void testInterruptPause(int pool){
		final long sleep1 = 250, sleepP = 100, sleep2 = 100;
		var exes = new LTaskExeSOnThreadPool(pool, pool);
		var ctxt = new SimpleTaskContext(exes);
		AtomicBoolean[] ress = Stream.generate(AtomicBoolean::new).limit(pool).toArray(AtomicBoolean[]::new);
		IntStream.range(0, pool).forEach(i -> ctxt.submit(new ParamSleeperTask(() -> ress[i].set(true), sleep1, sleep2)));
		mainTSleepFor(sleep1/2);
		var interruptR = ctxt.interrupt();
		mainTSleepFor(sleepP);
		assertTrue(Arrays.stream(ress).noneMatch(AtomicBoolean::get), "Some tasks finished - " + Arrays.toString(ress));
		interruptR.accept(InterruptReason.PAUSE);
		mainTSleepFor(sleep2);
		assertTrue(Arrays.stream(ress).allMatch(AtomicBoolean::get), "Not all tasks finished - " + Arrays.toString(ress));
		exes.shutdown();
	}

	@ParameterizedTest
	@ValueSource(ints = {2, 4, 8, 16})
	public void testInterruptTerminate(int pool){
		final long sleep1 = 250, sleep2 = 250;
		var exes = new LTaskExeSOnThreadPool(pool, pool);
		var ctxt = new SimpleTaskContext(exes);
		AtomicBoolean[] ressG1 = Stream.generate(AtomicBoolean::new).limit(pool/2).toArray(AtomicBoolean[]::new);
		AtomicBoolean[] ressG2 = Stream.generate(AtomicBoolean::new).limit(pool/2).toArray(AtomicBoolean[]::new);
		IntStream.range(0, pool/2).forEach(i -> ctxt.submit(new ParamSleeperTask(() -> ressG2[i].set(true), sleep1, sleep2)));
		IntStream.range(0, pool/2).forEach(i -> ctxt.submit(new ParamSleeperTask(() -> ressG1[i].set(true), sleep1)));
		mainTSleepFor(sleep1 + sleep2/4);
		ctxt.interrupt().accept(InterruptReason.TERMINATE);
		assertTrue(Arrays.stream(ressG1).allMatch(AtomicBoolean::get), "Some tasks of group 1 got terminated - " + Arrays.toString(ressG1));
		assertTrue(Arrays.stream(ressG2).noneMatch(AtomicBoolean::get), "Not all tasks of group 2 got terminated - " + Arrays.toString(ressG2));
		exes.shutdown();
	}

	@ParameterizedTest
	@ValueSource(ints = {2, 4, 8, 16})
	public void testInterruptSuspendAsPause(int pool){
		final long sleep1 = 250, sleep2 = 100;
		var exes = new LTaskExeSOnThreadPool(pool, pool);
		var ctxt = new SimpleTaskContext(exes);
		AtomicBoolean[] ress = Stream.generate(AtomicBoolean::new).limit(pool).toArray(AtomicBoolean[]::new);
		IntStream.range(0, pool).forEach(i -> ctxt.submit(new ParamSleeperTask(() -> ress[i].set(true), sleep1, sleep2)));
		mainTSleepFor(sleep1/4);
		ctxt.interrupt().accept(InterruptReason.SUSPEND);
		while(true) try {
			ctxt.awaitSuspension();
			break;
		} catch(InterruptedException e){}
		assertTrue(Arrays.stream(ress).noneMatch(AtomicBoolean::get), "Some tasks finished - " + Arrays.toString(ress));
		ctxt.resume();
		mainTSleepFor(sleep2);
		assertTrue(Arrays.stream(ress).allMatch(AtomicBoolean::get), "Not all tasks finished - " + Arrays.toString(ress));
		exes.shutdown();
	}

	protected static class ParamSleeperTask implements LTask<ParamSleeperTask> {

		private final Queue<Long> sleeps;
		private final Runnable finish;

		public ParamSleeperTask(Runnable finish, Collection<Long> sleeps){
			this.sleeps = new LinkedList<>(sleeps);
			this.finish = finish;
		}

		public ParamSleeperTask(Runnable finish, long... sleeps){
			this(finish, Arrays.stream(sleeps).boxed().collect(Collectors.toList()));
		}

		@Override
		public @NonNull Supertype<ParamSleeperTask> getSupertype(){
			return null;//Whatevs
		}

		private LTaskContext context;

		@Override
		public @NonNull LTaskContext getContext(){
			return context;
		}

		@Override
		public void setContext(@NonNull LTaskContext context){
			if(this.context != null && this.context != context) throw new UnsupportedOperationException();
			this.context = context;
		}

		@Override
		public void run(){
			while(sleeps.peek() != null){
				try {
					Thread.sleep(sleeps.poll());
				} catch(InterruptedException e){
					Thread.currentThread().interrupt(); //Re-set the interrupt status
				}
				var r = InterruptHelper.interruptSuspendRequestReason(this);
				if(r.isPresent()) switch(r.get()){
					case SUSPEND:
					case TERMINATE: return;
				}
			}
			if(finish != null) finish.run();
		}

		@Override
		public boolean hasCompleted(){
			return sleeps.isEmpty();
		}
	}

}
