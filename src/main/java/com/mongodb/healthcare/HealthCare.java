package com.mongodb.healthcare;

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

public class HealthCare implements Executor {
	
	private static final String appName = "HealthCare";
	private final Application worker;
	private final SampleSet samples;

    private String dbname = "hcd";
    private Integer maxCount = 0;
    private String contentSubDir = "small";
	private AtomicInteger count = new AtomicInteger(0);

	private AtomicInteger countHospitals = new AtomicInteger(0);
	private AtomicInteger countPhysicians = new AtomicInteger(0);
	private AtomicInteger countPatients = new AtomicInteger(0);
	private AtomicInteger countProcedures = new AtomicInteger(0);
	private AtomicInteger countRecords = new AtomicInteger(0);

    private HealthCareCallBack hospitalCB = new HealthCareCallBack();
    private HealthCareCallBack proceduresCB = new HealthCareCallBack();
    private HealthCareCallBack cityCB = new HealthCareCallBack();
    private HealthCareCallBack streetsCB = new HealthCareCallBack();
    private HealthCareCallBack lastCB = new HealthCareCallBack();
    private HealthCareCallBack firstCB = new HealthCareCallBack();

	private Converter converter = new Converter();
	private DAO daoHospitals = null;
	private DAO daoPhysicians = null;
	private DAO daoPatients = null;
	private DAO daoProcedures = null;
	private DAO daoRecords = null;
    private DAO daoContent = null;
	
	private static Boolean verbose = false;
	private String filename = null;
	
    class HealthCareCallBack implements CallBack {
        
        private String filename = null;
        private BufferedReader br = null;
        private Converter converter = new Converter();
        private Integer linesRead = 0;
        public List<DBObject> DBObjects = null;
        
        public void dataFileHandler(String type, String[] values) {
            filename  = values[0];
            try {
                BufferedReader br = null;
                if (type.equals("resource")) {
                    InputStream is =
                        HealthCare.class.getResourceAsStream(filename);
                    br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                } else if (type.equals("filesystem")) {
                    br = new BufferedReader(new FileReader(filename));
                }

                DBObjects = new ArrayList<DBObject>();
                
                // read header line from file
                String ln = br.readLine();
                String colDelim = ln.substring(0,1);
                String fieldDelim = ln.substring(1,2);
                String header = ln.substring(2);
                converter.setDelimiter( colDelim.charAt(0) );
                for (String column : header.split(colDelim)) {
                    String[] s = column.split(fieldDelim);
                    converter.addField(s[0], Transformer.getTransformer(s[1]));
                }
                
                // read data lines
                try {
                    
                    // read the next line from source file
                    while ((ln=br.readLine()) != null) {

                        linesRead += 1;
                        
                        // Create the hospital DBObject
                        DBObject object = converter.convert( ln );
                        
                        // Add the DBObject to the DBObjects array
                        DBObjects.add(object);
                    }
                } catch (IOException e) {
                    System.out.println("Caught exception in HealthCareCallBack: "
                                       +e.getMessage());
                    if (verbose)
                        e.printStackTrace();
                    try {
                        synchronized ( br ) {
                            if (br != null) br.close();
                        }
                    } catch (IOException ex) {
                        System.out.println
                            ("Caught exception in HealthCareCallBack: "
                             +e.getMessage());
                        if (verbose)
                            ex.printStackTrace();
                    }
                }
                br.close();
                br = null;
            } catch (Exception e) {
                System.out.println("Caught excepting in HealthCareCallback: "
                                   +e.getMessage());
                if (verbose)
                    e.printStackTrace();
                System.exit(-1);
            }
        }

        public void handle(String[] values) {
            dataFileHandler ("filesystem", values);
        }
    }
    
