package simpledb;

import java.io.*;
import java.util.*;

public class LRUTest {

	public static void main(String[] args){

		Type types[] = new Type[]{ Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE };
        String names[] = new String[]{ "field0", "field1", "field2" };

        TupleDesc td = new TupleDesc(types, names);

        // create the tables, associate them with the data files
        // and tell the catalog about the schema  the tables.
        HeapFile table1 = new HeapFile(new File("some_data_file1.dat"), td);
        Database.getCatalog().addTable(table1, "t1");

        BufferPool pool = Database.getBufferPool();
        System.out.println(pool.size());

        pool.printVals();

	}
	
}