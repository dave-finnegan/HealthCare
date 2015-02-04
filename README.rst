========
HealthCare
========

:Description: This application is designed to simulate a healthcare patient record data load on a MongoDB database.  The driver creates five document collections representing hospital, physician, patient, procedure, and patient record data.
:Author: Dave Finnegan <dave.finnegan@gmail.com>

Overview 
========

Healthcare patient records data is hiearchical in nature.  Hospitals relate to physicians, physicians to patients, patients to procedures, and procedures to patient records.  Inter-relationships are useful for satisfying various queries of the data set.

The data is randomly generated with input from lists of raw data gleaned from the Internet for hospital name, procedure name, street, city, state, zip, and first and last name values.

The application takes a number of command line switches to make possible a variety of different load conditions.  The default behaviour is to create (insert) a single patient record document, followed by updates, or upserts, to each of the related procedure, patient, physician, and hospital documents.  The schema for these docs is listed below.

Options
=======

Options exist to change the load charateristics in the following ways:

  - Insert only: Create only the patient record document which drives an insert only work load (-ix command line switch)
  - Large/Small/Tiny: Create patient record content of varying size to simulate docs/sec or MB/sec work loads (-cd large|small|tiny)
  - DB per Collection: Create a seperate DB for each of the five collections allowing for write lock load testing (-dbpercol)
  - Thread count: Specify number of threads per application client (-t <cnt>)
  - Write Concern, journal, fsync: Specify write options (-wc, -wj, -ws)

Schema
======

The document schema is as follows

::

  hospitals {
      _id: integer
      name: string
      city: string
      state: string
      beds: integer
      trauma_center: boolean
      physicians: array of integer
      }
  physicians {
      _id: integer
      first: string
      last: string
      addr: {
        street: string
        city: string
        state: string
        zip: int
      }
      hospitals: array of integer
    }
  patients {
      _id: integer
      first: string
      last: string
      addr: {
        street: string
        city: string
        state: string
        zip: int
      }
      physicians: array of integer
      procedures: array of integer
    }
  procedures {
      _id: integer
      type: string
      date: date
      hospital: integer
      physician: integer
      patient: integer
      records: array of ObjectId()
    }
  records {
      _id: ObjectId()
      type: string
      procedure: integer
      content: binary
    }

Usage
=====

.. list-table::
   :header-rows: 1
   :widths: 10,25,20,90

   * - **option**
     - **long form**
     - **type**
     - **description**
   * - -c
     - --count
     - integer
     - number of records to insert (required)
   * - -cr
     - --noPretty
     -        
     - print out in CR-delimited lines. Default is console mode pretty printing (when possible)
   * - -fc
     - --city_file
     - <city filepath>               
     - filename to import city data from
   * - -ff
     - --first_file
     - <first filepath>               
     - filename to import first name data from
   * - -fh
     - --hospital_file
     - <hospital filepath>               
     - filename to import hospital data from
   * - -fl
     - --last_file
     - <last filepath>               
     - filename to import city last name from
   * - -fp
     - --procedures_file
     - <city filepath>               
     - filename to import procedure data from
   * - -fs
     - --fsync 
     -                   
     - write concern: wait for page flush
   * - -h
     - --hsots 
     - <host:port>           
     - ',' delimited list of mongodb hosts to connect to. Default localhost:27017
   * - -ix
     - --insert_only
     -                   
     - create only patient records (insert only load; no updates/upserts)
   * - -pi
     - --printInterval  
     - <seconds>
     - print output every n seconds
   * - -ri
     - --reportInterval
     - <seconds>        
     - average stats over a time interval of i milliseconds
   * - -t
     - --threads 
     - <threads>         
     - number of worker threads. Default 1
   * - -up
     - --usrpwd 
     - <usr:pwd>
     - username and password: must specify --hosts switch!
   * - -v
     - --verbose
     -            
     - Enable verbose output
   * - -wc
     - --writeConcern 
     - <concern>   
     - write concern. Default = w:1
   * - -wj
     - --journal
     -                
     - enable write concern wait for journal commit
   * - -ws
     - --fsync
     -                
     - enable write concern wait for page flush

Example run
~~~~~~~~~~~

::

 java -jar target/HealthCare-0.1.1.one-jar.jar --threads 12 --count 1000

Deployment
----------

Deployment of this HealthCare application requires the existence of a MongoDB server, replica set, or sharded cluster.  Typical environments can be deployed on local nodes, from an MMS account, or through the AWS EC2 environment.  If this tool is to be used for benchmarking it is recommended that a minimum of a 3-node replica set be deployed with m3.xlarge AWS instances, along with at least one client node for driving load from the HealthCare application.

Metrics
-------

Preliminary testing shows throughput rates for a 3-node replica set running on m3.xlarge instances as follows:

.. list-table::
   :header-rows: 1
   :widths: 10,25,90

   * - **Version**
     - **Switches**
     - **Throughput**
   * - 2.8.0-rc5 MMAPv1
     - -t 16 -cd tiny -c 500000 -ix
     - 13.2k ops (500k inserts)
   * - 2.8.0-rc5 MMAPv1
     - -t 16 -cd tiny -c 100000
     - 11.9k ops (100k i, 400k u)
   * - 2.8.0-rc5 wt
     - -t 16 -cd tiny -c 500000 -ix
     - 13.2k ops (500k inserts)
   * - 2.8.0-rc5 wt
     - -t 16 -cd tiny -c 100000
     - 11.9k ops (100k i, 400k u)
   * 2.6.4
     - -t 16 -cd tiny -c 500000 -ix
     - 13.8k ops (500k inserts)
   * 2.6.4
     - -t 16 -cd tiny -c 100000
     - 8.6k ops (empty db; 100k i, 400k u)


Firehose thread-pool framework
------------------------------

This HealthCare application is built upon the Firehose thread-pool framework created by Bryan Reinero (reference below).

The process of creating an application upon the Firehose framework requires the creation of an Executor class (see HealthCare.java), and an options.json file to define the application's command line arguments.  Another example of an application built upon the Firehose framework is the `DSVLoader <https://github.com/dave-finnegan/DSVLoader>`_ which is a delimiter separated value file import loader for MongoDB servers.

Dependencies
------------

HealthCare is supported and somewhat tested on Java 1.7

Additional dependencies are:
    - `MongoDB Java Driver <http://docs.mongodb.org/ecosystem/drivers/java/>`_
    - `JUnit 4 <http://junit.org/>`_
    - `Apache Commons CLI 1.2 <http://commons.apache.org/proper/commons-cli/>`_
    - `Firehose thread-pool framework <https://github.com/dave-finnegan/Firehose>>`_

    
License
-------
Copyright (C) {2014}  {Dave Finnegan}, {2013}  {Bryan Reinero}

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.


Disclaimer
----------
This software is not supported by MongoDB, Inc. under any of their commercial support subscriptions or otherwise. Any usage of HealthCare is at your own risk. Bug reports, feature requests and questions can be posted in the Issues section here on github.

