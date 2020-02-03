package lanes.util.ltask.impl;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class PauseWaitingLock {

	private boolean paused = false;
	private final ReentrantLock lock = new ReentrantLock();
	private final Condition unpaused = lock.newCondition();

	public void pause(){
		lock.lock();
		try {
			this.paused = true;
		} finally {
			lock.unlock();
		}
	}

	public void resume(){
		lock.lock();
		try {
			this.paused = false;
		} finally {
			lock.unlock();
		}
	}

	public boolean isPaused(){
		lock.lock();
		try {
			return paused;
		} finally {
			lock.unlock();
		}
	}

	public void awaitIfPaused(@NonNull Thread t){
		lock.lock();
		try {
			while(this.paused) unpaused.await();
		} catch(InterruptedException e){
			t.interrupt();
		} finally {
			lock.unlock();
		}
	}

}
