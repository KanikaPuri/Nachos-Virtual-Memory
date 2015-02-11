package nachos.threads.test;

import nachos.machine.*;
import nachos.threads.*;

public class SPT2 implements TestScheduler {
    public void test(){
        Lib.debug('t', "Enter KThread.selfTest");
        

        //disable interrupts because we need to set priorities of threads
        boolean intStatus = Machine.interrupt().disable();

        //create threads
        int num= Integer.parseInt(Config.getString("Kernel.numThreads"));
        for(int i=0; i<num; i++){
            new KThread(new Test(i), StaticPriorityScheduler.priorityMaximum - 1
            - i).setName("t"+i).fork();
        }

        //set main thread to lowest priority
        ThreadedKernel.scheduler.setPriority(KThread.currentThread(),
                StaticPriorityScheduler.priorityMaximum);
       
        //restore interrupt state
        Machine.interrupt().restore(intStatus);
        
        //yield main thread to let others run
        //for(int i = 0; i < 100; i++)
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
}   