	public HealthCare ( String[] args ) throws Exception {
		
		Map<String, CallBack> myCallBacks = new HashMap<String, CallBack>();
		
		//  command line callback for dbname
		myCallBacks.put("db", new CallBack() {
			@Override
			public void handle(String[] values) {
                dbname = values[0];
			}
		});

		// custom command line callback for count
		myCallBacks.put("c", new CallBack() {
			@Override
			public void handle(String[] values) {
                maxCount = Integer.parseInt(values[0]);
			}
		});

        // Content directory; defaults to 'small' w/in resources directory
		myCallBacks.put("cd", new CallBack() {
			@Override
			public void handle(String[] values) {
				contentSubDir = values[0];
			}

		});

        // Verbose
		myCallBacks.put("v", new CallBack() {
			@Override
			public void handle(String[] values) {
				verbose = true;
			}
		});

		// custom command line callback for hospital data file
		myCallBacks.put("fh", hospitalCB);

		// custom command line callback for procedures data file
		myCallBacks.put("fp", proceduresCB);

		// custom command line callback for city data file
		myCallBacks.put("fc", cityCB);

		// custom command line callback for streets data file
		myCallBacks.put("fs", streetsCB);

		// custom command line callback for last data file
		myCallBacks.put("fl", lastCB);

		// custom command line callback for first data file
		myCallBacks.put("ff", firstCB);

		worker = Application.ApplicationFactory.getApplication
            (appName, this, args, myCallBacks);

        if (hospitalCB.DBObjects == null)
            hospitalCB.dataFileHandler
                ("resource", new String[] {"/data/hospitals.dat"});
        if (proceduresCB.DBObjects == null)
            proceduresCB.dataFileHandler
                ("resource", new String[] {"/data/procedures.dat"});
        if (cityCB.DBObjects == null)
            cityCB.dataFileHandler
                ("resource", new String[] {"/data/city.dat"});
        if (streetsCB.DBObjects == null)
            streetsCB.dataFileHandler
                ("resource", new String[] {"/data/streets.dat"});
        if (lastCB.DBObjects == null)
            lastCB.dataFileHandler
                ("resource", new String[] {"/data/last.dat"});
        if (firstCB.DBObjects == null)
            firstCB.dataFileHandler
                ("resource", new String[] {"/data/first.dat"});

		samples = worker.getSampleSet();
        daoHospitals = worker.getDAO(dbname, "hospitals");
        daoPhysicians = worker.getDAO(dbname, "physicians");
        daoPatients = worker.getDAO(dbname, "patients");
        daoProcedures = worker.getDAO(dbname, "procedures");
        BasicDBObject proceduresIndex = new BasicDBObject()
            .append("hospital", 1)
            .append("physician", 1)
            .append("patient", 1)
            .append("type", 1)
            ;
        daoProcedures.createIndex(proceduresIndex);
        daoRecords = worker.getDAO(dbname, "records");
        daoContent = worker.getDAO(dbname, "content");
		worker.addPrintable(this);
		worker.start();

	}
	
