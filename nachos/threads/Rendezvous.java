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

    private HashMap<Integer, KThread> waitThread;
    private HashMap<Integer, Integer> value;
    
    public Rendezvous () {
        waitThread = new HashMap<Integer, KThread>();
        value = new HashMap<Integer, Integer>();
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
        if(!waitThread.containsKey(tag)) {
            waitThread.put(tag, KThread.currentThread());
            this.value.put(tag, value);
            KThread.sleep();

            Machine.interrupt().restore(intStatus);
            return this.value.get(tag);
        } else {
            KThread thread = waitThread.get(tag);
            if(thread==null) {
                waitThread.put(tag, KThread.currentThread());
                this.value.put(tag, value);
                KThread.sleep();
                
                Machine.interrupt().restore(intStatus);
                return this.value.get(tag);
            } else {
                int returnValue = this.value.get(tag);
                KThread wakeThread = waitThread.get(tag);
                this.value.replace(tag, value);
                waitThread.replace(tag, null);
                wakeThread.ready();

                Machine.interrupt().restore(intStatus);
                return returnValue;
            }
        }
    }

    public static void selfTest() {
        rendezTest1();    
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
}
