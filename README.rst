========
mdbSimple
========

:Description: This is a basic MongoDB application that simply inserts some documents. It is built upon the Firehose platform enabling threading and various other options.
:Author: Dave Finnegan <dave.finnegan@gmail.com>

Schema
======

The document schema is as follows

::

  col1 {
      _id: ObjectId()
      str: string
      num: integer
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
   * - -h
     - --hsots 
     - <host:port>           
     - ',' delimited list of mongodb hosts to connect to. Default localhost:27017
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

 java -jar target/mdbSimple-0.1.1.one-jar.jar --threads 12 --count 1000

Building
--------

The application is java based and includes a maven build configuration.  It also relies upon the Firehose application framework.

To build the application first download the Firehose framework (see dependencies below) and run the 'mvn install' command to build and install the framework jar.  Then run the 'mvn package' command within the mdbSimple application directory.  From there the target jar can be driven either without arguments to get usage information, or with arguments as shown in the example above.

Deployment
----------

Deployment of this simple application requires the existence of a MongoDB server, replica set, or sharded cluster.  Typical environments can be deployed on local nodes, from an MMS account, or through the AWS EC2 environment.  If this tool is to be used for benchmarking it is recommended that a minimum of a 3-node replica set be deployed with m3.xlarge AWS instances, along with at least one client node for driving load from the HealthCare application.

Firehose thread-pool framework
------------------------------

This simple application is built upon the Firehose thread-pool framework created by Bryan Reinero (reference below).

The process of creating an application upon the Firehose framework requires the creation of an Executor class (see mdbSimple.java), and an options.json file to define the application's command line arguments.  Another example of an application built upon the Firehose framework is the `DSVLoader <https://github.com/dave-finnegan/DSVLoader>`_ which is a delimiter separated value file import loader for MongoDB servers.

Dependencies
------------

mdbSimple is supported and somewhat tested on Java 1.7

Additional dependencies are:
    - `MongoDB Java Driver <http://docs.mongodb.org/ecosystem/drivers/java/>`_
    - `JUnit 4 <http://junit.org/>`_
    - `Apache Commons CLI 1.2 <http://commons.apache.org/proper/commons-cli/>`_
    - `Firehose thread-pool framework <https://github.com/dave-finnegan/Firehose>`_

    
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
This software is not supported by MongoDB, Inc. under any of their commercial support subscriptions or otherwise. Any usage of mdbSimple is at your own risk. Bug reports, feature requests and questions can be posted in the Issues section here on github.

