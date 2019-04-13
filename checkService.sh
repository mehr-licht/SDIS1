#!/bin/bash
count=$(ps aux | grep rmiregistry | grep -v grep |wc -l)
pid=$(ps -elf | grep rmiregistry | grep -v grep | awk '{print $4}')
if [ $count -gt 0 ]
then
echo "service is running!!!"
echo "with pid=$pid" 
else
echo "service not running!!!"
#/etc/init.d/$service start
fi
