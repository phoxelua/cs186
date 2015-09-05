package simpledb;

import java.io.*;

import java.util.*;

import java.lang.*;
/**
 * BufferPool manages the reading and writing of pages into memory from
 * disk. Access methods call into it to retrieve pages, and it fetches
 * pages from the appropriate location.
 * <p>
 * The BufferPool is also responsible for locking;  when a transaction fetches
 * a page, BufferPool checks that the transaction has the appropriate
 * locks to read/write the page.
 */
public class BufferPool {
    /** Bytes per page, including header. */
    public static final int PAGE_SIZE = 4096; //4096

    /** Default number of pages passed to the constructor. This is used by
    other classes. BufferPool should use the numPages argument to the
    constructor instead. */
    public static final int DEFAULT_PAGES = 50; //50

    private HashMap<Integer,Page> pool; //worst design decision ever, should be PID --> Page.. 
    private int numPages;

    //queue to implemet LRU eviction, stores pid hashes
    //front(old)-----back(new)
    private LinkedList<Integer> recentQueue;  


    private LockManager lockManager;
    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    public BufferPool(int numPages) {
        // some code goes here
        pool = new HashMap<Integer, Page>(); //hm maybe shouldve done pid -> to page
        this.numPages = numPages;
        // recentQueue = new LinkedList<Integer>();
        lockManager = new LockManager();
    }

    /**
     * Retrieve the specified page with the associated permissions.
     * Will acquire a lock and may block if that lock is held by another
     * transaction.
     * <p>
     * The retrieved page should be looked up in the buffer pool.  If it
     * is present, it should be returned.  If it is not present, it should
     * be added to the buffer pool and returned.  If there is insufficient
     * space in the buffer pool, an page should be evicted and the new page
     * should be added in its place.
     *
     * @param tid the ID of the transaction requesting the page
     * @param pid the ID of the requested page
     * @param perm the requested permissions on the page
     */
    public synchronized Page getPage(TransactionId tid, PageId pid, Permissions perm)
        throws TransactionAbortedException, DbException {
        // some code goes here
        //Look in buffer pool >  present? return : try add to pool (if no space, be sad), return
        // System.out.println(pid == null);
        Page page = pool.get(pid.hashCode());

        //Pool doesn't have page, try fetch it
        if (page == null){//couldn't be found in bufferpool
            page = Database.getCatalog().getDbFile(pid.getTableId()).readPage(pid);
            if (pool.size() >= numPages) evictPage();
            pool.put(pid.hashCode(), page);
            // recentQueue.addLast(pid.hashCode()); 
        }
        //Pool has page, page to be returned must be moved to back of queue (new)
        // else{
        //     int index = recentQueue.indexOf(pid.hashCode()); //get first occurence of page
        //     if (index == -1) throw new NoSuchElementException("Queue not updating correctly!");
        //     recentQueue.addLast(recentQueue.get(index));
        //     recentQueue.remove(index);       
        // }
        //Handle locks 
        boolean gotLock = false;
        long startTime = System.currentTimeMillis(); 
        if (perm.toString().equals("READ_ONLY")){
            while(lockManager.exclusiveLocks.containsKey(pid) ){
                if (System.currentTimeMillis() - startTime > 700){
                    throw new TransactionAbortedException();
                }
                // try {
                if(lockManager.exclusiveLocks.containsKey(pid) && lockManager.exclusiveLocks.get(pid).equals(tid)){
                    break;
                }
                // catch (NullPointerException poop){
                //     System.out.println(lockManager.exclusiveLocks.containsKey(pid));
                //     System.out.println("hey");
                //     System.out.println(lockManager.exclusiveLocks.get(pid).equals(tid));    
                // }       
            }
            ArrayList<TransactionId> tids = new ArrayList<TransactionId>();
            if(lockManager.sharedLocks.contains(pid)) tids = lockManager.sharedLocks.get(pid);
            tids.add(tid);
            lockManager.sharedLocks.put(pid, tids);
        }
        if (perm.toString().equals("READ_WRITE")){
            gotLock = false; //just paranoia
            while(!gotLock){
                if (System.currentTimeMillis() - startTime > 700) {
                    throw new TransactionAbortedException();
                }                
                else if(lockManager.hasExclusiveLock(pid,tid)){
                    gotLock = true;
                }           
                else if(lockManager.isUpgradable(pid, tid)){
                    lockManager.sharedLocks.remove(pid);
                    gotLock = true;                
                }
                else if (!lockManager.sharedLocks.containsKey(pid) && !lockManager.exclusiveLocks.containsKey(pid)){
                    gotLock = true;                      
                }
            }
            lockManager.exclusiveLocks.put(pid,tid);
        }
        return page;     

    }

