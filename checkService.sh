#!/bin/bash
count=$(ps aux | grep rmiregistry | wc -l)
if [ $count -gt 1 ]
then
echo "service is running!!!"
else
echo "service not running!!!"
#/etc/init.d/$service start
fi
