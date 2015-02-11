package nachos.userprog;

import java.util.LinkedList;
import nachos.machine.*;
import nachos.threads.Semaphore;

public class Allocator{

    public Allocator(){
        frames = new FrameInfo[numPages];

        for(int i = 0; i < numPages; i++){
            frames[i] = new FrameInfo(i);
            freeList.add(frames[i]);
        }
    }
    
    //class to hold info for frames
    private class FrameInfo{

        FrameInfo(){
            frameNum = -1;
            pid = -1;
        }

        FrameInfo(int num){
            frameNum = num;
            pid = -1;
        }

        public int pid; //pid of process this frame is allocated to (-1 is invalid)
        public final int frameNum; //phys page number
    }

    //finds an unused frame and allocates it to process pid
    public int allocateFrame(int pid){
        if(freeList.isEmpty())
            return -1;

        FrameInfo f = (FrameInfo) freeList.remove();
        f.pid = pid;

        //one more frame is mapped;
        numMapped++;

        if(numMapped > maxNumMapped)
            maxNumMapped = numMapped;

        return f.frameNum;
    }

    //frees an individual frame
    public void freeFrame(int fnum){
        if(frames[fnum].pid == -1)
            return;

        frames[fnum].pid = -1;
        freeList.add(frames[fnum]);

        //one less frame is mapped
        numMapped--;
    }

    //frees all the frames for process pid
    public void freeProcessFrames(int pid){
        int count = 0;

        for(int i = 0; i < numPages; i++){
            if(frames[i].pid == pid){
                frames[i].pid = -1;
                freeList.add(frames[i]);
                count++;
            }
        }

        //now have count less frames mapped
        numMapped -= count;
    }

    //keep track of the number of reserved frames
    public int getAvailFrames(){
        return availableFrames;
    }

    public void reserve(int n){
        availableFrames -= n;
        numReserved += n;
        numProc++;

        if(numReserved > maxNumReserved)
            maxNumReserved = numReserved;
        if(numProc > maxNumProc)
            maxNumProc = numProc;
    }

    public void unreserve(int n){
        availableFrames += n;
        numReserved -= n;
        numProc--;
    }

    //get funcs for the statistics
    int getMaxNumProc(){
        return maxNumProc;
    }

    int getMaxNumMapped(){
        return maxNumMapped;
    }

    int getMaxNumReserved(){
        return maxNumReserved;
    }

    private FrameInfo[] frames;
    private LinkedList freeList = new LinkedList<FrameInfo>();
    private final int numPages = Machine.processor().getNumPhysPages();
    public Semaphore waitSem = new Semaphore(0);
    public boolean waiting = false;

    //performance metrics
    private int numProc = 0;
    private int numMapped = 0;
    private int numReserved = 0;
    private int availableFrames = Machine.processor().getNumPhysPages();

    private int maxNumReserved = 0;
    private int maxNumProc = 0;
    private int maxNumMapped = 0;
}
