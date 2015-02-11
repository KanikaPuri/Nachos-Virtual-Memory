package nachos.threads.test;

import nachos.machine.*;
import nachos.threads.*;

public class SPT3 implements TestScheduler {
    public void test(){
        Lib.debug('t', "Enter KThread.selfTest");
        

        //disable interrupts because we need to set priorities of threads
        boolean intStatus = Machine.interrupt().disable();

        //make a high priorty thread
        new KThread(new PingTest(0), 0).setName("t0").fork();

        //make a low priority thread
        new KThread(new PingTest(1), StaticPriorityScheduler.priorityMaximum -
        1).setName("t1").fork();

        //create other regular priority threads
        int num= Integer.parseInt(Config.getString("Kernel.numThreads"));
        for(int i=2; i<num; i++){
            new KThread(new PingTest(i)).setName("t"+i).fork();
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

    private static class PingTest implements Runnable {
        PingTest(int which) {
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
}   
