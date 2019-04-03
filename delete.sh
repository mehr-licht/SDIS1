#!/bin/sh
file=$1
java -classpath bin service.TestApp //localhost/1 DELETE $file
