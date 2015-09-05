package simpledb;

import java.util.Arrays;

/** A class to represent a fixed-width histogram over a single integer-based field.
 */
public class IntHistogram {

    /**
     * Create a new IntHistogram.
     * 
     * This IntHistogram should maintain a histogram of integer values that it receives.
     * It should split the histogram into "buckets" buckets.
     * 
     * The values that are being histogrammed will be provided one-at-a-time through the "addValue()" function.
     * 
     * Your implementation should use space and have execution time that are both
     * constant with respect to the number of values being histogrammed.  For example, you shouldn't 
     * simply store every value that you see in a sorted list.
     * 
     * @param buckets The number of buckets to split the input value into.
     * @param min The minimum integer value that will ever be passed to this class for histogramming
     * @param max The maximum integer value that will ever be passed to this class for histogramming
     */
    private int buckets;
    private int min, max, width, total; 
    private int[] histogram;


    public IntHistogram(int buckets, int min, int max) {
    	// some code goes here
        this.buckets = buckets;
        this.min = min;
        this.max = max;
        this.histogram = new int[buckets]; //Java init all elements to 0
        this.width = (max - min) / buckets  + 1;
        this.total = 0;
    }

    public int getBucketNum(int v){
        return (v - this.min) / this.width;
    }

    public int getLeftPt(int bucketNum){
        return this.min + this.width * bucketNum;
    }

    public int getRightPt(int bucketNum){
        return getLeftPt(bucketNum + 1) - 1;
    }


    /**
     * Add a value to the set of values that you are keeping a histogram of.
     * @param v Value to add to the histogram
     */
    public void addValue(int v) {
    	// some code goes here
        int bucketNum = getBucketNum(v);
        this.histogram[bucketNum] += 1;
        this.total +=1;
    }

    /**
     * Estimate the selectivity of a particular predicate and operand on this table.
     * 
     * For example, if "op" is "GREATER_THAN" and "v" is 5, 
     * return your estimate of the fraction of elements that are greater than 5.
     * 
     * @param op Operator
     * @param v Value
     * @return Predicted selectivity of this particular operator and value
     */
    public double estimateSelectivity(Predicate.Op op, int v) {
    	// some code goes here

        //Do some prescreening to keep case statements cleaner looking
        if (this.total == 0){
            return 0.0;
        }
        else if (v < this.min){
            return (op.equals(Predicate.Op.GREATER_THAN) || op.equals(Predicate.Op.GREATER_THAN_OR_EQ) || op.equals(Predicate.Op.NOT_EQUALS))? 1.0 : 0.0; 
        }
        else if (v > this.max){
            return (op.equals(Predicate.Op.LESS_THAN) || op.equals(Predicate.Op.LESS_THAN_OR_EQ) || op.equals(Predicate.Op.NOT_EQUALS))? 1.0 : 0.0;
        }

        int bucketNum = getBucketNum(v);
        double selCount = 0, bPart = 0;

        //assume uniform distrib in bucket
        switch(op){
            // case LIKE:
                // return histogram[bucketNum] / ((double) this.width * this.total);
            case LIKE: case EQUALS:
                return histogram[bucketNum] / ((double) this.width * this.total);
            case GREATER_THAN:
                for (int i = bucketNum + 1; i < this.buckets; i++){
                    selCount += histogram[i];
                }
                bPart = ((getRightPt(bucketNum) - v) / this.width) * histogram[bucketNum];
                return (selCount + bPart) / (double) this.total;
            case LESS_THAN:
                for (int i = 0; i < bucketNum; i++){
                    selCount += histogram[i];
                }
                bPart = ((v - getLeftPt(bucketNum)) / this.width) * histogram[bucketNum];
                return (selCount + bPart) / (double) this.total;
            case LESS_THAN_OR_EQ:
                for (int i = 0; i < bucketNum; i++){
                    selCount += histogram[i];
                }
                bPart = (v - getLeftPt(bucketNum)) * histogram[bucketNum] + histogram[bucketNum];
                return (selCount + bPart) / ((double) this.width * this.total);
            case GREATER_THAN_OR_EQ:
                for (int i = bucketNum + 1; i < this.buckets; i++){
                    selCount += histogram[i];
                }
                bPart = (getRightPt(bucketNum) - v) * histogram[bucketNum] + histogram[bucketNum];
                return (selCount + bPart) / ((double) this.width * this.total);
            case NOT_EQUALS:
                return (this.total - histogram[bucketNum]) / ((double) this.width * this.total);
        }
    return 1.0;
    }
    
    /**
     * @return
     *     the average selectivity of this histogram.
     *     
     *     This is not an indispensable method to implement the basic
     *     join optimization. It may be needed if you want to
     *     implement a more efficient optimization
     * */
    public double avgSelectivity()
    {
        // some code goes here
        return 1.0;
    }
    
    /**
     * @return A string describing this histogram, for debugging purposes
     */
    public String toString() {

        // some code goes here
        return Arrays.toString(this.histogram);
    }
}
