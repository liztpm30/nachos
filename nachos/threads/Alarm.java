package nachos.threads;

import java.util.PriorityQueue;

import nachos.machine.*;

/*
 * 
 * Alarm(): allocate a new Alarm, set the machine's timer interrupt handler to this alarm's callback.

  create a Runnable object to wrap run(), run() just to invoke this.timerInterrupt 
  invoke Machine.timer().setInterruptHandler, specify Runnable object as the on
  
  
  timerInterrupt(): callback function, will be invoked by Nacho machine's timer approximately every 500 clock ticks.

  check if threads in waiting queue are due
  put due threads into ready queue
  
  
 * waitUntil(long x): put the current thread to sleep for at least x ticks. The thread must be woken up during the first timer interrupt where
	(current time) >= (WaitUntil called time) + (x)

   current time plus wait time, get the due time.    
   put current thread and due time into waiting queue
   current thread sleeps 
 */



/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
	
	public PriorityQueue<KThreadTimer> sleepingThreads = new PriorityQueue<KThreadTimer>();
	
	
	
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }
    

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
	

	Machine.interrupt().disable();
	
	while (sleepingThreads.peek() != null && sleepingThreads.peek().getWaitTime() <= Machine.timer().getTime()) {
		sleepingThreads.remove().getThread().ready();
	}

	KThread.yield();
	
	Machine.interrupt().enable();

	KThread.yield();
  
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    

    
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
	long wakeTime = Machine.timer().getTime() + x;

	
	KThreadTimer currentThreadTimer = new KThreadTimer(KThread.currentThread(), wakeTime);
	
	Machine.interrupt().disable();
	
	sleepingThreads.add(currentThreadTimer);
	KThread.sleep();
	
	Machine.interrupt().enable();
    }
}
