========
HealthCare
========

:Description: This HealthCare application creates hospital, physician, patient, procedure, and record data and stores it in a MongoDB database.  Built upon Bryan Reinero's Firehose platform.
:Author: Dave Finnegan <dave.finnegan@gmail.com>

Overview 
========

HealthCare is a demo application which creates patient record data and stores it in a MongoDB database.  The data is randomly generated with input from lists of raw data gleaned from the internet for hospital name, procedure name, street, city, state, zip, and first and last name.

Usage
-----

.. list-table::
   :header-rows: 1
   :widths: 10,25,20,90

   * - **option**
     - **long form**
     - **type**
     - **description**
   * - -c,
     - --count
     -        
     - number of records to insert
   * - -cr,
     - --noPretty
     -        
     - print out in CR-delimited lines. Default is console mode pretty printing (when possible)
   * - -fc,
     - --city_file
     - <city filepath>               
     - filename to import city data from
   * - -ff,
     - --first_file
     - <first filepath>               
     - filename to import first name data from
   * - -fh,
     - --hospital_file
     - <hospital filepath>               
     - filename to import hospital data from
   * - -fl,
     - --last_file
     - <last filepath>               
     - filename to import city last name from
   * - -fp,
     - --procedures_file
     - <city filepath>               
     - filename to import procedure data from
   * - -fs,
     - --fsync 
     -                   
     - write concern: wait for page flush
   * - -m,
     - --mongos 
     - <host:port>           
     - ',' delimited list of mongodb hosts to connect to. Default localhost:27017
   * - -pi,
     - --printInterval  
     - <seconds>
     - print output every n seconds
   * - -ri,
     - --reportInterval
     - <seconds>        
     - average stats over a time interval of i milliseconds
   * - -t,
     - --threads 
     - <threads>         
     - number of worker threads. Default 1
   * - -v,
     - --verbose
     -            
     - Enable verbose output
   * - -wc,
     - --writeConcern 
     - <concern>   
     - write concern. Default = w:1
   * - -wj,
     - --journal
     -                
     - enable write concern wait for journal commit
   * - -ws,
     - --fsync
     -                
     - enable write concern wait for page flush

Example run
~~~~~~~~~~~

::

 java -jar target/HealthCare-0.1.1.one-jar.jar --threads 12 --count 1000

Dependencies
------------

Firehose is supported and somewhat tested on Java 1.7

Additional dependencies are:
    - `MongoDB Java Driver <http://docs.mongodb.org/ecosystem/drivers/java/>`_
    - `JUnit 4 <http://junit.org/>`_
    - `Apache Commons CLI 1.2 <http://commons.apache.org/proper/commons-cli/>`_

    
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
This software is not supported by MongoDB, Inc. under any of their commercial support subscriptions or otherwise. Any usage of Firehose is at your own risk. Bug reports, feature requests and questions can be posted in the Issues section here on github.

To Do
-----
- Move data files to resources directory
