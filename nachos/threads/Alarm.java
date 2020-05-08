package nachos.threads;

import java.util.PriorityQueue;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		// KThread.currentThread().yield();
		
			
		boolean intStatus = Machine.interrupt().disable();
		// Constantly checks to make sure sleepQueue is not empty and has not reached machine's timer
		while(!sleepQueue.isEmpty() && sleepQueue.peek().wakeTime <= Machine.timer().getTime()) {
			KThread wakeThread = sleepQueue.poll().thread;
			if(wakeThread.getCV()!=null) {
				wakeThread.getCV().cancel(wakeThread);
			}
			wakeThread.ready();
		}
		// if it has then force an interrupt
		Machine.interrupt().restore(intStatus);
		
		return;
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		// long wakeTime = Machine.timer().getTime() + x;
		// while (wakeTime > Machine.timer().getTime()) {
		// 	KThread.yield();
		// }

		// makes sure clock ticks a positive number
		if(x <= 0) {
			return;
		} else {
			boolean intStatus = Machine.interrupt().disable();
			long wakeTime = Machine.timer().getTime() + x;
			SleepThread sleepThread = new SleepThread(KThread.currentThread(), wakeTime);
			sleepQueue.add(sleepThread);
			KThread.sleep();
			Machine.interrupt().restore(intStatus);
			return;
		}
	}

    /**
	 * Cancel any timer set by <i>thread</i>, effectively waking
	 * up the thread immediately (placing it in the scheduler
	 * ready set) and returning true.  If <i>thread</i> has no
	 * timer set, return false.
	 * 
	 * <p>
	 * @param thread the thread whose timer should be cancelled.
	 */
    public boolean cancel(KThread thread) {
		for(SleepThread st : sleepQueue) {
			if(thread.compareTo(st.thread)==0) {
				sleepQueue.remove(st);
				return true;
			}
		}
		return false;
	}

	/**
	 * SleepThread encapsulate KThread and wakeTime
	 */
	private class SleepThread implements Comparable<SleepThread>{
		public KThread thread;
		public long wakeTime;

		public SleepThread(KThread thread, long t) {
			this.thread = thread;
			wakeTime = t;
		}

		public int compareTo(SleepThread t) {
			if(wakeTime < t.wakeTime) {
				return -1;
			}
			if(wakeTime > t.wakeTime) {
				return 1;
			}
			return 0; // should raise an error.
		}
	}
	
	public PriorityQueue<SleepThread> sleepQueue = new PriorityQueue<SleepThread>();
	
	// sample alarm test, makes sure Alarm is waiting
	public static void alarmTest1() {
		int durations[] = {1000, 10*1000, 100*1000};
		long t0, t1;

		for (int d : durations) {
		    t0 = Machine.timer().getTime();
		    ThreadedKernel.alarm.waitUntil(d);
		    t1 = Machine.timer().getTime();
		    System.out.println ("alarmTest1: waited for " + (t1 - t0) + " ticks");
		}
    }
}
