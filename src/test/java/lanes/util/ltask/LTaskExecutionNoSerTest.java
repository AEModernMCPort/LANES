package lanes.util.ltask;

import lanes.util.ltask.impl.LTaskExeSOnThreadPool;
import lanes.util.ltask.impl.SimpleTaskContext;
import net.jodah.concurrentunit.Waiter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentest4j.AssertionFailedError;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeoutException;
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

	@ParameterizedTest
	@ValueSource(ints = {2, 5, 10})
	public void testMultiInterrupts(int interrupts){
		final long baseSleep = 100;
		final long[] sleeps = {baseSleep, baseSleep, baseSleep, baseSleep, baseSleep};
		final int pool = 8;
		var piExes = new LTaskExeSOnThreadPool(interrupts, interrupts);
		var piCtxt = new SimpleTaskContext(piExes);
		var exes = new LTaskExeSOnThreadPool(pool, pool);
		var ctxt = new SimpleTaskContext(exes);
		AtomicBoolean[] ress = Stream.generate(AtomicBoolean::new).limit(pool).toArray(AtomicBoolean[]::new);
		IntStream.range(0, pool).forEach(i -> ctxt.submit(new ParamSleeperTask(() -> ress[i].set(true), sleeps)));
		IntStream.range(0, interrupts).forEach(i -> piCtxt.submit(new ParamSleeperTask(() -> {
			var irr = ctxt.interrupt();
			try{
				Thread.sleep(150);
			} catch(InterruptedException e){}
			irr.accept(InterruptReason.PAUSE);
		}, (i+1)*50)));
		mainTSleepFor(50+interrupts/2*150);
		assertTrue(Arrays.stream(ress).noneMatch(AtomicBoolean::get), "Some tasks finished - " + Arrays.toString(ress));
		mainTSleepFor(interrupts/2*150 + baseSleep*sleeps.length);
		assertTrue(Arrays.stream(ress).allMatch(AtomicBoolean::get), "Not all tasks finished - " + Arrays.toString(ress));
		exes.shutdown();
		piExes.shutdown();
	}

	@ParameterizedTest
	@EnumSource(value = InterruptReason.class, names = {"SUSPEND", "TERMINATE"})
	public void testMultiInterruptsThrows(InterruptReason treason){
		final long baseSleep = 100;
		final int pool = 8;
		var piExes = new LTaskExeSOnThreadPool(2, 2);
		var piCtxt = new SimpleTaskContext(piExes);
		var exes = new LTaskExeSOnThreadPool(pool, pool);
		var ctxt = new SimpleTaskContext(exes);
		var waiter = new Waiter();
		IntStream.range(0, pool).forEach(i -> ctxt.submit(new ParamSleeperTask(null, baseSleep, baseSleep, baseSleep, baseSleep, baseSleep)));
		piCtxt.submit(new ParamSleeperTask(() -> {
			ctxt.interrupt().accept(InterruptReason.TERMINATE);
			waiter.resume();
		}, baseSleep/2));
		piCtxt.submit(new ParamSleeperTask(() -> {
			try {
				assertThrows(IllegalStateException.class, () -> ctxt.interrupt().accept(InterruptReason.PAUSE), "Interrupting terminated context didn't fail");
				waiter.resume();
			} catch(AssertionFailedError e){
				waiter.rethrow(e);
			}
		}, baseSleep));
		mainTSleepFor(baseSleep*8);
		try{
			waiter.await(2000, 2);
		} catch(TimeoutException | InterruptedException e){
			fail("Something went wrong when awaiting", e);
		}
		exes.shutdown();
		piExes.shutdown();
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
			while(true){
				var r = InterruptHelper.interruptSuspendRequestReason(this);
				if(r.isPresent()) switch(r.get()){
					case SUSPEND:
					case TERMINATE: return;
				}
				else try {
					if(sleeps.isEmpty()) break;
					Thread.sleep(sleeps.poll());
				} catch(InterruptedException e){
					Thread.currentThread().interrupt(); //Re-set the interrupt status
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
