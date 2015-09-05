package simpledb;

/**
 * Inserts tuples read from the child operator into the tableid specified in the
 * constructor
 */
public class Insert extends Operator {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     * 
     * @param t
     *            The transaction running the insert.
     * @param child
     *            The child operator from which to read tuples to be inserted.
     * @param tableid
     *            The table in which to insert tuples.
     * @throws DbException
     *             if TupleDesc of child differs from table into which we are to
     *             insert.
     */

    private TransactionId tid;
    private DbIterator child;
    private int tableid;

    private TupleDesc td;
    private boolean called;

    public Insert(TransactionId t,DbIterator child, int tableid)
            throws DbException {
        // some code goes here
        if (child.getTupleDesc().equals(Database.getCatalog().getTupleDesc(tableid))){
            this.tid = tid;
            this.child = child;
            this.tableid = tableid;
            this.td = new TupleDesc(new Type[]{Type.INT_TYPE});
            this.called = false;

        }
        else{
            throw new DbException("child TupleDesc and table TupleDesc don't match!");
        }

    }

    public TupleDesc getTupleDesc() {
        // some code goes here
        return td;
    }

    public void open() throws DbException, TransactionAbortedException {
        child.open();
        super.open();
        // some code goes here
    }

    public void close() {
        // some code goes here
        super.close();
        child.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // some code goes here
        child.close();
        child.open();
    }

    /**
     * Inserts tuples read from child into the tableid specified by the
     * constructor. It returns a one field tuple containing the number of
     * inserted records. Inserts should be passed through BufferPool. An
     * instances of BufferPool is available via Database.getBufferPool(). Note
     * that insert DOES NOT need check to see if a particular tuple is a
     * duplicate before inserting it.
     * 
     * @return A 1-field tuple containing the number of inserted records, or
     *         null if called more than once.
     * @see Database#getBufferPool
     * @see BufferPool#insertTuple
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // some code goes here
        if (!called){
            int count = 0;    
            while(child.hasNext()){
                try{
                    Database.getBufferPool().insertTuple(tid, tableid, child.next());
                    count++;
                }
                catch (Exception bad){
                    // System.out.println(child.next());
                    // System.out.println("fetchNext cased IOException");
                    bad.printStackTrace();
                    //do nothing
                }
            }
            called = true;
            Tuple tup = new Tuple(getTupleDesc());
            tup.setField(0, new IntField(count));
            return tup;
        }
        return null;
    }

    @Override
    public DbIterator[] getChildren() {
        // some code goes here
        return new DbIterator[] {child};
    }

    @Override
    public void setChildren(DbIterator[] children) {
        // some code goes here
        this.child = children[0];
    }
}
