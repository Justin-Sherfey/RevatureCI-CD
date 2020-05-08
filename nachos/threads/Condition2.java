package nachos.threads;

import java.util.Queue;
import java.util.LinkedList;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 * 
 * <p>
 * You must implement this.
 * 
 * @see nachos.threads.Condition
 */
public class Condition2 {
	/**
	 * Allocate a new condition variable.
	 * 
	 * @param conditionLock the lock associated with this condition variable.
	 * The current thread must hold this lock whenever it uses <tt>sleep()</tt>,
	 * <tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;
		waitQueue = new LinkedList<KThread>();
	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The current
	 * thread must hold the associated lock. The thread will automatically
	 * reacquire the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		
		boolean intStatus = Machine.interrupt().disable();
		
		KThread currentThread = KThread.currentThread();
		currentThread.setCV(this);
		
		waitQueue.add(currentThread);
		conditionLock.release();
		KThread.sleep();
		conditionLock.acquire();
		
		Machine.interrupt().restore(intStatus);
		
		return;
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread must hold the associated lock.
	 */
	public void wake() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		
		boolean intStatus = Machine.interrupt().disable();
		
		KThread waitThread = null;
		do {
			waitThread = waitQueue.poll();
		} while(waitThread != null);

		if(waitThread != null) {
			ThreadedKernel.alarm.cancel(waitThread);
			waitThread.setCV(null);
			waitThread.ready();
		}
		
		Machine.interrupt().restore(intStatus);
		return;
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wakeAll() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		
		boolean intStatus = Machine.interrupt().disable();

		while(true) {
			KThread waitThread = waitQueue.poll();
			if(waitThread==null) {
				break;
			} else {
				ThreadedKernel.alarm.cancel(waitThread);
				waitThread.setCV(null);
				waitThread.ready();
			}
		}

		Machine.interrupt().restore(intStatus);

		return;
	}

    /**
	 * Atomically release the associated lock and go to sleep on
	 * this condition variable until either (1) another thread
	 * wakes it using <tt>wake()</tt>, or (2) the specified
	 * <i>timeout</i> elapses.  The current thread must hold the
	 * associated lock.  The thread will automatically reacquire
	 * the lock before <tt>sleep()</tt> returns.
	 */
    public void sleepFor(long timeout) {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		if(timeout<=0) {
			return;
		}

		boolean intStatus = Machine.interrupt().disable();

		KThread currentKThread = KThread.currentThread();
		currentKThread.setCV(this);

		waitQueue.add(currentKThread);
		conditionLock.release();
		ThreadedKernel.alarm.waitUntil(timeout);
		conditionLock.acquire();
		
		Machine.interrupt().restore(intStatus);
		
		return;
	}

	public void cancel(KThread thread) {
		for(KThread waitThread : waitQueue) {
			if(waitThread.compareTo(thread)==0) {
				waitQueue.remove(waitThread);
				return;
			}
		}
		Lib.assertTrue(false, "this line should not be reachable.");
		return;
	}

	public static void selfTest() {
		// new InterlockTest();
		// cvTest5();
		sleepForTest1();
	}

	private static class InterlockTest {
        private static Lock lock;
        private static Condition2 cv;

        private static class Interlocker implements Runnable {
            public void run () {
                lock.acquire();
                for (int i = 0; i < 10; i++) {
                    System.out.println(KThread.currentThread().getName());
                    cv.wake();   // signal
                    cv.sleep();  // wait
                }
                lock.release();
            }
        }

        public InterlockTest () {
            lock = new Lock();
            cv = new Condition2(lock);

            KThread ping = new KThread(new Interlocker());
            ping.setName("ping");
            KThread pong = new KThread(new Interlocker());
            pong.setName("pong");

            ping.fork();
            pong.fork();

            // We need to wait for ping to finish, and the proper way
            // to do so is to join on ping.  (Note that, when ping is
            // done, pong is sleeping on the condition variable; if we
            // were also to join on pong, we would block forever.)
            // For this to work, join must be implemented.  If you
            // have not implemented join yet, then comment out the
            // call to join and instead uncomment the loop with
            // yields; the loop has the same effect, but is a kludgy
            // way to do it.
            ping.join();
            // for (int i = 0; i < 50; i++) { KThread.currentThread().yield(); }
		}
	}

	public static void cvTest5() {
        final Lock lock = new Lock();
        // final Condition empty = new Condition(lock);
        final Condition2 empty = new Condition2(lock);
        final LinkedList<Integer> list = new LinkedList<>();

        KThread consumer = new KThread( new Runnable () {
                public void run() {
                    lock.acquire();
                    while(list.isEmpty()){
                        empty.sleep();
                    }
                    Lib.assertTrue(list.size() == 5, "List should have 5 values.");
                    while(!list.isEmpty()) {
                        // context swith for the fun of it
                        KThread.currentThread().yield();
                        System.out.println("Removed " + list.removeFirst());
                    }
                    lock.release();
                }
            });

        KThread producer = new KThread( new Runnable () {
                public void run() {
                    lock.acquire();
                    for (int i = 0; i < 5; i++) {
                        list.add(i);
                        System.out.println("Added " + i);
                        // context swith for the fun of it
                        KThread.currentThread().yield();
                    }
                    empty.wake();
                    lock.release();
                }
            });

        consumer.setName("Consumer");
        producer.setName("Producer");
        consumer.fork();
        producer.fork();

        // We need to wait for the consumer and producer to finish,
        // and the proper way to do so is to join on them.  For this
        // to work, join must be implemented.  If you have not
        // implemented join yet, then comment out the calls to join
        // and instead uncomment the loop with yield; the loop has the
        // same effect, but is a kludgy way to do it.
        consumer.join();
        producer.join();
        //for (int i = 0; i < 50; i++) { KThread.currentThread().yield(); }
    }

	private static void sleepForTest1() {
		Lock lock = new Lock();
		Condition2 cv = new Condition2(lock);
	
		lock.acquire();
		long t0 = Machine.timer().getTime();
		System.out.println (KThread.currentThread().getName() + " sleeping");
		// no other thread will wake us up, so we should time out
		cv.sleepFor(2000);
		long t1 = Machine.timer().getTime();
		System.out.println (KThread.currentThread().getName() +
					" woke up, slept for " + (t1 - t0) + " ticks");
		lock.release();
	}

	private static void sleepForTest2() {
		Lock lock = new Lock();
		Condition2 cv = new Condition2(lock);
	
		lock.acquire();
		long t0 = Machine.timer().getTime();
		System.out.println (KThread.currentThread().getName() + " sleeping");
		// no other thread will wake us up, so we should time out
		cv.sleepFor(2000);
		long t1 = Machine.timer().getTime();
		System.out.println (KThread.currentThread().getName() +
					" woke up, slept for " + (t1 - t0) + " ticks");
		lock.release();
	}

	private Lock conditionLock;
	private Queue<KThread> waitQueue;
}
