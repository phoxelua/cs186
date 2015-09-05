package simpledb;

import java.util.*;
/**
 * Knows how to compute some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    /**
     * Aggregate constructor
     * 
     * @param gbfield
     *            the 0-based index of the group-by field in the tuple, or
     *            NO_GROUPING if there is no grouping
     * @param gbfieldtype
     *            the type of the group by field (e.g., Type.INT_TYPE), or null
     *            if there is no grouping
     * @param afield
     *            the 0-based index of the aggregate field in the tuple
     * @param what
     *            the aggregation operator
     */

    private int gbfield;
    private Type gbfieldtype;
    private int afield;
    private Op operator;
    private HashMap<String,SQLAggregates> groupMap; //groupVal (the groupby field) to 5 SQLAggregates struct


    public IntegerAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // some code goes here
        this.gbfield = gbfield;
        this.gbfieldtype = gbfieldtype;
        this.afield = afield;
        this.operator = what;
        this.groupMap = new HashMap<String,SQLAggregates>();
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the
     * constructor
     * 
     * @param tup
     *            the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // some code goes here
        String groupVal = (this.gbfield == Aggregator.NO_GROUPING)? "" : tup.getField(gbfield).toString(); //good idea?
        int tupVal = ((IntField) tup.getField(afield)).getValue();
        SQLAggregates sag = (groupMap.containsKey(groupVal))? groupMap.get(groupVal) : new SQLAggregates(groupVal);
        sag.count++;
        sag.sum += tupVal;
        sag.avg = sag.sum / sag.count; //careful division by zero??
        sag.min = (sag.min < tupVal)? sag.min : tupVal;
        sag.max = (sag.max > tupVal)? sag.max : tupVal;
        // sag.avg = ((sag.avg * (sag.count-1)) + tupVal) / sag.count;

        //replace (update) or add new value
        if (groupMap.containsKey(groupVal)) groupMap.remove(groupVal);
        groupMap.put(groupVal, sag);

    }

    /**
     * Create a DbIterator over group aggregate results.
     * 
     * @return a DbIterator whose tuples are the pair (groupVal, aggregateVal)
     *         if using group, or a single (aggregateVal) if no grouping. The
     *         aggregateVal is determined by the type of aggregate specified in
     *         the constructor.
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

            int aggregateVal = 0;
            switch(this.operator){
                case COUNT:
                    aggregateVal = sag.count;
                    break;
                case SUM:
                    aggregateVal = sag.sum;
                    break;
                case AVG:
                    aggregateVal = sag.avg;
                    break;
                case MIN:
                    aggregateVal = sag.min;
                    break;
                case MAX:
                    aggregateVal = sag.max;
                    break;
            }

            tup.setField(realafield, new IntField(aggregateVal));
            tuples.add(tup);

        }


        return new TupleIterator(td ,tuples);
        // throw new
        // UnsupportedOperationException("please implement me for proj2");
    }

}
