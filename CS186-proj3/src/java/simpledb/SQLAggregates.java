package simpledb;

class SQLAggregates {
	public String groupVal;
	public int count, sum, avg, min, max; //5 SQL aggregates

	public SQLAggregates(String groupVal){
		this.groupVal = groupVal;
		this.count = 0;
		this.sum = 0;
		this.avg = 0;
		this.min = Integer.MAX_VALUE; //good idea?
		this.max = Integer.MIN_VALUE;
	}

}