package nachos.threads;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	this.conditionLock = conditionLock;
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    
    /*
     * 
     * sleep(): atomically release the lock and relinkquish the CPU until woken; then reacquire the lock.

       release condition lock
       diable interrupt
       add current thread to wait queue
       make current thread sleep
       restore interrupt 
       acquire condition lock
       
	wake(): wake up a single thread sleeping in this condition variable, if possible.

      if wait queue is not empty 
          disable interrupt
          remove the first element from wait queue 
          put the first element into ready queue
          restore interrupt
          
	wakeAll(): wake up all threads sleeping inn this condition variable.

       while wait queue is not empty
          invoke wake() 
     */
    public void sleep() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());

	Machine.interrupt().disable();

	conditionLock.release();
	KThread thread = KThread.currentThread();
	waitQueue.waitForAccess(thread);
	KThread.sleep();

	conditionLock.acquire();
	Machine.interrupt().enable();
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	
	Machine.interrupt().disable();
	KThread thread = waitQueue.nextThread();
	if (thread != null) {
		thread.ready();
		System.out.println("Wake up a thread");
	}
	Machine.interrupt().enable();    
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	while(waitQueue.nextThread() != null){
		System.out.println("Wake up a thread");
		wake();
	}
	Machine.interrupt().enable();
    }

    private Lock conditionLock;
    private ThreadQueue waitQueue = ThreadedKernel.scheduler.newThreadQueue(true);
}
