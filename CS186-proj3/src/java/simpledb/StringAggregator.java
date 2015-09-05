package simpledb;

import java.util.*;
/**
 * Knows how to compute some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    /**
     * Aggregate constructor
     * @param gbfield the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield the 0-based index of the aggregate field in the tuple
     * @param what aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */
    private int gbfield;
    private Type gbfieldtype;
    private int afield;
    private Op operator;
    private HashMap<String,SQLAggregates> groupMap; //groupVal (the groupby field) to 5 SQLAggregates struct

    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // some code goes here
        this.gbfield = gbfield;
        this.gbfieldtype = gbfieldtype;
        this.afield = afield;
        if (what != Op.COUNT){
            // System.out.println("good");
            throw new IllegalArgumentException("StringAggregator can only COUNT!");    
        } 
        this.operator = what;
        this.groupMap = new HashMap<String,SQLAggregates>();
        // System.out.println(what.toString());
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        String groupVal = (this.gbfield == Aggregator.NO_GROUPING)? "" : tup.getField(gbfield).toString(); //good idea?
        SQLAggregates sag = (groupMap.containsKey(groupVal))? groupMap.get(groupVal) : new SQLAggregates(groupVal);
        sag.count++;
        // sag.avg = ((sag.avg * (sag.count-1)) + tupVal) / sag.count;

        //replace (update) or add new value
        // if (groupMap.containsKey(groupVal)) groupMap.remove(groupVal);
        groupMap.put(groupVal, sag);
    }

    /**
     * Create a DbIterator over group aggregate results.
     *
     * @return a DbIterator whose tuples are the pair (groupVal,
     *   aggregateVal) if using group, or a single (aggregateVal) if no
     *   grouping. The aggregateVal is determined by the type of
     *   aggregate specified in the constructor.
     */
    public DbIterator iterator() {
        // some code goes here
        ArrayList<Tuple> tuples = new ArrayList<Tuple>();
        int realafield;

        //Determine pair or single
        TupleDesc td;
        if (this.gbfield == NO_GROUPING){
            td = new TupleDesc( new Type[] {Type.INT_TYPE});
            realafield = 0;
        }
        else{
            td = new TupleDesc(new Type[] {this.gbfieldtype, Type.INT_TYPE});
            realafield = 1;
        }

        for (String groupVal : groupMap.keySet()){
            Tuple tup = new Tuple(td);
            SQLAggregates sag = groupMap.get(groupVal);

            if (this.gbfield != NO_GROUPING){ 
                if (gbfieldtype == Type.INT_TYPE) tup.setField(0, new IntField(  new Integer(groupVal)));
                else if (gbfieldtype == Type.STRING_TYPE) tup.setField(0, new StringField(groupVal, Type.STRING_TYPE.getLen()));
            }

            tup.setField(realafield, new IntField(sag.count));
            tuples.add(tup);

        }

        // System.out.println("got to iterator");
        return new TupleIterator(td ,tuples);
    }

}
