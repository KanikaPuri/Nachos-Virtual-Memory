package nachos.threads.test;

import nachos.machine.*;
import nachos.threads.*;

public class Lock2Inversion implements TestScheduler {
    public void test(){
        Lib.debug('t', "Enter KThread.selfTest");
        

        //disable interrupts because we need to set priorities of threads
        boolean intStatus = Machine.interrupt().disable();

        //create low priority lock hog (for inversion)
        int num= Integer.parseInt(Config.getString("Kernel.numThreads"));

        new KThread(new TestHog(num - 1, testLock, shared), 0).setName("thog").fork();
        KThread.currentThread().yield();

        //create always runnable mid priority thread
        new KThread(new AlwaysRun(), num - 2).setName("talways").fork();

        //create high priority thread
        for(int i = 0; i < num - 2; i++){
            new KThread(new Test(i, testLock, shared), i).setName("t" + i).fork();
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

    private static class TestHog implements Runnable {
        TestHog(int which, Lock theLock, SharedInt data) {
            this.which = which;
            this.theLock = theLock;
            this.data = data;
        }

        public void run() {

            //lower priority to minimum
            boolean intStatus = Machine.interrupt().disable();
            int num= Integer.parseInt(Config.getString("Kernel.numThreads"));
            ThreadedKernel.scheduler.setPriority(KThread.currentThread(), num - 1);
            Machine.interrupt().restore(intStatus);

            for (int i=0; i<5; i++) {
                //acquire lock
                theLock.acquire();
                
                //critical section
                data.myInt++;
                System.out.println("*** lock hog " + which + ": data = "
                        + data.myInt);
               
                //yield before releasing lock
                KThread.currentThread().yield();

                //release the lock
                theLock.release();

                KThread.currentThread().yield();
            }
        }
        
        private Lock theLock;
        private int which;
        private SharedInt data;
    }

    private static class AlwaysRun implements Runnable {
        public void run(){
            for(int i = 0; i < 100; i++){
                System.out.println("*** Always Run");
                KThread.currentThread().yield();
            }
        }
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
   
