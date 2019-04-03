#!/bin/bash

if [ $# -ne 1 ]; then
    echo $# arguments
    echo usage: "sh ./createPeers.sh number_of_peers"
    exit 1
fi

version=1.0
peers=$1


echo "creating $peers peers"
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
count=$(ps aux | grep rmiregistry | wc -l)
if [ $count -gt 1 ]
then
echo "RMIregistry service already running"
else
echo 'sh ./rmi.sh'
sh ./rmi.sh
echo 'started the RMI registry'
fi

echo .
#start all peers : sh peer.sh <protocol_version> <peer_id>       1 ... $peers;
for i in $(seq 1 1 $peers)
do
	echo 'sh ./peer.sh 2.0' $i
	gnome-terminal --tab --title="peer $i" --command="bash -c 'sh ./peer.sh $version $i; $SHELL'"
done
echo "$i peers created in $i tabs"
echo .

