package simpledb;

import java.io.*;

import java.util.*;

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

    //public Page[] pool;
    private HashMap<Integer,Page> pool;
    private int numPages;

    //queue to implemet LRU eviction, stores pid hashes
    //front(old)-----back(new)
    private LinkedList<Integer> recentQueue; 

    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    public BufferPool(int numPages) {
        // some code goes here
	//pool = new Page[numPages-1];
    	pool = new HashMap<Integer, Page>(); //hm maybe shouldve done pid -> to page
    	this.numPages = numPages;
        recentQueue = new LinkedList<Integer>();
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
    public  Page getPage(TransactionId tid, PageId pid, Permissions perm)
        throws TransactionAbortedException, DbException {
        // some code goes here
    	//Look in buffer pool >  present? return : try add to pool (if no space, be sad), return
    	Page page = pool.get(pid.hashCode());

        //Pool doesn't have page, try fetch it
    	if (page == null){//couldn't be found in bufferpool
            // try{
                page = Database.getCatalog().getDbFile(pid.getTableId()).readPage(pid);
            // }
            // catch(NoSuchElementException bad){
            //     throw new DbException("No such file in catalog!");
            // }
    		if (pool.size() >= numPages) evictPage(); //throw new DbException("No more space!");
    		pool.put(pid.hashCode(), page);
            recentQueue.addLast(pid.hashCode()); 
            // System.out.println(page == null);
    	}
        //Pool has page, page to be returned must be moved to back of queue (new)
        else{
            int index = recentQueue.indexOf(pid.hashCode()); //get first occurence of page
            if (index == -1) throw new NoSuchElementException("Queue not updating correctly!");
            recentQueue.addLast(recentQueue.get(index));
            recentQueue.remove(index);       
        }
        // System.out.println(pool.size());
        // this.printVals();
        return page;     

    }

    public int size(){
        return this.size();
    }

    public void printVals(){
        System.out.println("Priting rQ of size " + pool.size() + " : " + recentQueue);
        // System.out.println(recentQueue);

    }
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
    }

    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public void transactionComplete(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for proj1
    }

    /** Return true if the specified transaction has a lock on the specified page */
    public boolean holdsLock(TransactionId tid, PageId p) {
        // some code goes here
        // not necessary for proj1
        return false;
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
        ArrayList<Page> filthyPages = file.insertTuple(tid, t);

        // Random r = new Random();
        // List<Integer> keys = new ArrayList<Integer>(pool.keySet());
        // int random = keys.get(r.nextInt(keys.size()));

        // random = (int) pool.keySet().iterator().next();

        // System.out.println("INSERT: FILTHY PAGES: " + random);
        // System.out.println("Before: " + recentQueue);        
        for (Page p : filthyPages){
            p.markDirty(true, tid);
            pool.put(p.getId().hashCode(), p);

            int index = recentQueue.indexOf(p.getId().hashCode()); //get first occurence of page
            if (index == -1) throw new NoSuchElementException("Queue not updating correctly!");
            recentQueue.addLast(recentQueue.get(index));
            recentQueue.remove(index); 
        }
        // System.out.println("After:  " + recentQueue);        

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
    public  void deleteTuple(TransactionId tid, Tuple t)
        throws DbException, TransactionAbortedException {
        // some code goes here
        // not necessary for proj1
        DbFile file = Database.getCatalog().getDbFile(t.getRecordId().getPageId().getTableId());
        Page page = file.deleteTuple(tid, t);
        page.markDirty(true,tid);

        // System.out.println("DELETE FILTHY PAGE: " + page.getId().hashCode());        
        // System.out.println("Before: " + recentQueue); 

        int index = recentQueue.indexOf(page.getId().hashCode()); //get first occurence of page
        if (index == -1) throw new NoSuchElementException("Queue not updating correctly!");
        recentQueue.addLast(recentQueue.get(index));
        recentQueue.remove(index); 
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
        // for (Integer key : pool.keySet()){
        //     Page page = pool.get(key);
        //     if (page.isDirty() != null && page.isDirty().equals(tid)){
        //         flushPage(page.getId());
        //     }
        // }
    }

    /**
     * Discards a page from the buffer pool.
     * Flushes the page to disk to ensure dirty pages are updated on disk.
     */
    private synchronized  void evictPage() throws DbException {
        // some code goes here
        // not necessary for proj1
        try{
            int oldestPID = recentQueue.removeFirst(); //get and remove oldest pid hash code in queue
            Page oldestPage = pool.get(oldestPID);
            if (oldestPage.isDirty() != null){ //dirty
                flushPage(oldestPage.getId());
            }
            // System.out.println("-----EVICTION TIME. DIE :" + oldestPID);
            pool.remove(oldestPID);   
        }
        catch(IOException bad){
            bad.printStackTrace();
        }


    }

}
