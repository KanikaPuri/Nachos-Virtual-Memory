package nachos.threads.test;

import nachos.machine.*;
import nachos.threads.*;

public class Scenario2 implements TestScheduler {
    public void test(){
        Lib.debug('t', "Enter KThread.selfTest");
        

        //disable interrupts because we need to set priorities of threads
        boolean intStatus = Machine.interrupt().disable();

        //create high priority thread
        KThread high = new KThread(new High(0, testLock2, shared), 0).setName("high");

        //create low priority thread
        int num= Integer.parseInt(Config.getString("Kernel.numThreads"));

        new KThread(new Low(2, testLock1, shared, high), 0).setName("low").fork();

        //create mid priority thread
        new KThread(new Medium(1, testLock1, testLock2, shared, high), 0).setName("medium").fork();

        high.fork();

        new KThread(new AlwaysRun(), 7).setName("always1").fork();

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
            boolean intStatus = Machine.interrupt().disable();
            KThread.currentThread().sleep();
            Machine.interrupt().restore(intStatus);

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
        Low(int which, Lock theLock, SharedInt data, KThread high) {
            this.which = which;
            this.theLock = theLock;
            this.high = high;
            this.data = data;
        }

        public void run() {

            //lower priority to low
            boolean intStatus = Machine.interrupt().disable();
            ThreadedKernel.scheduler.setPriority(KThread.currentThread(), 10);
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

            intStatus = Machine.interrupt().disable();
            //create another always ready thread with priority higher than
            //medium
            new KThread(new AlwaysRun(), 3).setName("always2").fork();

            //wake up high priority thread
            high.ready();

            KThread.currentThread().yield();
            Machine.interrupt().restore(intStatus);

            for(int i = 0; i < 5; i++){
                System.out.println("I still have Lock A");
                KThread.currentThread().yield();
            }

            //release the lock
            theLock.release();
        }
        
        private Lock theLock;
        private KThread high;
        private int which;
        private SharedInt data;
    }

    private static class Medium implements Runnable {
        Medium(int which, Lock theLock1, Lock theLock2, SharedInt data, KThread high) {
            this.which = which;
            this.theLock1 = theLock1;
            this.theLock2 = theLock2;
            this.data = data;
            this.high = high;
        }

        public void run() {

            //lower priority to medium
            boolean intStatus = Machine.interrupt().disable();
            ThreadedKernel.scheduler.setPriority(KThread.currentThread(), 5);
            Machine.interrupt().restore(intStatus);

            //acquire lock2
            theLock2.acquire();
            
            for(int i = 0; i < 5; i++){
                System.out.println("I have lock B!");
                KThread.currentThread().yield();
            }

            //acquire lock1
            theLock1.acquire();

            for (int i=0; i<5; i++) {
                
                //critical section
                data.myInt++;
                System.out.println("*** Medium: data = "
                        + data.myInt);
               
            KThread.currentThread().yield();
            }

            //release the lock
            theLock2.release();
        }
        
        private Lock theLock1;
        private Lock theLock2;
        private KThread high;
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

    private Lock testLock1 = new Lock("LockA");
    private Lock testLock2 = new Lock("LockB");
    private SharedInt shared = new SharedInt();
}   
   
