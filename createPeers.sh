#!/bin/bash

if [ $# -ne 2 ]; then
    echo $# arguments
    echo usage: "sh ./createPeers.sh version number_of_peers"
    exit 1
fi

version=$1
peers=$2


echo "Creating $peers peers"
echo .
#clear the peers' file system
echo 'Running: sh ./clearFiles.sh'
sh ./clearFiles.sh
echo 'Warning: all peers files system cleared'
echo .
#compile in LINUX/UNIX
echo 'Running: sh ./compile.sh'
sh ./compile.sh
echo 'Warning: recompiled finished'
echo .
#start the RMI registry
count=$(ps aux | grep rmiregistry | wc -l)
if [ $count -gt 1 ]
then
echo "RMIregistry service already running"
else
echo ' - Rmi not available -'
echo 'Running: sh ./rmi.sh'
sh ./rmi.sh
echo 'Started the RMI registry'
fi

echo .
#start all peers : sh peer.sh <protocol_version> <peer_id>       1 ... $peers;
for i in $(seq 1 1 $peers)
do
	echo "sh ./peer.sh $version $i"
	gnome-terminal --tab --title="Peer $i" -- bash -c "sh ./peer.sh $version $i; $SHELL"
done
echo "Successfully created $i peers in $i terminal tabs"
echo .

