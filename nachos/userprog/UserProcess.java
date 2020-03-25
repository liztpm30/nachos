package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.LinkedList;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
	
	
	
    /**
     * Allocate a new process.
     */
    public UserProcess(){
    	
    	
    	//Creating file descriptors
    	for (int i=0; i<MAXFD; i++) {                               
            fds[i] = new FileDescriptor();                                 
       }
   
		int numPhysPages = Machine.processor().getNumPhysPages();
		pageTable = new TranslationEntry[numPhysPages];
		for (int i=0; i<numPhysPages; i++)
		    pageTable[i] = new TranslationEntry(i,i, true,false,false,false);
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	
	new UThread(this).setName(name).fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	
    	Lib.assertTrue(maxLength >= 0);

    	byte[] bytes = new byte[maxLength+1];

    	int bytesRead = readVirtualMemory(vaddr, bytes);

    	for (int length=0; length<bytesRead; length++) {
    	    if (bytes[length] == 0)
    		return new String(bytes, 0, length);
    	}

    	return null;

    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
    	Lib.assertTrue(length >= 0);
    	
    	Processor pro = Machine.processor();

		byte[] bytes = pro.getMemory();
	
		int bytesRead = readVirtualMemory(vaddr, bytes);
		
	       
	    int virtualpn = pro.pageFromAddress(vaddr);                            
	    int addressOffset = pro.offsetFromAddress(vaddr);  
	    
	    if (virtualpn >= numPages) {          
	        return -1;        
	    }                                                                    

		TranslationEntry entry = null;                            
	    entry = pageTable[virtualpn];                          

	    if (pageTable[virtualpn].valid == false) {
	        return -1;                                                         
	    }                           

		entry.used = true;                                     

	    int ppn = entry.ppn;             
		int paddr = (ppn*pageSize) + addressOffset;                       
	    Lib.debug(dbgProcess,                                     
	                "[UserProcess.readVirtualMemory] ppn " + ppn   
	                + ",paddr: " + paddr);                          

	    if (ppn < 0 || ppn >= pro.getNumPhysPages())  {                
	        Lib.debug(dbgProcess,                                           
	                "\t\t UserProcess.readVirtualMemory(): bad ppn " + ppn);  
	        return 0;                                                        
	    }             

		int amount = Math.min(length, bytes.length-paddr);
		System.arraycopy(bytes, paddr, data, offset, amount);

		return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
    	
    	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

        Processor pro = Machine.processor();  
    	byte[] bytes = Machine.processor().getMemory();
        int virtualpn = pro.pageFromAddress(vaddr);                  
        int addressOffset = pro.offsetFromAddress(vaddr);

        
        if (virtualpn >= numPages) {    
            return -1;                                    
        }           

    	TranslationEntry te = null;
        te = pageTable[virtualpn];                  

        if (te.readOnly) { 
            return -1;                                      
        }                                                           

    	te.used = true;                                        
    	te.dirty = true;                                  
        int ppn = te.ppn;                                
    	int paddr = (ppn * pageSize) + addressOffset;         

        if (ppn < 0 || ppn >= pro.getNumPhysPages())  {  
            return 0;                                           
        }                                                

    	int amount = Math.min(length, bytes.length - paddr);
    	System.arraycopy(data, offset, bytes, paddr, amount); 

    	return amount;
        }


    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;
	
	/* Initializing the user page table */
    pageTable = new TranslationEntry [numPages];
    int freePage;
    
    for (int i=0; i<numPages; i++){
    	freePage = UserKernel.getFreePage();
    	pageTable[i] = new TranslationEntry(i,freePage,true,false,false,false);
    }
    

    pageTable = new TranslationEntry[numPages];       
    for (int i = 0; i < numPages; i++) {                          
        int ppn = UserKernel.getFreePage();               
        pageTable[i] =  new TranslationEntry(i, ppn, true, false, false, false);
    }       
	

	if (!loadSections())
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
	if (numPages > Machine.processor().getNumPhysPages()) {
	    coff.close();
	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
	    return false;
	}

	// load sections
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    
	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
		      + " section (" + section.getLength() + " pages)");

	    for (int i=0; i<section.getLength(); i++) {
		int vpn = section.getFirstVPN()+i;
		
		TranslationEntry entry = pageTable[vpn];
        entry.readOnly = section.isReadOnly();                             

        int ppn = entry.ppn;                                                  

		section.loadPage(i, ppn);
	    }
	}
	
	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
    	
	    coff.close();                                                        

        for (int i = 0; i < numPages; i++) {                         
            UserKernel.addFreePage(pageTable[i].ppn);        
            pageTable[i] = null;                        
        }                                   

        pageTable = null;    

    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {

	Machine.halt();
	
	Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }
    
    private int handleCreate (int arg){
    	
    	//Debugging
    	Lib.debug(dbgProcess, "handleCreate()");
    	
    	//Looking for the file in memory
    	String filename = readVirtualMemoryString(arg, MAXSTRLEN);
    	
    	//Debugging
    	Lib.debug(dbgProcess, "filename: "+filename);
    	
    	//Open file
    	OpenFile fvar = UserKernel.fileSystem.open(filename, true);
    	
    	 if (fvar == null) {                                           
             return -1;                                                     
         }                                                                  
         else {                                                            
             int fileHandle = findEmptyFileDescriptor();                    
             if (fileHandle < 0)                                           
                 return -1;                                                  
             else {                                                         
                 fds[fileHandle].filename = filename;                       
                 fds[fileHandle].file = fvar;                             
                 fds[fileHandle].position = 0;                              
                 return fileHandle;                                         
             }                                                               
         } 
    	
    }
    
    private int handleOpen (int arg){

    	//Looking for the file in memory
        String filename = readVirtualMemoryString(arg, MAXSTRLEN);         

	    Lib.debug(dbgProcess, "Opening file: "+filename);                     

        // Opening the file
        OpenFile file  = UserKernel.fileSystem.open(filename, false);    

        if (file == null) {                                              
            return -1;                                                     
        }                                                                 
        else {                                                            
            int fileHandle = findEmptyFileDescriptor();                     
            if (fileHandle < 0)                                            
                return -1;                                                 
            else {                                                         
                fds[fileHandle].filename = filename;                       
                fds[fileHandle].file = file;                             
                fds[fileHandle].position = 0;                              
                return fileHandle;                                         
            }                                                              
        }                                                                 
    }
    
    private int handleRead (int arg1, int arg2, int arg3){
    	
    	Lib.debug(dbgProcess, "handleRead()");                            
        
        int handle = arg1;                   
        int vaddr = arg2;              
        int bufsize = arg3;                                         

        if (handle < 0 || handle > MAXFD                                  
                || fds[handle].file == null)                              
            return -1;                                                    

        FileDescriptor fd = fds[handle];                                  
        byte[] buf = new byte[bufsize];                                   

        // read file
        int fvar = fd.file.read(fd.position, buf, 0, bufsize);          

        if (fvar < 0) {                                                 
            return -1;                                                    
        }                                                                 
        else {                                                            
            int number = writeVirtualMemory(vaddr, buf);                  
            fd.position = fd.position + number;                           
            return fvar;                                                
        }  
    }
    
    private int handleWrite (int arg1, int arg2, int arg3){
        
        int handle = arg1;    
        int vaddr = arg2;            
        int bufsize = arg3;                

        if (handle < 0 || handle > MAXFD                        
                || fds[handle].file == null)                  
            return -1;                                               

        FileDescriptor fd = fds[handle];                           

        byte[] buf = new byte[bufsize];                         

        int bytesRead = readVirtualMemory(vaddr, buf);            

        // write file                   
        int fvar = fd.file.write(fd.position, buf, 0, bytesRead);

        if (fvar < 0) {                                            
            return -1;                                           
        }          
        else {                                          
            fd.position = fd.position + fvar;               
            return fvar;     
        } 
    }

    private int handleClose (int arg){
        
  
        if (arg < 0 || arg >= MAXFD)                                   
            return -1;                    

        boolean close = true;       

        FileDescriptor fd = fds[arg];      

        fd.position = 0;                             
        fd.file.close();                    
                  
        if (fd.toRemove) {                                   
            close = UserKernel.fileSystem.remove(fd.filename);    
            fd.toRemove = false;                                   
        }                                                    

        fd.filename = "";                                   

        return close ? 0 : -1;
    }
    
    private int handleUnlink (int arg){

        boolean unlinked = true;
        int file;

        // read file in virtual memory
        String filename = readVirtualMemoryString(arg, MAXSTRLEN);      

	    Lib.debug(dbgProcess, "filename: " + filename);
	
	    
	    for (int i = 0; i < MAXFD; i++) {                 
            if (fds[i].filename == filename)        
            	file = i;                             
        }                                          

	    file = -1;
	    
        if (file < 0) {                                   

        	unlinked = UserKernel.fileSystem.remove(fds[file].filename); 
        }                                                                     
        else { 
        	
             fds[file].toRemove = true;                             
        }                                        

        return unlinked ? 0 : -1;  
    }

    private static final int
    
    syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	switch (syscall) {
	case syscallHalt:
	    return handleHalt();
	case syscallCreate:
		return handleCreate(a0);
	case syscallOpen:
		return handleOpen(a0);
	case syscallRead:
		return handleRead(a0, a1, a2);
	case syscallWrite:
		return handleWrite(a0, a1, a2);
	case syscallClose:
		return handleClose(a0);
	case syscallUnlink:
		return handleUnlink(a0);

	default:
	    Lib.debug(dbgProcess, "Unknown syscall " + syscall);
	    Lib.assertNotReached("Unknown system call!");
	}
	return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
				       
	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
	    Lib.assertNotReached("Unexpected exception");
	}
    }
    
    private int findEmptyFileDescriptor() {              
        for (int i = 0; i < 16; i++) {                  
            if (fds[i].file == null)                       
                return i;                                  
        }                                                  

        return -1;                                          
    }  

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    
    
    public class FileDescriptor {                                 
        public FileDescriptor() {                                 
        }                                                         
        private  String   filename = "";   // opened file name    
        private  OpenFile file = null;     // opened file object  
        private  int      position = 0;    // IO position        

        private  boolean  toRemove = false;// if need to remove   
                                           // this file           
                                            
    }                                                            
    /* maximum number of opened files per process                       */
    public static final int MAXFD = 16;                           

    /* standard input file descriptor                                   */
    public static final int STDIN = 0;                             

    /* standard output file descriptor                                  */
    public static final int STDOUT = 1;                           

    /* maximum length of strings passed as arguments to system calls    */
    public static final int MAXSTRLEN = 256;                        

    /* pid of root process(first user process)                          */
    public static final int ROOT = 1;                               

    /* file descriptors per process                                     */
    private FileDescriptor fds[] = new FileDescriptor[MAXFD];        

    /* number of opened files                                           */
    private int cntOpenedFiles = 0;                               

    /* process ID                                                       */
    private int pid;                                              

    /* parent process's ID                                              */
    private int ppid;                                             

    /* child processes                                                  */
    private LinkedList<Integer> children                          
                   = new LinkedList<Integer>();                   

    /* exit status                                                      */
    private int exitStatus;                                       

    /* user thread that's associated with this process                  */
    private UThread thread;  
                                   
}
