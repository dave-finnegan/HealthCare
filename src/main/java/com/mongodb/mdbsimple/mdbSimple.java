package com.mongodb.mdbsimple;

import com.bryanreinero.firehose.Converter;
import com.bryanreinero.firehose.Transformer;
import com.bryanreinero.firehose.cli.CallBack;
import com.bryanreinero.firehose.metrics.Interval;
import com.bryanreinero.firehose.metrics.SampleSet;

import com.bryanreinero.util.Application;
import com.bryanreinero.util.WorkerPool.Executor;
import com.bryanreinero.util.WorkerPool;
import com.bryanreinero.util.DAO;
import com.bryanreinero.util.Printer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import java.text.SimpleDateFormat;
import java.lang.Object;
import org.bson.types.ObjectId;

public class mdbSimple implements Executor {
	
	private static final String appName = "mdbSimple";
	private final Application worker;
	private final SampleSet samples;

    private String dbname = "mdbSimple";
    private Boolean dbPerCol = false;
    private Integer maxCount = 1;
	private AtomicInteger docCount = new AtomicInteger(0);

	private Converter converter = new Converter();
	private DAO daoSimple = null;
	
	private static Boolean verbose = false;
	
	public mdbSimple ( String[] args ) throws Exception {
		
		Map<String, CallBack> myCallBacks = new HashMap<String, CallBack>();
		
		//  command line callback for dbname
		myCallBacks.put("db", new CallBack() {
			@Override
			public void handle(String[] values) {
                dbname = values[0];
			}
		});

		// custom command line callback for docCount
		myCallBacks.put("c", new CallBack() {
			@Override
			public void handle(String[] values) {
                maxCount = Integer.parseInt(values[0]);
			}
		});

        // Verbose
		myCallBacks.put("v", new CallBack() {
			@Override
			public void handle(String[] values) {
				verbose = true;
			}
		});

		worker = Application.ApplicationFactory.getApplication
            (appName, this, args, myCallBacks);

		samples = worker.getSampleSet();
        daoSimple = worker.getDAO ( dbname, "col1");
		worker.addPrintable(this);
		worker.start();

	}
	
    @Override
    public void execute() {

        try {

            Integer currentCount = docCount.incrementAndGet();

            Interval intTotal = samples.set("Total");
            if (currentCount > maxCount) {
                worker.stop();

            } else {

                Random rnd = new Random();

                // Generate some random data

                // Doc fields
                ObjectId docId = ObjectId.get();
                int docNum = rnd.nextInt(100000);
                String lower = "abcdefghijklmnopqrstuvwxyz";
                char str[] = {lower.charAt(rnd.nextInt(26)), lower.charAt(rnd.nextInt(26)), lower.charAt(rnd.nextInt(26)), lower.charAt(rnd.nextInt(26))};
                String docStr = new String (str);

                BasicDBObject doc = new BasicDBObject()
                    .append("_id", docId)
                    .append("str", docStr)
                    .append("num", docNum)
                    ;
                WriteResult docResult = null;
                try {
                    Interval intInsert = samples.set("Insert");
                    docResult = daoSimple.insert (doc);
                    intInsert.mark();
                    docCount.incrementAndGet();
                } catch (Exception e) {
                    System.out.println ("Caught exception in insert: "
                                        +e.getMessage());
                    System.out.println ("doc: "+doc);
                }

            }
            intTotal.mark();

        } catch (Exception e) {
            System.out.println("Caught exception in execute:"+e.getMessage());
            e.printStackTrace();
            worker.stop();
        }
    }
	
	@Override 
	public String toString() {
		StringBuffer buf = new StringBuffer("{ ");
        if (verbose) {
            buf.append(String.format("threads: "+worker.getNumThreads()+", "));
        }
		buf.append(String.format("docCount: "+this.docCount ));
		buf.append(String.format(", samples: "+ samples ));
		
		if( verbose ) {
			buf.append(", dao: "+daoSimple);
		}
		buf.append(" }");
		return buf.toString();
	}
    
    public static void main( String[] args ) {
    	
    	try {
    		mdbSimple simple = new mdbSimple( args );
		} 
		catch (Exception e) {
            System.out.println("Caught exception in main:"+e.getMessage());
            if (verbose)
                e.printStackTrace();
            System.out.println("Exiting from main.");
			System.exit(-1);
		}
    }
}
