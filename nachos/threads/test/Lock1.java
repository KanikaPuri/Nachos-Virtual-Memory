package nachos.threads.test;

import nachos.machine.*;
import nachos.threads.*;

public class Lock1 implements TestScheduler {
    public void test(){
        Lib.debug('t', "Enter KThread.selfTest");
        

        //disable interrupts because we need to set priorities of threads
        boolean intStatus = Machine.interrupt().disable();

        //create threads
        int num= Integer.parseInt(Config.getString("Kernel.numThreads"));
        for(int i=0; i<num; i++){
            new KThread(new Test(i, testLock, shared), i).setName("t"+i).fork();
        }

        //set main thread to lowest priority
        ThreadedKernel.scheduler.setPriority(KThread.currentThread(),
                StaticPriorityScheduler.priorityMaximum);
       
        //restore interrupt state
        Machine.interrupt().restore(intStatus);
        
        //yield main thread to let others run
        KThread.currentThread().yield();

        ThreadedKernel.scheduler.printSystemStats();
    }

    private static class Test implements Runnable {
        Test(int which, Lock theLock, SharedInt data) {
            this.which = which;
            this.theLock = theLock;
            this.data = data;
        }

        public void run() {
            for (int i=0; i<5; i++) {
                //acquire lock
                theLock.acquire();
                
                //critical section
                data.myInt++;
                System.out.println("*** thread " + which + ": data = "
                        + data.myInt);

                //release the lock
                theLock.release();

                KThread.currentThread().yield();
            }
        }
        
        private Lock theLock;
        private int which;
        private SharedInt data;
    }

    private static class SharedInt {
        SharedInt(){
            myInt = 0;
        }

        SharedInt(int x){
            myInt = x;
        }

        public int myInt;
    }

    private Lock testLock = new Lock();
    private SharedInt shared = new SharedInt();
}   
   
