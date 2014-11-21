package com.bryanreinero.util;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.MissingOptionException;

import com.bryanreinero.firehose.cli.CallBack;
import com.bryanreinero.firehose.cli.CommandLineInterface;
import com.bryanreinero.firehose.metrics.SampleSet;
import com.bryanreinero.util.WorkerPool.Executor;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.BasicDBObject;

public class Application {
	
	private final static String appName = "ApplicationFramework";
	private final WorkerPool workers;
	private final CommandLineInterface cli;
	private Printer printer = new Printer( DEFAULT_PRINT_INTERVAL );
	private final SampleSet samples;
	private DAO daoHospitals = null;
	private DAO daoPhysicians = null;
	private DAO daoPatients = null;
	private DAO daoProcedures = null;
	private DAO daoRecords = null;
	private DAO daoContent = null;
	
	public static final int DEFAULT_PRINT_INTERVAL = 1;
	public static final long DEFAULT_REPORTING_INTERVAL = 5;
	
	private int numThreads = 1; 
	private List<ServerAddress> adresses = null;
	private String writeConcern = null;
	private boolean journal = false;
	private boolean fsync = false;
	
	
	public static class ApplicationFactory {
		public static Application getApplication( String name, Executor executor, String[] args, Map<String, CallBack> cbs ) throws Exception {
			try {
				Application w = new Application(executor);
				
				w.cli.addOptions(name);
				//add custom callbacks
				for ( Entry<String, CallBack> e : cbs.entrySet() )
					w.cli.addCallBack(e.getKey(), e.getValue());
				
				try { 
					// the CLI is ready to parse the command line
					w.cli.parse(args);
				} catch ( MissingOptionException e) {
					w.cli.printHelp();
                    System.exit(-1);
					//throw new Exception( "bad options", e );
				}
				
				MongoClient client;
				if( w.adresses == null || w.adresses.isEmpty() ) 
					client = new MongoClient();
				else
					client = new MongoClient(w.adresses);
				
                w.daoHospitals =
                    new DAO(client.getDB("hcd").getCollection("hospitals"));
                w.daoPhysicians =
                    new DAO(client.getDB("hcd").getCollection("physicians"));
                w.daoPatients =
                    new DAO(client.getDB("hcd").getCollection("patients"));
                w.daoProcedures =
                    new DAO(client.getDB("hcd").getCollection("procedures"));
                BasicDBObject proceduresIndex = new BasicDBObject()
                    .append("hospital", 1)
                    .append("physician", 1)
                    .append("patient", 1)
                    .append("type", 1)
                    ;
                w.daoProcedures.createIndex(proceduresIndex);
                w.daoRecords =
                    new DAO(client.getDB("hcd").getCollection("records"));
                w.daoContent =
                    new DAO(client.getDB("hcd").getCollection("content"));

				if (  w.writeConcern != null ) {
                    w.daoHospitals.setConcern(w.writeConcern);
                    w.daoPhysicians.setConcern(w.writeConcern);
                    w.daoPatients.setConcern(w.writeConcern);
                    w.daoProcedures.setConcern(w.writeConcern);
                    w.daoRecords.setConcern(w.writeConcern);
                    w.daoContent.setConcern(w.writeConcern);
                }
				if ( w.journal) {
                    w.daoHospitals.setJournal(w.journal);
                    w.daoPhysicians.setJournal(w.journal);
                    w.daoPatients.setJournal(w.journal);
                    w.daoProcedures.setJournal(w.journal);
                    w.daoRecords.setJournal(w.journal);
                    w.daoContent.setJournal(w.journal);
                }
				if ( w.fsync ) {
                    w.daoHospitals.setFSync(w.fsync);
                    w.daoPhysicians.setFSync(w.fsync);
                    w.daoPatients.setFSync(w.fsync);
                    w.daoProcedures.setFSync(w.fsync);
                    w.daoRecords.setFSync(w.fsync);
                    w.daoContent.setFSync(w.fsync);
                }

				return w;

			} catch ( Exception e )  {
				throw new Exception( "Can't initialize Worker", e );
			}
		}
	}
	
	public SampleSet getSampleSet() {
		return samples;
	}

	public CommandLineInterface getCli() {
		return cli;
	}

	public void addCommandLineCallback(String key, CallBack cb ) {
		cli.addCallBack(key, cb);
	}

	private Application(Executor executor) throws Exception {
		samples = new SampleSet();
		samples.setTimeToLive(DEFAULT_REPORTING_INTERVAL);

		// prep the CLI with a set of 
		// standard CL option handlers
		cli = new CommandLineInterface();
		cli.addOptions(appName);

        // Threads
		cli.addCallBack("t", new CallBack() {

			@Override
			public void handle(String[] values) {
				numThreads = Integer.parseInt(values[0]);
			}
		});

		// Mongos'es
		cli.addCallBack("m", new CallBack() {

			@Override
			public void handle(String[] values) {
				adresses = DAO.getServerAddresses(values);

			}

		});

        // WriteConcern
		cli.addCallBack("wc", new CallBack() {
			@Override
			public void handle(String[] values) {
				writeConcern = values[0];
			}

		});

        // Write Journal
		cli.addCallBack("wj", new CallBack() {

			@Override
			public void handle(String[] values) {
				journal = Boolean.parseBoolean(values[0]);
			}

		});

        // Write sync
		cli.addCallBack("ws", new CallBack() {

			@Override
			public void handle(String[] values) {
				fsync = Boolean.parseBoolean(values[0]);
			}

		});

        // Print Interval
		cli.addCallBack("pi", new CallBack() {

			@Override
			public void handle(String[] values) {
				printer = new Printer(Integer.parseInt(values[0]));
			}
		});

        // Report Interval
		cli.addCallBack("ri", new CallBack() {

			@Override
			public void handle(String[] values) {
				samples.setTimeToLive(Long.parseLong(values[0]));
			}
		});

        // No pretty print (carriage return lines output)
		cli.addCallBack("cr", new CallBack() {

			@Override
			public void handle(String[] values) {
				printer.setConsole(false);
			}

		});

		workers = new WorkerPool(executor);
	}

	public void start() {
		workers.start(this.numThreads);
		printer.start();
	}
	
	public void stop() {
		workers.stop();
		printer.stop();
		samples.stop();
	}

	public int getNumThreads() {
		return this.numThreads;
	}

	public DAO getDAO(String name) {
        if (name.equals("hospitals"))
            return daoHospitals;
        if (name.equals("physicians"))
            return daoPhysicians;
        if (name.equals("patients"))
            return daoPatients;
        if (name.equals("procedures"))
            return daoProcedures;
        if (name.equals("records"))
            return daoRecords;
        if (name.equals("content"))
            return daoContent;
        throw new IllegalArgumentException("Invalid dao spec ("+name+")");
    }

	public void addPrintable(Object o) {
		printer.addPrintable(o);
	}

}

