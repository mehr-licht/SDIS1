#!/bin/sh
file=$1
java -classpath bin service.TestApp //127.0.0.1/1 RESTORE $1
