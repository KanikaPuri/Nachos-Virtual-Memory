package nachos.threads;

import nachos.machine.*;
import nachos.threads.Log;

/**
 * A <tt>Lock</tt> is a synchronization primitive that has two states,
 * <i>busy</i> and <i>free</i>. There are only two operations allowed on a
 * lock:
 *
 * <ul>
 * <li><tt>acquire()</tt>: atomically wait until the lock is <i>free</i> and
 * then set it to <i>busy</i>.
 * <li><tt>release()</tt>: set the lock to be <i>free</i>, waking up one
 * waiting thread if possible.
 * </ul>
 *
 * <p>
 * Also, only the thread that acquired a lock may release it. As with
 * semaphores, the API does not allow you to read the lock state (because the
 * value could change immediately after you read it).
 */
public class Lock extends ResourceNode{
    /**
     * Allocate a new lock. The lock will initially be <i>free</i>.
     */
    public Lock() {
        setMyPriority();
    }

    public Lock(String name){
        setMyPriority();
        this.name = name;
    }

    /**
     * Atomically acquire this lock. The current thread must not already hold
     * this lock.
     */
    public void acquire() {
        Lib.assertTrue(!isHeldByCurrentThread());

        boolean intStatus = Machine.interrupt().disable();
        KThread thread = KThread.currentThread();

        if (lockHolder != null) {
            //add request edge
            thread.addEdge(this);
            //Log.write("W," + 
            //          getName() + "," +
            //          Log.getTimeInMillis() + "," +
            //          thread.getName() + "," + 
            //          ThreadedKernel.scheduler.getEffectivePriority(thread));

            waitQueue.waitForAccess(thread);
            KThread.sleep();
        }
        else {
            //add allocation edge
            addEdge(thread);
            //Log.write("A," + 
            //          getName() + "," +
            //          Log.getTimeInMillis() + "," +
            //          thread.getName() + "," + 
            //          ThreadedKernel.scheduler.getEffectivePriority(thread));
            waitQueue.acquire(thread);
            lockHolder = thread;
        }

        Lib.assertTrue(lockHolder == thread);

        Machine.interrupt().restore(intStatus);
    }

    /**
     * Atomically release this lock, allowing other threads to acquire it.
     */
    public void release() {
        Lib.assertTrue(isHeldByCurrentThread());

        boolean intStatus = Machine.interrupt().disable();
        
        KThread thread = KThread.currentThread();

        //remove allocation edge
        rmEdge(thread);
        //Log.write("R," + 
        //          getName() + "," +
        //          Log.getTimeInMillis() + "," +
        //          thread.getName() + "," + 
        //          ThreadedKernel.scheduler.getEffectivePriority(thread));

        if ((lockHolder = waitQueue.nextThread()) != null){
            //remove request edge
            lockHolder.rmEdge(this);
            //add allocation edge
            addEdge(lockHolder);
            //Log.write("A," + 
            //          getName() + "," +
            //          Log.getTimeInMillis() + "," +
            //          lockHolder.getName() + "," + 
            //          ThreadedKernel.scheduler.getEffectivePriority(lockHolder));
            
            lockHolder.ready();
        }

        Machine.interrupt().restore(intStatus);
    }

    /**
     * Test if the current thread holds this lock.
     *
     * @return	true if the current thread holds this lock.
     */
    public boolean isHeldByCurrentThread() {
        return (lockHolder == KThread.currentThread());
    }

    public String getName(){
        if(name == null)
            return toString();
        else
            return name;
    }

    protected int getMyPriority(){
        return Integer.parseInt(Config.getString("scheduler.maxPriorityValue"));
    }

    private String name;
    private KThread lockHolder = null;
    private Scheduler scheduler = new RoundRobinScheduler();
    private ThreadQueue waitQueue = scheduler.newThreadQueue(true);
}
