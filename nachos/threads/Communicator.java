package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    	
    	// Allocate a new communicator, and initialize class members.
    	
		mutex=new Lock();
		sleepingSpeakers=new Condition(mutex);
		sleepingListeners=new Condition(mutex);
		currentSpeaker=new Condition(mutex);
		occupied=false;
    	
    	
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    

    
    public void speak(int word) {
    	
   		/*
		 * Wait for a thread to listen through this communicator, and then transfer word to the listener. Note Exactly one listener should receive word .

       speaker acquires the lock
       increase the number of speakers by one

       while no available listener or word is ready(but listener hasn't fetched it)
               speaker goes to sleep

       speaker says a word
       set flag to show that word is ready.
       wake up all the listener
      
       decrease the number of speaker by one
       speaker releases the lock     
		 */
    	
		mutex.acquire();
		
		while(occupied){
			sleepingSpeakers.sleep();
		}
		occupied=true;
		transfer=word;
		sleepingListeners.wake();
		currentSpeaker.sleep();
		mutex.release();
    	
    	
    	
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    
    
    /*
     * Wait for a thread to speak through this communicator, and then return the word that thread passed to speak.

           listener acquires the lock
           increase the number of listener by one
          
           while word is not ready
                      Wake up all the speaker (although it's not efficient, it can keep code simple enough)
                      listener goes to sleep
                     

           (By now listener is waken up and require the lock again)

           listener receives the word
           reset flag to show that word is unavailable 

           decrease the number of listener by one
           listener releases the lock  
     */
    public int listen() {
	mutex.acquire();
		
		while(!occupied){
			sleepingListeners.sleep();
		}
		int temp= transfer;
		occupied=false;
		
		currentSpeaker.wake();
		sleepingSpeakers.wake();
		mutex.release();
		return temp;
    }
    
	private Lock mutex;	
	private Condition  sleepingSpeakers;
	private Condition sleepingListeners;
	private Condition currentSpeaker;
	private boolean occupied;
	private int transfer;
	
}
