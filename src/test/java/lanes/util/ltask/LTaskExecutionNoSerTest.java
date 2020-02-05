package lanes.util.ltask;

import lanes.util.ltask.impl.LTaskExeSOnThreadPool;
import lanes.util.ltask.impl.SimpleTaskContext;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class LTaskExecutionNoSerTest {

	@ParameterizedTest
	@ValueSource(ints = {2, 4, 8, 16})
	public void testJustParallelExecution(int pool){
		final long sleep = 1000;
		var exes = new LTaskExeSOnThreadPool(pool, pool);
		var ctxt = new SimpleTaskContext(exes);
		AtomicBoolean[] ress = Stream.generate(AtomicBoolean::new).limit(pool).toArray(AtomicBoolean[]::new);
		IntStream.range(0, pool).forEach(i -> ctxt.submit(new ParamSleeperTask(() -> ress[i].set(true), sleep)));
		long slept = 0;
		while(slept < sleep + sleep/8) try{
			Thread.sleep(sleep/8);
			slept += sleep/8;
		} catch(InterruptedException e){}
		assertTrue(Arrays.stream(ress).allMatch(AtomicBoolean::get), "Not all tasks executed! - " + Arrays.toString(ress));
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
			if(this.context != null) throw new UnsupportedOperationException();
			this.context = context;
		}

		@Override
		public void run(){
			while(sleeps.peek() != null){
				try {
					Thread.sleep(sleeps.poll());
				} catch(InterruptedException e){ }
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
