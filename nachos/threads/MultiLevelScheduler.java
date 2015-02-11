package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
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
public class MultiLevelScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public MultiLevelScheduler() {

        //initialize statistics and logging for part D.
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
        return waitQueue = new MThreadPriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getMThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getMThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
        Lib.debug('t', Machine.interrupt().disabled() + "");
        Lib.assertTrue(Machine.interrupt().disabled());

        Lib.assertTrue(priority >= priorityMinimum &&
                priority <= priorityMaximum);

        getMThreadState(thread).setPriority(priority);
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

    public void threadCreated(KThread thread){//increments number of threads total 
        totalNumThreads++;
        getMThreadState(thread).arrived();

    }

    public void threadDestroyed(KThread thread){ 
        MThreadState t = getMThreadState(thread);
        t.departed();

        //print thread stats when destroyed
        Log.write(thread.getName()+","+
                  t.arrival+","+
                  t.running+","+
                  t.waiting+","+
                  Log.getTimeInMillis());

        //update system level stats
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
    Integer.parseInt(Config.getString("scheduler.maxPriorityValue"));//Changed according to implementation of threadDestroyed(Kthread thread). 

    public static final int agingTime =
    Integer.parseInt(Config.getString("scheduler.agingTime"));
    
    private int totalNumThreads;
    private long WaitingTime;
    private long TurnaroundTime;
    private long maximumWaitingTime;
    private MThreadPriorityQueue waitQueue;

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    public static MThreadState getMThreadState(KThread kthread) {
        if (kthread.schedulingState == null)
            kthread.schedulingState = new MThreadState(kthread);

        return (MThreadState) kthread.schedulingState; 
    }


}

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    class MThreadPriorityQueue extends ThreadQueue {
        MThreadPriorityQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;
        }

        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            insert(thread);
            MultiLevelScheduler.getMThreadState(thread).waitForAccess(this);
        }

        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            MultiLevelScheduler.getMThreadState(thread).acquire(this);
        }

        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());
            
            //age the threads
            age();

            //Picks the first non empty queue out of topWaitQueue, midWaitQueue and botWaitQueue.
            LinkedList current;
            if(!topWaitQueue.isEmpty())
                current = topWaitQueue;
            else if(!midWaitQueue.isEmpty())
                current = midWaitQueue;
            else if(!botWaitQueue.isEmpty())
                current = botWaitQueue;
            else
                return null;


            KThread temp = (KThread) current.poll();
            MThreadState ts = MultiLevelScheduler.getMThreadState(temp);
            ts.scheduled();

            //write stats about scheduled thread
            Log.write(Log.getTimeInMillis() + ","+
                      temp.getName()+","+
                      ts.getEffectivePriority());

            return temp;
        }

        public void age(){//Implements age by ensure threads are in queues according to priorities compared to the thread currently in queue and the restrictions of the queues themselves (topWait:0-10, midWait:11-20, botWait:21 and above)
            
            //for each queue, age each thread, removing it from the queue if it
            //exceeds the limit and add it back to the appropriate one
            Iterator iter = topWaitQueue.iterator();
            for( ; iter.hasNext(); ){
                KThread t = (KThread) iter.next();
                MThreadState ts = MultiLevelScheduler.getMThreadState(t);
                ts.age();
                if(ts.getEffectivePriority() > 10){
                    iter.remove();
                    insert(t);
                }
            }
            iter = midWaitQueue.iterator();
            for( ; iter.hasNext(); ){
                KThread t = (KThread) iter.next();
                MThreadState ts = MultiLevelScheduler.getMThreadState(t);
                ts.age();
                if(ts.getEffectivePriority() < 11 || ts.getEffectivePriority() > 20){
                    iter.remove();
                    insert(t);
                }
            }
            iter = botWaitQueue.iterator();
            for( ; iter.hasNext(); ){
                KThread t = (KThread) iter.next();
                MThreadState ts = MultiLevelScheduler.getMThreadState(t);
                ts.age();
                if(ts.getEffectivePriority() < 21){
                    iter.remove();
                    insert(t);
                }
            }
        }

        /**
         * Return the next thread that <tt>nextThread()</tt> would return,
         * without modifying the state of this queue.
         *
         * @return	the next thread that <tt>nextThread()</tt> would
         *		return.
         */
        //protected KThread pickNextThread() {
        //}

        public void print() {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me (if you want)
        }
        

        //Adds thread in the right position of the linked list based on its effective priority.
        public void insert(KThread thread){
            int priority =
                MultiLevelScheduler.getMThreadState(thread).getEffectivePriority();

            if(priority < 11){
                topWaitQueue.add(thread);
            }
            else if (priority < 21){
                midWaitQueue.add(thread);
            }
            else{
                botWaitQueue.add(thread);
            }
                
        }

        /**
         * <tt>true</tt> if this queue should transfer priority from waiting
         * threads to the owning thread.
         */
        public boolean transferPriority;
 
        private LinkedList topWaitQueue = new LinkedList();
        private LinkedList midWaitQueue = new LinkedList();
        private LinkedList botWaitQueue = new LinkedList();
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    class MThreadState {
        /**
         * Allocate a new <tt>MThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param	thread	the thread this state belongs to.
         */
        public MThreadState(KThread thread) {
            this.thread = thread;

            setPriority(MultiLevelScheduler.priorityDefault);
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

            //divide by the aging time because if a thread has waited 5ms and
            //the aging time is 5ms then its priority is increased by 1
            int effectivePriority = priority - sum / MultiLevelScheduler.agingTime;

            //keep priorities within range of limits
            if(effectivePriority < MultiLevelScheduler.priorityMinimum)
                effectivePriority = MultiLevelScheduler.priorityMinimum;
            else if(effectivePriority > MultiLevelScheduler.priorityMaximum)
                effectivePriority = MultiLevelScheduler.priorityMaximum;

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
        public void waitForAccess(MThreadPriorityQueue waitQueue) {
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
        public void acquire(MThreadPriorityQueue waitQueue) {//Checks how to acquire thread from Pool and it will be the next scheduled that will be picked up. 
            scheduled();
        }
        
        public void arrived(){
            arrival = Log.getTimeInMillis();
            last = arrival;   
        }

        public void departed(){
            long diff = Log.getTimeInMillis() - last;
            if(wasRunning){
                running += diff;
                wasRunning = false;
                last = Log.getTimeInMillis();
            }

        }

        public void scheduled(){
            long diff = Log.getTimeInMillis() - last;
            wasRunning = true;
            waiting += diff;
            last = Log.getTimeInMillis();
        }

        public void age(){
            long diff = Log.getTimeInMillis() - last;
            waiting += diff;
            last = Log.getTimeInMillis();
        }

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
