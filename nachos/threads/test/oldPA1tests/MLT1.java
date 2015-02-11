package nachos.threads.test;

import nachos.machine.*;
import nachos.threads.*;

public class MLT1 implements TestScheduler {
    public void test(){
        Lib.debug('t', "Enter KThread.selfTest");
        

        //disable interrupts because we need to set priorities of threads
        boolean intStatus = Machine.interrupt().disable();

        //create threads in top queue
        int num= Integer.parseInt(Config.getString("Kernel.numThreads"));
        for(int i=0; i<5; i++){
            new KThread(new LongTest(i), i+1).setName("t"+i).fork();
        }

        //create threads in middle queue
        for(int i=5; i<10; i++){
            new KThread(new LongTest(i), i+10).setName("t"+i).fork();
        }

        //create threads in bottom queue
        for(int i=10; i<15; i++){
            new KThread(new LongTest(i), i+15).setName("t"+i).fork();
        }

        //set main thread to lowest priority
        ThreadedKernel.scheduler.setPriority(KThread.currentThread(),
                DynamicPriorityScheduler.priorityMaximum);
       
        //restore interrupt state
        Machine.interrupt().restore(intStatus);
        
        //yield main thread to let others run
        for(int i = 0; i < 100; i++)
        KThread.currentThread().yield();

        ThreadedKernel.scheduler.printSystemStats();
    }

    private static class Test implements Runnable {
        Test(int which) {
            this.which = which;
        }

        public void run() {
            for (int i=0; i<5; i++) {
                System.out.println("*** thread " + which + " looped "
                        + i + " times");
                KThread.currentThread().yield();
            }
        }

        private int which;
    }

    private static class LongTest implements Runnable {
        LongTest(int which) {
            this.which = which;
        }

        public void run() {
            for (int i=0; i<10; i++) {
                for(int j = 0; j < 10000000; j++){
                    int kasdf = 100+j;
                }
                System.out.println("*** thread " + which + " looped "
                        + i + " times");
                KThread.currentThread().yield();
            }
        }

        private int which;
    }
}   
   