    // public int size(){
    //     return this.size();
    // }

    // public void printVals(){
    //     System.out.println("Priting rQ of size " + pool.size() + " : " + recentQueue);
    // }
    /**
     * Releases the lock on a page.
     * Calling this is very risky, and may result in wrong behavior. Think hard
     * about who needs to call this and why, and why they can run the risk of
     * calling it.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param pid the ID of the page to unlock
     */
    public  void releasePage(TransactionId tid, PageId pid) {
        // some code goes here
        // not necessary for proj1
        if (lockManager.hasExclusiveLock(pid, tid)) lockManager.exclusiveLocks.remove(pid);

        if(lockManager.sharedLocks.containsKey(pid)){
            if (lockManager.sharedLocks.get(pid).size() == 1) lockManager.sharedLocks.remove(pid);
            else{
                lockManager.sharedLocks.get(pid).removeAll(Collections.singleton(tid));
            }
        }
    }


    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public void transactionComplete(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for proj1
        transactionComplete(tid, true);
    }

    /** Return true if the specified transaction has a lock on the specified page */
    public boolean holdsLock(TransactionId tid, PageId p) {
        // some code goes here
        // not necessary for proj1
        return lockManager.hasSharedLock(p,tid) || lockManager.hasExclusiveLock(p, tid);
    }

    /**
     * Commit or abort a given transaction; release all locks associated to
     * the transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param commit a flag indicating whether we should commit or abort
     */
    public void transactionComplete(TransactionId tid, boolean commit)
        throws IOException {
        // some code goes here
        // not necessary for proj1

        //if commit -> flush all dirty pages associated with transaction to disk
        //if abort -> revert any changes made by the transaction by restoring the page to its on-disk state
        //Either way -> release any state the BufferPool keeps regarding the transaction
        // aka releasing any locks that the transaction held
        Page p;
        Catalog  catalog;


        // System.out.println("Before " + tid);
        // System.out.println(lockManager.sharedLocks.size());
        // System.out.println(lockManager.exclusiveLocks.size());

        if (commit){
            flushPages(tid);
        }
        else { //abort
            for(Integer key : pool.keySet()){
                p = pool.get(key);
                if (p.isDirty() != null && (p.isDirty()).equals(tid)){
                    catalog = Database.getCatalog();
                    p  = catalog.getDbFile(p.getId().getTableId()).readPage(p.getId());
                    pool.put(p.getId().hashCode(), p);
                }
            }
        }

        //Release all locks held by tid
        for (PageId pid : lockManager.sharedLocks.keySet()) {
            if (lockManager.sharedLocks.get(pid).contains(tid)) releasePage(tid, pid);
        }
        for (PageId pid : lockManager.exclusiveLocks.keySet()) {
            if (lockManager.exclusiveLocks.get(pid).equals(tid)) releasePage(tid, pid);
        }

        // System.out.println("After " + tid);
        // System.out.println(lockManager.sharedLocks.size());
        // System.out.println(lockManager.exclusiveLocks.size());

    }

