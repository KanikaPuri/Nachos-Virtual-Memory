package nachos.userprog;

import java.util.LinkedList;
import java.util.Iterator;
import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
	super();
    }

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    public void initialize(String[] args) {
	super.initialize(args);

	console = new SynchConsole(Machine.console());
	
	Machine.processor().setExceptionHandler(new Runnable() {
		public void run() { exceptionHandler(); }
	    });
    }

    /**
     * Test the console device.
     */	
    public void selfTest() {
	super.selfTest();

	System.out.println("");
    }

    /**
     * Returns the current process.
     *
     * @return	the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
	if (!(KThread.currentThread() instanceof UThread))
	    return null;
	
	return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
	Lib.assertTrue(KThread.currentThread() instanceof UThread);

	UserProcess process = ((UThread) KThread.currentThread()).process;
	int cause = Machine.processor().readRegister(Processor.regCause);
	process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see	nachos.machine.Machine#getShellProgramName
     */
    public void run() {
	super.run();

    //set main tread priority to high priority
    boolean intStatus = Machine.interrupt().disable();
    UserKernel.scheduler.setPriority(KThread.currentThread(), 0);
    Machine.interrupt().restore(intStatus);
	UserProcess process; 

	String shellPrograms = Machine.getShellProgramName();

    int numProgs = Integer.parseInt(shellPrograms.substring(0, shellPrograms.indexOf(':')));

	String[] progs = shellPrograms.substring(shellPrograms.indexOf(':') + 1).split(",");

    for(int i = 0; i < progs.length; i++){
        for(int j = 0; j < numProgs; j++){
            process = UserProcess.newUserProcess();
	        if(process.execute(progs[i], new String[] {}) == false){
                if(process.getNumPages() <= Machine.processor().getNumPhysPages()){
                    memWait.add(process);
                }
                else{
                    System.out.println(process.getName() + ",reject," + process.getNumPages());
                }
            }
        }
    }

    while(!memWait.isEmpty()){
        allocator.waiting = true;
        allocator.waitSem.P();

        Iterator iter = memWait.iterator();
        while(iter.hasNext()){
            process = (UserProcess) iter.next();
            if(process.getNumPages() <= allocator.getAvailFrames()){
                iter.remove();
	            process.execute(new String(), new String[] {});
            }                
        }
    }

    //set main tread priority to low priority
    intStatus = Machine.interrupt().disable();
    UserKernel.scheduler.setPriority(KThread.currentThread(), 10);
    Machine.interrupt().restore(intStatus);

    //yield so that other threads get to run. Since this is now lowest priority
    //thread it will not be scheduled again until all other threads have
    //completed
    KThread.currentThread().yield();

    System.out.println();
    System.out.println("Max running processes: " + allocator.getMaxNumProc());
    System.out.println("Max frames reserved:   " + allocator.getMaxNumReserved());
    System.out.println("Max frames mapped:     " + allocator.getMaxNumMapped());
    
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }

    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;

    public static Allocator allocator = new Allocator();
    private LinkedList memWait = new LinkedList<UserProcess>();

    // dummy variables to make javac smarter
    private static Coff dummy1 = null;
}
