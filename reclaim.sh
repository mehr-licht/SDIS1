#!/bin/sh
reclaim=$1
java -classpath bin service.TestApp //127.0.0.1/2 RECLAIM $reclaim
