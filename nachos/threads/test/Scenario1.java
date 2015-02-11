package nachos.threads.test;

import nachos.machine.*;
import nachos.threads.*;

public class Scenario1 implements TestScheduler {
    public void test(){
        Lib.debug('t', "Enter KThread.selfTest");
        

        //disable interrupts because we need to set priorities of threads
        boolean intStatus = Machine.interrupt().disable();

        //create low priority thread
        int num= Integer.parseInt(Config.getString("Kernel.numThreads"));

        new KThread(new Low(2, testLock, shared), 0).setName("low").fork();

        //create mid priority thread
        new KThread(new Medium(1, testLock, shared), 0).setName("medium").fork();

        //create high priority thread
        new KThread(new High(0, testLock, shared), 0).setName("high").fork();

        new KThread(new AlwaysRun(), 1).setName("always").fork();

        //set main thread to lowest priority
        ThreadedKernel.scheduler.setPriority(KThread.currentThread(),
            StaticPriorityScheduler.priorityMaximum);
       
        //restore interrupt state
        Machine.interrupt().restore(intStatus);
        
        //yield main thread to let others run
        KThread.currentThread().yield();

        ThreadedKernel.scheduler.printSystemStats();
    }

    private static class High implements Runnable {
        High(int which, Lock theLock, SharedInt data) {
            this.which = which;
            this.theLock = theLock;
            this.data = data;
        }

        public void run() {
            //acquire lock
            theLock.acquire();

            for (int i=0; i<5; i++) {
                //critical section
                data.myInt++;
                System.out.println("*** High: data = "
                        + data.myInt);

                KThread.currentThread().yield();
            }
            //release the lock
            theLock.release();
        }
        
        private Lock theLock;
        private int which;
        private SharedInt data;
    }

    private static class Low implements Runnable {
        Low(int which, Lock theLock, SharedInt data) {
            this.which = which;
            this.theLock = theLock;
            this.data = data;
        }

        public void run() {

            //lower priority to low
            boolean intStatus = Machine.interrupt().disable();
            ThreadedKernel.scheduler.setPriority(KThread.currentThread(), 2);
            Machine.interrupt().restore(intStatus);

            //acquire lock
            theLock.acquire();
            KThread.currentThread().yield();

            for (int i=0; i<5; i++) {
                //critical section
                data.myInt++;
                System.out.println("*** Low: data = "
                        + data.myInt);
               
            KThread.currentThread().yield();
            }

            //release the lock
            theLock.release();
        }
        
        private Lock theLock;
        private int which;
        private SharedInt data;
    }

    private static class Medium implements Runnable {
        Medium(int which, Lock theLock, SharedInt data) {
            this.which = which;
            this.theLock = theLock;
            this.data = data;
        }

        public void run() {

            //lower priority to medium
            boolean intStatus = Machine.interrupt().disable();
            ThreadedKernel.scheduler.setPriority(KThread.currentThread(), 1);
            Machine.interrupt().restore(intStatus);

            //acquire lock
            theLock.acquire();
            KThread.currentThread().yield();

            for (int i=0; i<5; i++) {
                
                //critical section
                data.myInt++;
                System.out.println("*** Medium: data = "
                        + data.myInt);
               
            KThread.currentThread().yield();
            }

            //release the lock
            theLock.release();
        }
        
        private Lock theLock;
        private int which;
        private SharedInt data;
    }

    private static class AlwaysRun implements Runnable {
        public void run(){
            for(int i = 0; i < 10; i++){
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
   
