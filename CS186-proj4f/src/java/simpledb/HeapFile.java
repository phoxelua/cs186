package simpledb;

import java.io.*;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 * 
 * @see simpledb.HeapPage#HeapPage
 * @author Sam Madden
 */
public class HeapFile implements DbFile {

    private File file;
    private TupleDesc td;

    /**
     * Constructs a heap file backed by the specified file.
     * 
     * @param f
     *            the file that stores the on-disk backing store for this heap
     *            file.
     */
    public HeapFile(File f, TupleDesc td) {
        // some code goes here
        this.file = f;
        this.td = td;
    }

    /**
     * Returns the File backing this HeapFile on disk.
     * 
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        // some code goes here
        return file;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     * 
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        // some code goes here
        return file.getAbsoluteFile().hashCode();
        // throw new UnsupportedOperationException("implement this");
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     * 
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        return td;
        // throw new UnsupportedOperationException("implement this");
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) {
        // some code goes here
        //put file into RAF format -> find data in RAF --> put into heap page constructor 
        HeapPage page;
        byte[] data = new byte[BufferPool.PAGE_SIZE];
        try {
            RandomAccessFile raf = new RandomAccessFile(getFile(), "r");
            raf.seek(pid.pageNumber()*BufferPool.PAGE_SIZE); //set pointer to page
            raf.read(data); //read file into data byte array
            page = new HeapPage((HeapPageId)pid, data);
            raf.close();
        }
        catch (FileNotFoundException bad){
            System.out.println("File not found");
            return null;
        }
        catch (IOException bad){
            System.out.println("RAF seek or read failure");
            return null;
        }
        return page;
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        // some code goes here
        // not necessary for proj1
        byte[] data = page.getPageData();
        try{
            RandomAccessFile raf = new RandomAccessFile(getFile(), "rw");
            raf.seek(page.getId().pageNumber()*BufferPool.PAGE_SIZE);
            raf.write(data);
            raf.close();
        }
        catch (FileNotFoundException bad){
            System.out.println("File not found");
            bad.printStackTrace();
        }
        catch (IOException bad){
            System.out.println("RAF seek or read failure");
            bad.printStackTrace();
        }
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        // some code goes here
        return (int) Math.ceil(file.length() / (double) BufferPool.PAGE_SIZE);
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        if (t == null) throw new DbException("Tuple is null!");
        BufferPool bufferpool = Database.getBufferPool();
        ArrayList<Page> freePages = new ArrayList<Page>(); //should be called dirtyPages
        //Get all pages that have free space
        for (int i = 0; i < this.numPages(); i++){
            HeapPage currPage = (HeapPage) bufferpool.getPage(tid, (PageId) new HeapPageId(getId(), i), Permissions.READ_WRITE);
            if (currPage.getNumEmptySlots() > 0){
                freePages.add(currPage);
            }
        }

        for (Page p : freePages){
            try{
                ((HeapPage) p ).insertTuple(t);
                break;
            }
            catch(Exception e){
                //do nothing- continue trying
            }
        }

        //Nore more space - create new page
        if (freePages.isEmpty()){
            try{
                PageId pid = new HeapPageId(getId(), numPages());
                byte[] b = HeapPage.createEmptyPageData();
                RandomAccessFile raf = new RandomAccessFile(getFile(), "rw");
                raf.seek(pid.pageNumber()*BufferPool.PAGE_SIZE);
                raf.write(b);
                raf.close();
                HeapPage newPage = (HeapPage) bufferpool.getPage(tid, pid, Permissions.READ_WRITE);
                newPage.insertTuple(t);
                freePages.add(newPage);
            }
            catch (FileNotFoundException bad){
                System.out.println("File not found");
                return null;
            }
            catch (IOException bad){
                System.out.println("RAF seek or read failure");
            }
            //Let all other exceptions run
        }



        return freePages;
        // not necessary for proj1
    }

    // see DbFile.java for javadocs
    public Page deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        // some code goes here
        BufferPool bufferpool = Database.getBufferPool();
        HeapPage page = (HeapPage) bufferpool.getPage(tid, t.getRecordId().getPageId(), Permissions.READ_WRITE);
        page.deleteTuple(t);        
        return page; 
        // not necessary for proj1
    }


    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        // some code goes here
        return new HeapFileIterator(tid);
    }

    public class HeapFileIterator implements DbFileIterator {
        TransactionId tid;
        int pageNum;
        boolean opened;

        Iterator<Tuple> iterator;

        public HeapFileIterator(TransactionId tid){
            this.tid = tid;
            this.pageNum = 0;
            this.opened = false;
        }

        /**
         * Opens the iterator
         * @throws DbException when there are problems opening/accessing the database.
         */ 
        public void open() throws DbException, TransactionAbortedException{
            //Note: HeapPageId pgNo hardcoded to 0 in case .open() is called on already opened iterator - reset          
            HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, new HeapPageId(getId(),0), Permissions.READ_ONLY);
            if (page == null) throw new DbException("Database accessing error!");
            iterator = page.iterator();
            pageNum++;
            opened = true;
        }

        /** @return true if there are more tuples available. */
        public boolean hasNext() throws DbException, TransactionAbortedException{
            if (opened){
                if (iterator == null) return false;
                if (iterator.hasNext()) return true;
                //end of page, fetch the next populated page
                while (pageNum <= numPages()-1){
                    HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, new HeapPageId(getId(),pageNum++), Permissions.READ_ONLY);
                    iterator = page.iterator();
                    if (iterator.hasNext()) return true;
                }
            }
            return false;
        }


        /**
         * Gets the next tuple from the operator (typically implementing by reading
         * from a child operator or an access method).
         *
         * @return The next tuple in the iterator.
         * @throws NoSuchElementException if there are no more tuples
         */
        public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException{
            if (opened){
                if (iterator.hasNext()){
                    Tuple temp = iterator.next();
                    if (temp != null) return temp;                    
                }
                throw new NoSuchElementException("No more tuples");
            }
            throw new NoSuchElementException("Not opened!");  
        }

        /**
         * Resets the iterator to the start.
         * @throws DbException When rewind is unsupported.
         */
        public void rewind() throws DbException, TransactionAbortedException{
            close();
            open(); //will throw DbException if database fails
        }

        /**
         * Closes the iterator.
         */
        public void close(){
            //reset all values
            iterator = null;
            opened  = false;
            pageNum = 0;

        }

    }

}

