#!/bin/sh
file=$1
replic=$2
java -classpath bin service.TestApp //127.0.0.1/1 BACKUP $file $replic
