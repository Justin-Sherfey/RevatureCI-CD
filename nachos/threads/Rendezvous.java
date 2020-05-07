package nachos.threads;

import java.util.HashMap;

import nachos.machine.*;

/**
 * A <i>Rendezvous</i> allows threads to synchronously exchange values.
 */
public class Rendezvous {
    /**
     * Allocate a new Rendezvous.
     */
    private HashMap<Integer, Integer> turn; // 0 for producer, 1 for consumer
    private HashMap<Integer, Boolean> isDone;
    private HashMap<Integer, Lock> lock;
    private HashMap<Integer, Condition> waitCV;
    private HashMap<Integer, Condition> readCV;
    private HashMap<Integer, Integer> buffer;
    
    public Rendezvous () {
        turn = new HashMap<Integer, Integer>();
        isDone = new HashMap<Integer, Boolean>();
        lock = new HashMap<Integer, Lock>();
        waitCV = new HashMap<Integer, Condition>();
        readCV = new HashMap<Integer, Condition>();
        buffer = new HashMap<Integer, Integer>();
    }

    /**
     * Synchronously exchange a value with another thread.  The first
     * thread A (with value X) to exhange will block waiting for
     * another thread B (with value Y).  When thread B arrives, it
     * will unblock A and the threads will exchange values: value Y
     * will be returned to thread A, and value X will be returned to
     * thread B.
     *
     * Different integer tags are used as different, parallel
     * synchronization points (i.e., threads synchronizing at
     * different tags do not interact with each other).  The same tag
     * can also be used repeatedly for multiple exchanges.
     *
     * @param tag the synchronization tag.
     * @param value the integer to exchange.
     */
    public int exchange (int tag, int value) {
        boolean intStatus = Machine.interrupt().disable();
        int returnValue = 0;
        if(!turn.containsKey(tag)) {
            //Initialize
            turn.put(tag, 0);
            isDone.put(tag, true);
            Lock guard = new Lock();
            Condition wait = new Condition(guard);
            Condition read = new Condition(guard);
            lock.put(tag, guard);
            waitCV.put(tag, wait);
            readCV.put(tag, read);
            buffer.put(tag, 0);

            guard.acquire();
            isDone.replace(tag, false);
            buffer.replace(tag, value);
            turn.replace(tag, 1);
            wait.wake();
            read.sleep();

            returnValue = buffer.get(tag);
            isDone.replace(tag, true);
            turn.replace(tag, 0);
            wait.wake();
            guard.release();
        } else {
            Lock guard = lock.get(tag);
            Condition wait = waitCV.get(tag);
            Condition read = readCV.get(tag);
            //System.out.println("***Thread " + KThread.currentThread() + "---" + isDone.get(tag) + "---" + turn.get(tag));
            guard.acquire();
            while(isDone.get(tag)==false && turn.get(tag)==0) {
                // sleep new producer while current exchange has not done yet.
                wait.sleep();
            }
            if(turn.get(tag)==0) {
                isDone.replace(tag, false);
                buffer.replace(tag, value);
                turn.replace(tag, 1);
                wait.wake();
                read.sleep();
                returnValue = buffer.get(tag);
                isDone.replace(tag, true);
                turn.replace(tag, 0);
                wait.wake();
                guard.release();
            } else {
                returnValue = buffer.get(tag);
                buffer.replace(tag, value);
                turn.replace(tag, 0);
                read.wake();
                guard.release();
            }
        }
        Machine.interrupt().restore(intStatus);
        return returnValue;
    }

    public static void selfTest() {
        rendezTest2();    
    }

    public static void rendezTest1() {
        final Rendezvous r = new Rendezvous();
    
        KThread t1 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = -1;
    
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
            });
        t1.setName("t1");
        KThread t2 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = 1;
    
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
            });
        t2.setName("t2");
    
        t1.fork(); t2.fork();
        // assumes join is implemented correctly
        t1.join(); t2.join();
    }

    public static void rendezTest2() {
        final Rendezvous r = new Rendezvous();
    
        KThread t1 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = -1;
    
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
            });
        t1.setName("t1");
        KThread t2 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = 1;
    
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
            });
        t2.setName("t2");

        KThread t3 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = -2;
    
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 2, "Thread " + KThread.currentThread().getName() + " Was expecting " + 2 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
            });
        t3.setName("t3");
        KThread t4 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = 2;
    
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == -2, "Thread " + KThread.currentThread().getName() + " Was expecting " + -2 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
            });
        t4.setName("t4");
    
        // assumes join is implemented correctly
        t1.fork(); t2.fork();
        t3.fork(); t4.fork();
        t1.join(); t2.join();
        t3.join(); t4.join();
    }
}
