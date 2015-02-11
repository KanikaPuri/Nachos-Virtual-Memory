package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.LinkedList;
import java.util.Comparator;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fashion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class StaticPriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public StaticPriorityScheduler() {

        //initialize statistics and logging
        Log.init();

        totalNumThreads = -1;
        WaitingTime = 0;
        TurnaroundTime = 0;
        maximumWaitingTime = 0;

    }

    public void printSystemStats(){//inserted to maintain statistics for part D.
        Log.destroy(); 
    }

    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return waitQueue = new SThreadPriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getSThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getSThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
        Lib.debug('t', Machine.interrupt().disabled() + "");
        Lib.assertTrue(Machine.interrupt().disabled());

        Lib.assertTrue(priority >= priorityMinimum &&
                priority <= priorityMaximum);

        getSThreadState(thread).setPriority(priority);
        thread.resetPriority();
    }

    public boolean increasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMaximum)
            return false;

        setPriority(thread, priority+1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    public boolean decreasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMinimum)
            return false;

        setPriority(thread, priority-1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    public void threadCreated(KThread thread){//counts number of threads
        totalNumThreads++;
        getSThreadState(thread).arrived();

    }

    public void threadDestroyed(KThread thread){//Write stats about recently destroyed thread
        SThreadState t = getSThreadState(thread);
        t.departed();

        //recalculate system level stats
        WaitingTime += t.waiting;
        TurnaroundTime += Log.getTimeInMillis() - t.arrival;
        if(t.waiting > maximumWaitingTime)
            maximumWaitingTime = t.waiting;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum =  
    Integer.parseInt(Config.getString("scheduler.maxPriorityValue")); 
    
    private int totalNumThreads;
    private long WaitingTime;
    private long TurnaroundTime;
    private long maximumWaitingTime;
    private SThreadPriorityQueue waitQueue;

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    public static SThreadState getSThreadState(KThread kthread) {
        if (kthread.schedulingState == null)
            kthread.schedulingState = new SThreadState(kthread);

        return (SThreadState) kthread.schedulingState; 
    }


}

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    class SThreadPriorityQueue extends ThreadQueue {
        SThreadPriorityQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;
        }

        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            insert(thread);
            StaticPriorityScheduler.getSThreadState(thread).waitForAccess(this);
        }

        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            StaticPriorityScheduler.getSThreadState(thread).acquire(this);
        }

        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());
            
            if(waitQueue.isEmpty())
                return null;

            //get next thread from queue
            Iterator iter = waitQueue.iterator();
            KThread temp = (KThread) iter.next();
            while(iter.hasNext()){
                KThread next = (KThread) iter.next();
                int temp_p, next_p;
                if(transferPriority){
                    temp_p =
                        StaticPriorityScheduler.getSThreadState(temp).getEffectivePriority();
                    next_p = 
                        StaticPriorityScheduler.getSThreadState(next).getEffectivePriority();
                }
                else{
                    temp_p =
                        StaticPriorityScheduler.getSThreadState(temp).getPriority();
                    next_p = 
                        StaticPriorityScheduler.getSThreadState(next).getPriority();
                }
                if(next_p < temp_p)
                    temp = next;
            }

            waitQueue.remove(temp);

            SThreadState ts = StaticPriorityScheduler.getSThreadState(temp);
            ts.scheduled();

            //print stats about scheduled thread
            //Log.write("S," + Log.getTimeInMillis() + ","+
            //         temp.getName()+","+
            //          (transferPriority ? ts.getEffectivePriority() :
            //          ts.getPriority()));

            return temp;
        }

        /**
         * Return the next thread that <tt>nextThread()</tt> would return,
         * without modifying the state of this queue.
         *
         * @return	the next thread that <tt>nextThread()</tt> would
         *		return.
         */
        protected KThread pickNextThread() {
            if(waitQueue.isEmpty())
                return null;

            return (KThread) waitQueue.peek();
            
        }

        public void print() {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me (if you want)
        }
        

        //adds a thread in the right position of the linked list based on its effective priority.
        public void insert(KThread thread){
            
            waitQueue.add(thread);
        }
	
	//removes a thread from the waitQueue. 
        public void remove(KThread thread){
            waitQueue.remove(thread);
        }
	
	//Returns true if the list contains the specified thread. 
        public boolean contains(KThread thread){
            return waitQueue.contains(thread);
        }   

        /**
         * <tt>true</tt> if this queue should transfer priority from waiting
         * threads to the owning thread.
         */
        public boolean transferPriority;
 
        //create a PriorityQueue and give it a comparator
        //if 2 threads have the same priority, the one that has the earliest
        //arrival time gets priority
        private LinkedList waitQueue = new LinkedList<KThread>();
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    class SThreadState {
        /**
         * Allocate a new <tt>SThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param	thread	the thread this state belongs to.
         */
        public SThreadState(KThread thread) {
            this.thread = thread;

            //setPriority(StaticPriorityScheduler.priorityDefault);
            priority = StaticPriorityScheduler.priorityDefault;
        }

        /**
         * Return the priority of the associated thread.
         *
         * @return	the priority of the associated thread.
         */
        public int getPriority() {
            return priority;
        }

        /**
         * Return the effective priority of the associated thread.
         *
         * @return	the effective priority of the associated thread.
         */
        public int getEffectivePriority() {
            // implement me
            return thread.getMin();
        }

        /**
         * Set the priority of the associated thread to the specified value.
         *
         * @param	priority	the new priority.
         */
        public void setPriority(int priority) {
            if (this.priority == priority)
                return;

            this.priority = priority;
            thread.resetPriority();

            // implement me
        }

        /**
         * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
         * the associated thread) is invoked on the specified priority queue.
         * The associated thread is therefore waiting for access to the
         * resource guarded by <tt>waitQueue</tt>. This method is only called
         * if the associated thread cannot immediately obtain access.
         *
         * @param	waitQueue	the queue that the associated thread is
         *				now waiting on.
         *
         * @see	nachos.threads.ThreadQueue#waitForAccess
         */
        public void waitForAccess(SThreadPriorityQueue waitQueue) {
            // implement me
            waiting();
        }

        /**
         * Called when the associated thread has acquired access to whatever is
         * guarded by <tt>waitQueue</tt>. This can occur either as a result of
         * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
         * <tt>thread</tt> is the associated thread), or as a result of
         * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
         *
         * @see	nachos.threads.ThreadQueue#acquire
         * @see	nachos.threads.ThreadQueue#nextThread
         */
        public void acquire(SThreadPriorityQueue waitQueue) {
            scheduled();
        }
        
        //start timer for waiting time
        public void arrived(){
            arrival = Log.getTimeInMillis();
            last = arrival;   
        }

        //if thread was running when its destroyed, add that to run time
        public void departed(){
            long diff = Log.getTimeInMillis() - last;
            if(wasRunning){
                running += diff;
                wasRunning = false;
                last = Log.getTimeInMillis();
            }

        }

        //start timing how long it runs, add how long it has waited
        public void scheduled(){
            long diff = Log.getTimeInMillis() - last;
            wasRunning = true;
            waiting += diff;
            last = Log.getTimeInMillis();
        }

        //start timer for waiting add how long its been running
        public void waiting(){
            long diff = Log.getTimeInMillis() - last;
            if(wasRunning){
                running += diff;
                wasRunning = false;
                last = Log.getTimeInMillis();
            }
        }

        /** The thread with which this object is associated. */	   
        protected KThread thread;
        /** The priority of the associated thread. */
        protected int priority;
        public long arrival = 0;
        public long waiting = 0;
        public long running = 0;
        public long last = 0;
        private boolean wasRunning = false;
    }