    @Override
    public void execute() {

        try {

            Integer currentCount = count.incrementAndGet();

            Interval intTotal = samples.set("Total");
            if (currentCount > maxCount) {
                worker.stop();

            } else {

                Random rnd = new Random();

                // Generate some random data
                // hospital, physician, patient, procedure, record

                // Various IDs
                int hospitalId = rnd.nextInt(hospitalCB.linesRead);
                int physicianId = rnd.nextInt(50000);   // 50k US doctors
                int patientId = rnd.nextInt(300000000); // 300M US pop.; ssn
                int procedureIdx = rnd.nextInt(proceduresCB.linesRead);
                ObjectId procedureId = ObjectId.get();
                ObjectId recordId = ObjectId.get();

                // Record
                Interval intRecordTotal = samples.set("RecordTotal");
                Interval intRecordBuild = samples.set("RecordBuild");
                String[] recordTypes = {
                    "txt",   "txt",   "txt",   "txt", // 40% txt (1k)
                    "jpg", "jpg",                     // 20% jpg (200k)
                    "pdf", "pdf",                     // 20% pdf (500k)
                    "pdf",                            // 10% pdf (1M)
                    "pdf"                             // 10% pdf (13M)
                };
                String[] recordFiles = {
                    "data-1k.txt", "data-1k.txt", "data-1k.txt", "data-1k.txt",
                    "data-200k.jpg", "data-200k.jpg",
                    "data-500k.pdf", "data-500k.pdf",
                    "data-1M.pdf",
                    "data-13M.pdf"
                };
                int recordTypeIdx = rnd.nextInt(recordTypes.length);
                // Resource directory based content file
                byte[] recordFileData = null;
                int recordFileLength = 0;
                if (contentSubDir.equals("tiny")) {
                    recordFileData = new byte[2];
                    recordFileLength = 1;
                } else {
                    String recordFile =
                        "/content/"+contentSubDir+"/"+recordFiles[recordTypeIdx];
                    InputStream recordFileIS =
                        HealthCare.class.getResourceAsStream(recordFile);
                    DataInputStream recordFileDIS =
                        new DataInputStream(recordFileIS);
                    recordFileLength = recordFileIS.available();
                    recordFileData = new byte[recordFileLength];
                    recordFileDIS.readFully(recordFileData);
                    recordFileDIS.close();
                    recordFileIS.close();
                }
                BasicDBObject recordDoc = new BasicDBObject()
                    .append("_id", recordId)
                    .append("type", recordTypes[recordTypeIdx])
                    .append("size", recordFileLength)
                    .append("content", recordFileData)
                    .append("procedure", procedureId)
                    ;
                intRecordBuild.mark();
                WriteResult recordResult = null;
                try {
                    Interval intRecordInsert = samples.set("RecordInsert");
                    recordResult = daoRecords.insert (recordDoc);
                    intRecordInsert.mark();
                    countRecords.incrementAndGet();
                } catch (Exception e) {
                    System.out.println ("Caught exception in RecordInsert: "
                                        +e.getMessage());
                    System.out.println ("record: "+recordDoc);
                }
                intRecordTotal.mark();

                // Procedure
                Interval intProcedureTotal = samples.set("ProcedureTotal");
                Interval intProcedureBuild = samples.set("ProcedureBuild");
                String procedureType =
                    (String)proceduresCB.DBObjects.get(procedureIdx).get("name");
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd");
                String dateInString = String.format
                    ( "%4d.%02d.%02d",rnd.nextInt(5)+2000,rnd.nextInt(11)+1,
                      rnd.nextInt(28));
                Date procedureDate = formatter.parse(dateInString);
                BasicDBObject procedureQuery = new BasicDBObject()
                    .append("_id", procedureId)
                    .append("hospital", hospitalId)
                    .append("physician", physicianId)
                    .append("patient", patientId)
                    .append("type", procedureType)
                    ;
                BasicDBObject procedureDoc = new BasicDBObject()
                    .append("$setOnInsert", procedureQuery)
                    .append("$addToSet", new BasicDBObject("records", recordId))
                    ;
                intProcedureBuild.mark();
                WriteResult procedureResult = null;
                try {
                    Interval intProcedureInsert = samples.set("ProcedureInsert");
                    procedureResult =
                        daoProcedures.update (procedureQuery, procedureDoc,
                                              true, false);
                    intProcedureInsert.mark();
                    countProcedures.incrementAndGet();
                } catch (Exception e) {
                    System.out.println ("Caught exception in ProcedureUpdate: "
                                        +e.getMessage());
                    System.out.println ("procedure: "+procedureDoc);
                }
                intProcedureTotal.mark();

                // Patient
                Interval intPatientTotal = samples.set("PatientTotal");
                Interval intPatientBuild = samples.set("PatientBuild");
                String patientFirst = (String)firstCB.DBObjects.get
                    (rnd.nextInt(firstCB.linesRead)).get("name");
                String patientLast = (String)lastCB.DBObjects.get
                    (rnd.nextInt (lastCB.linesRead)).get("name");
                String patientStreet =
                    Integer.toString(rnd.nextInt(1000))+" "+
                    (String)streetsCB.DBObjects
                    .get(rnd.nextInt(streetsCB.linesRead)).get("name");
                int cityIdx = rnd.nextInt(cityCB.linesRead);
                String patientCity = (String)cityCB.DBObjects.get
                    (cityIdx).get("city");
                String patientState = (String)cityCB.DBObjects.get
                    (cityIdx).get("state");
                Integer patientZip = (Integer)cityCB.DBObjects.get
                    (cityIdx).get("zip");
                BasicDBObject patientAddr =
                    new BasicDBObject()
                    .append("street", patientStreet)
                    .append("city", patientCity)
                    .append("state", patientState)
                    .append("zip", patientZip);
                BasicDBObject patientQuery = new BasicDBObject()
                    .append("_id", patientId)
                    ;
                BasicDBObject patientDocFields = new BasicDBObject()
                    .append("_id", patientId)
                    .append("first", patientFirst)
                    .append("last", patientLast)
                    .append("addr", patientAddr)
                    ;
                BasicDBObject patientDocPush = new BasicDBObject()
                    .append("physicians", physicianId)
                    .append("procedures", procedureResult.getUpsertedId())
                    ;
                BasicDBObject patientDoc = new BasicDBObject()
                    .append("$setOnInsert", patientDocFields)
                    .append("$addToSet", patientDocPush)
                    ;
                intPatientBuild.mark();
                WriteResult patientResult = null;
                try {
                    Interval intPatientUpdate = samples.set("PatientUpdate");
                    patientResult =
                        daoPatients.update (patientQuery, patientDoc,
                                            true, false);
                    intPatientUpdate.mark();
                    countPatients.incrementAndGet();
                } catch (Exception e) {
                    System.out.println ("Caught exception in PatientUpdate: "
                                        +e.getMessage());
                    System.out.println ("patient: "+patientDoc);
                }
                intPatientTotal.mark();

                // Physician
                Interval intPhysicianTotal = samples.set("PhysicianTotal");
                Interval intPhysicianBuild = samples.set("PhysicianBuild");
                String physicianFirst = (String)firstCB.DBObjects.get
                    (rnd.nextInt(firstCB.linesRead)).get("name");
                String physicianLast = (String)lastCB.DBObjects.get
                    (rnd.nextInt (lastCB.linesRead)).get("name");
                String physicianStreet =
                    Integer.toString(rnd.nextInt(1000))+" "+
                    (String)streetsCB.DBObjects
                    .get(rnd.nextInt(streetsCB.linesRead)).get("name");
                cityIdx = rnd.nextInt(cityCB.linesRead);
                String physicianCity = (String)cityCB.DBObjects.get
                    (cityIdx).get("city");
                String physicianState = (String)cityCB.DBObjects.get
                    (cityIdx).get("state");
                Integer physicianZip = (Integer)cityCB.DBObjects.get
                    (cityIdx).get("zip");
                BasicDBObject physicianAddr =
                    new BasicDBObject()
                    .append("street", physicianStreet)
                    .append("city", physicianCity)
                    .append("state", physicianState)
                    .append("zip", physicianZip);
                BasicDBObject physicianQuery = new BasicDBObject()
                    .append("_id", physicianId)
                    ;
                BasicDBObject physicianDocFields = new BasicDBObject()
                    .append("_id", physicianId)
                    .append("first", physicianFirst)
                    .append("last", physicianLast)
                    .append("addr", physicianAddr)
                    ;
                BasicDBObject physicianDoc = new BasicDBObject()
                    .append("$setOnInsert", physicianDocFields)
                    .append("$addToSet", new BasicDBObject
                            ("hospitals", hospitalId))
                    ;
                intPhysicianBuild.mark();
                WriteResult physicianResult = null;
                try {
                    Interval intPhysicianUpdate = samples.set("PhysicianUpdate");
                    physicianResult =
                        daoPhysicians.update (physicianQuery, physicianDoc,
                                            true, false);
                    intPhysicianUpdate.mark();
                    countPhysicians.incrementAndGet();
                } catch (Exception e) {
                    System.out.println ("Caught exception in PhysicianUpdate: "
                                        +e.getMessage());
                    System.out.println ("physician: "+physicianDoc);
                }
                intPhysicianTotal.mark();

                // Hospital
                Interval intHospitalTotal = samples.set("HospitalTotal");
                Interval intHospitalBuild = samples.set("HospitalBuild");
                int hospitalIdx = rnd.nextInt(hospitalCB.linesRead);
                String hospitalName = (String)hospitalCB.DBObjects.get
                    (hospitalIdx).get("name");
                String hospitalCity = (String)hospitalCB.DBObjects.get
                    (hospitalIdx).get("city");
                String hospitalState = (String)hospitalCB.DBObjects.get
                    (hospitalIdx).get("state");
                int hospitalBeds = rnd.nextInt(280) + 20; // min 20; max 300
                Boolean[] hospitalBooleans = {true,false};
                Boolean hospitalTraumaCenter = hospitalBooleans[rnd.nextInt(2)];
                BasicDBObject hospitalQuery = new BasicDBObject()
                    .append("_id", hospitalId)
                    ;
                BasicDBObject hospitalDocFields = new BasicDBObject()
                    .append("_id", hospitalId)
                    .append("name", hospitalName)
                    .append("city", hospitalCity)
                    .append("state", hospitalState)
                    .append("beds", hospitalBeds)
                    .append("trauma center", hospitalTraumaCenter)
                    ;
                BasicDBObject hospitalDoc = new BasicDBObject()
                    .append("$setOnInsert", hospitalDocFields)
                    .append("$addToSet", new BasicDBObject
                            ("physicians", physicianId))
                    ;
                intHospitalBuild.mark();
                WriteResult hospitalResult = null;
                try {
                    Interval intHospitalUpdate = samples.set("HospitalUpdate");
                    hospitalResult =
                        daoHospitals.update (hospitalQuery, hospitalDoc,
                                            true, false);
                    intHospitalUpdate.mark();
                    countHospitals.incrementAndGet();
                } catch (Exception e) {
                    System.out.println ("Caught exception in HospitalUpdate: "
                                        +e.getMessage());
                    System.out.println ("hospital: "+hospitalDoc);
                }
                intHospitalTotal.mark();

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
		buf.append(String.format("threads: "+worker.getNumThreads()));
		buf.append(String.format(", count: "+this.count ));
		buf.append(String.format(", countHospitals: "+this.countHospitals ));
		buf.append(String.format(", countPhysicians: "+this.countPhysicians ));
		buf.append(String.format(", countPatients: "+this.countPatients ));
		buf.append(String.format(", countProcedures: "+this.countProcedures ));
		buf.append(String.format(", countRecords: "+this.countRecords ));
		buf.append(String.format(", samples: "+ samples ));
		
		if( verbose ) {
			buf.append(", dao: "+daoHospitals);
			buf.append(", dao: "+daoPhysicians);
			buf.append(", dao: "+daoPatients);
			buf.append(", dao: "+daoProcedures);
			buf.append(", dao: "+daoRecords);
		}
		buf.append(" }");
		return buf.toString();
	}
    
    public static void main( String[] args ) {
    	
    	try {
    		HealthCare hcd = new HealthCare( args );
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