    /**
     * Add a tuple to the specified table behalf of transaction tid.  Will
     * acquire a write lock on the page the tuple is added to(Lock 
     * acquisition is not needed for lab2). May block if the lock cannot 
     * be acquired.
     * 
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and updates cached versions of any pages that have 
     * been dirtied so that future requests see up-to-date pages. 
     *
     * @param tid the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t the tuple to add
     */
    public void insertTuple(TransactionId tid, int tableId, Tuple t)
        throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for proj1
        DbFile file = Database.getCatalog().getDbFile(tableId);
        synchronized(this){
        ArrayList<Page> filthyPages = file.insertTuple(tid, t);      
            for (Page p : filthyPages){
                p.markDirty(true, tid);
                pool.put(p.getId().hashCode(), p);
            }
        }
    }

    /**
     * Remove the specified tuple from the buffer pool.
     * Will acquire a write lock on the page the tuple is removed from. May block if
     * the lock cannot be acquired.
     *
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit.  Does not need to update cached versions of any pages that have 
     * been dirtied, as it is not possible that a new page was created during the deletion
     * (note difference from addTuple).
     *
     * @param tid the transaction adding the tuple.
     * @param t the tuple to add
     */
    public void deleteTuple(TransactionId tid, Tuple t)
        throws DbException, TransactionAbortedException {
        // some code goes here
        // not necessary for proj1
        DbFile file = Database.getCatalog().getDbFile(t.getRecordId().getPageId().getTableId());
        synchronized(this){
            Page page = file.deleteTuple(tid, t);
            page.markDirty(true,tid);
        }

        // System.out.println("DELETE FILTHY PAGE: " + page.getId().hashCode());        
        // System.out.println("Before: " + recentQueue); 

        // int index = recentQueue.indexOf(page.getId().hashCode()); //get first occurence of page
        // if (index == -1) throw new NoSuchElementException("Queue not updating correctly!");
        // recentQueue.addLast(recentQueue.get(index));
        // recentQueue.remove(index); 
        // System.out.println("After: " + recentQueue);    
    }

    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     *     break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
        // some code goes here
        // not necessary for proj1
        for (Integer key : pool.keySet()){
            Page page = pool.get(key);
            flushPage(page.getId());
        }

    }

    /** Remove the specific page id from the buffer pool.
        Needed by the recovery manager to ensure that the
        buffer pool doesn't keep a rolled back page in its
        cache.
    */
    public synchronized void discardPage(PageId pid) {
        // some code goes here
    // not necessary for proj1
    }

    /**
     * Flushes a certain page to disk
     * @param pid an ID indicating the page to flush
     */
    private synchronized  void flushPage(PageId pid) throws IOException {
        // some code goes here
        // not necessary for proj1
        if (!pool.containsKey(pid.hashCode())) return;
        DbFile file = Database.getCatalog().getDbFile(pid.getTableId());
        Page page = pool.get(pid.hashCode());
        if (page.isDirty() != null) { //null = clean page
            page.markDirty(false, null); 
            file.writePage(page);      
        }
    }

    /** Write all pages of the specified transaction to disk.
     */
    public synchronized  void flushPages(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for proj1
        for (Integer key : pool.keySet()){
            Page page = pool.get(key);
            if (page.isDirty() != null && (page.isDirty()).equals(tid)){
                flushPage(page.getId());
            }
        }
    }

    /**
     * Discards a page from the buffer pool.
     * Flushes the page to disk to ensure dirty pages are updated on disk.
     */
    private synchronized  void evictPage() throws DbException {
        // some code goes here
        // not necessary for proj1

        //instead of flushpages to disk, store in ds until commit- then flush
        // OR just flush first non-dirty page to disk...lol

        // try{
        //     int oldestPID = recentQueue.removeFirst(); //get and remove oldest pid hash code in queue
        //     Page oldestPage = pool.get(oldestPID);
        //     if (oldestPage.isDirty() != null){ //dirty
        //         flushPage(oldestPage.getId());
        //     }
        //     // System.out.println("-----EVICTION TIME. DIE :" + oldestPID);
        //     pool.remove(oldestPID);   
        // }
        // catch(IOException bad){
        //     bad.printStackTrace();
        // }

        boolean allDirty = true;
        Page firstCleanPage = null;
        try {
            for (Integer key : pool.keySet()){
                //if page is clean
                if (pool.get(key).isDirty() == null){
                    firstCleanPage = pool.get(key);
                    allDirty = false;
                    break;
                }
            }
            if (allDirty){
                throw new DbException("All pages are dirty! Commit pages!");
            }
            flushPage(firstCleanPage.getId());
            // System.out.println(pool.containsKey(firstCleanPage.getId()));
            pool.remove(firstCleanPage.getId().hashCode());
        }
        catch (IOException bad) {
            bad.printStackTrace();
        }
    }

}
