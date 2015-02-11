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
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class DynamicPriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public DynamicPriorityScheduler() {

        //initialize statistics and logging for Part D.
        Log.init();
        totalNumThreads = -1;
        WaitingTime = 0;
        TurnaroundTime = 0;
        maximumWaitingTime = 0;
    }

    public void printSystemStats(){
        Log.write("System,"+totalNumThreads+","+
                  WaitingTime/totalNumThreads+","+
                  TurnaroundTime/totalNumThreads+","+
                  maximumWaitingTime);
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
        return waitQueue = new DThreadPriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getDThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getDThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
        Lib.debug('t', Machine.interrupt().disabled() + "");
        Lib.assertTrue(Machine.interrupt().disabled());

        Lib.assertTrue(priority >= priorityMinimum &&
                priority <= priorityMaximum);

        getDThreadState(thread).setPriority(priority);
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

    public void threadCreated(KThread thread){//Keeps track of number of threads. 
        totalNumThreads++;
        getDThreadState(thread).arrived();

    }

    public void threadDestroyed(KThread thread){//Displays statistics according to thread destruction and how it affects the time for other threads and maxWaiting time. 
        DThreadState t = getDThreadState(thread);
        t.departed();
        Log.write(thread.getName()+","+
                  t.arrival+","+
                  t.running+","+
                  t.waiting+","+
                  Log.getTimeInMillis());

        //add to system level stats
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

    public static final int agingTime =
    Integer.parseInt(Config.getString("scheduler.agingTime"));
    
    private int totalNumThreads;
    private long WaitingTime;
    private long TurnaroundTime;
    private long maximumWaitingTime;
    private DThreadPriorityQueue waitQueue;

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    public static DThreadState getDThreadState(KThread kthread) {
        if (kthread.schedulingState == null)
            kthread.schedulingState = new DThreadState(kthread);

        return (DThreadState) kthread.schedulingState; 
    }


}

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    class DThreadPriorityQueue extends ThreadQueue {
        DThreadPriorityQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;
        }

        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            insert(thread);
            DynamicPriorityScheduler.getDThreadState(thread).waitForAccess(this);
        }

        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            DynamicPriorityScheduler.getDThreadState(thread).acquire(this);
        }

        public KThread nextThread() {//According to policies of Dynamic Thread Scheduling, adjusts age of threads in the waitQueue and returns the thread that is next (lowest CPU time etc; lowest in age)
            Lib.assertTrue(Machine.interrupt().disabled());
            
            if(waitQueue.isEmpty())
                return null;

            age();

            KThread temp = (KThread) waitQueue.poll();
            DThreadState ts = DynamicPriorityScheduler.getDThreadState(temp);
            ts.scheduled();

            //write stats about scheduled thread
            Log.write(Log.getTimeInMillis() + ","+
                      temp.getName()+","+
                      ts.getEffectivePriority());

            return temp;
        }

        public void age(){//implements aging of threads in queue. 
            Iterator iter = waitQueue.iterator();
            for( ; iter.hasNext(); ){
                KThread t = (KThread) iter.next();
                DynamicPriorityScheduler.getDThreadState(t).age();
            }
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
        
        //inserts thread into queue
        public void insert(KThread thread){
            
            waitQueue.add(thread);
        }

        //removes specified thread from the queue
        public void remove(KThread thread){
            waitQueue.remove(thread);
        }

        //returns true if the queue contains the thread
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
        private PriorityQueue waitQueue = new PriorityQueue(10, new
        Comparator<KThread>() {
            public int compare(KThread t1, KThread t2){
                DThreadState s1, s2;
                s1 = DynamicPriorityScheduler.getDThreadState(t1);
                s2 = DynamicPriorityScheduler.getDThreadState(t2);
                if(s1.getEffectivePriority() < s2.getEffectivePriority())
                    return -1;
                else if(s1.getEffectivePriority() > s2.getEffectivePriority())
                    return 1;
                else if(s1.last < s2.last)
                    return -1;
                else if(s1.last > s2.last)
                    return 1;
                else 
                    return 0;
            }
        });
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    class DThreadState {
        /**
         * Allocate a new <tt>DThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param	thread	the thread this state belongs to.
         */
        public DThreadState(KThread thread) {
            this.thread = thread;

            setPriority(DynamicPriorityScheduler.priorityDefault);
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
            //find out how long it has been waiting compared to how long it has
            //run. If it has run for just as long as it has waited, then the
            //priority should be the same as what was set.
            int sum = (int) (waiting - running);

            //divide by the aging time. If a thread has waited for 5ms and the
            //aging time is 5ms, then its priority increases by one
            int effectivePriority = priority - sum / DynamicPriorityScheduler.agingTime;

            //make sure we are still within range
            if(effectivePriority < DynamicPriorityScheduler.priorityMinimum)
                effectivePriority = DynamicPriorityScheduler.priorityMinimum;
            else if(effectivePriority > DynamicPriorityScheduler.priorityMaximum)
                effectivePriority = DynamicPriorityScheduler.priorityMaximum;

            return effectivePriority;
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
        public void waitForAccess(DThreadPriorityQueue waitQueue) {
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
        public void acquire(DThreadPriorityQueue waitQueue) {
            scheduled();
        }
       
        //start the waiting timer 
        public void arrived(){
            arrival = Log.getTimeInMillis();
            last = arrival;   
        }

        //add the time it was running before it ended
        public void departed(){
            long diff = Log.getTimeInMillis() - last;
            if(wasRunning){
                running += diff;
                wasRunning = false;
                last = Log.getTimeInMillis();
            }

        }

        //add the time its been waiting and start run timer
        public void scheduled(){
            long diff = Log.getTimeInMillis() - last;
            wasRunning = true;
            waiting += diff;
            last = Log.getTimeInMillis();
        }

        //update age by making sure that waiting time is current,
        //getEffectivePriority will calculate the rest
        public void age(){
            long diff = Log.getTimeInMillis() - last;
            waiting += diff;
            last = Log.getTimeInMillis();
        }

        //add running time and start wait timer
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
