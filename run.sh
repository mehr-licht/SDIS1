#!/bin/bash

if [ $# -lt 2 ]; then
    echo $# arguments
    echo usage: "sh ./run.sh service number_of_peers"
    echo "service is one of backup, restore, reclaim, delete or state."
    exit 1
fi
 
version=2.0
service=$1
peers=$2

#state não pede nada
if [ $# -eq 2 ]; then
  if [ $service != "state" ]; then
	echo $# arguments
	echo "$service needs more arguments"
 	echo usage: "sh ./run.sh service number_of_peers"
  	echo "service either backup, restore, reclaim, delete or state."
echo .
echo "backup example: sh ./run.sh backup 2 "files/image1.png" 1"
echo "delete example: sh ./run.sh delete 2 "files/image1.png" "
echo "restore example: sh ./run.sh restore 2 "files/image1.png" "
echo "reclaim example: sh ./run.sh reclaim 2 9"
echo "state example: sh ./run.sh state 2"
	exit 1
  fi
fi

#backup pede file e replic
#delete pede file
#restore pede file
#reclaim pede space
if [ $# -ge 3 ]; then
#path name of the file or amount of space to be reclaim
	if [ $service != "reclaim" ]; then
	space=$3
	fi
	if [ $service != "reclaim" ]; then
	file=$3 	
	fi
fi;

if [ $# -eq 4 ]; then
  if [ $service != "backup" ]; then
	echo $# arguments
	echo "too many arguments for $service"
 	echo usage: "sh ./run.sh service number_of_peers"
  	echo "service is either backup, restore, reclaim, delete or state."
        exit 1
  fi
#specifies the desired replication degree. Applies only to backup sub-protocol
replic=$4
fi

echo "beginning $service service $version version with $peers peers (no rate so far)"
echo .
#clear the peers' file system
echo 'sh ./clearFileSystem.sh'
sh ./clearFileSystem.sh
echo 'all peers files system cleared'
echo .
#compile in LINUX/UNIX
echo 'sh ./compile.sh'
sh ./compile.sh
echo 'recompiled'
echo .
#start the RMI registry
echo 'sh ./rmi2.sh'
gnome-terminal --tab --title="RMI service" --command="bash -c 'sh ./rmi2.sh; $SHELL'" 
echo 'started the RMI registry in new tab'
echo .
#start all peers : sh peer.sh <protocol_version> <peer_id>       1 ... $peers;
for i in $(seq 1 1 $peers)
do
	echo 'sh ./peer.sh 2.0' $i
	gnome-terminal --tab --title="peer $i" --command="bash -c 'sh ./peer.sh $version $i; $SHELL'"       
done
echo "$i peers created in $i tabs"
echo .
#run the TestApp: "sh [backup|restore|reclaim|delete|state].sh" 
echo "sh ./$service.sh"


gnome-terminal --tab --title="$peer $i" --command="bash -c 'sh ./$service.sh; $SHELL'"
echo "file $service d"


#BACKUP file replic
#DELETE file
#RECLAIM space
#RESTORE file
#STATE
#run the TestApp: "sh [backup|restore|reclaim|delete|state].sh" 
if [ $service = "delete" ] || [ $service = "restore" ]; then
	echo "java TestApp $service $file"
	gnome-terminal --tab --title="$peer $i" --command="java TestApp $service $file"
fi
if [ $service = "reclaim" ]; then
	echo "java TestApp reclaim $space"
	gnome-terminal --tab --title="$peer $i" --command="java TestApp $reclaim $space"
fi
if [ $service = "backup" ]; then
	echo "java TestApp backup $file $replic"
	gnome-terminal --tab --title="$peer $i" --command="java TestApp backup $file $replic"
fi
if [ $service = "state" ]; then
	echo "java TestApp state"
	gnome-terminal --tab --title="$peer $i" --command="java TestApp state"
fi
