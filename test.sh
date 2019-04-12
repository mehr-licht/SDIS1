!/bin/bash

##mudar teste rmi para rebind [NAO SEI SE NO SCRIPT SE NO tESTApp

######################  FUNCTIONS  ###########################

#SÓ O y É QUE ESTÁ A FUNCIONAR : SE SE FIZER < y ENTER > O SCRIPT CONTINUA : VER OUTROS EXEMPLOS
confirm(){
#wait for yes to continue or exit to exit...
read -p "Continue (y)es/(n)o/(e)exit?" choice

while [ $choice != "y" ] 
do
case "$choose" in
  [eE]) exit 0;;
esac
if [ $choice = "e" ] || [ $choice = "E" ]; then
exit 0
fi
#echo $msg1 > /dev/tty;
 #  case "$choice" in 
 # "y"|"Y" ) break;echo $msg2 > /dev/tty;;
 # "e"|"E" ) echo $msg3 > /dev/tty;exit 1;;
#esac
done
}





ask_state (){
#ask for the state of the 5 peers
local msg1="asking each peer for their state"
local msg2="check their state on each tab"
echo $msg1 > /dev/tty
java -classpath bin service.TestApp //localhost/1 STATE
java -classpath bin service.TestApp //localhost/2 STATE
java -classpath bin service.TestApp //localhost/3 STATE
java -classpath bin service.TestApp //localhost/4 STATE
java -classpath bin service.TestApp //localhost/5 STATE
echo $msg2 > /dev/tty 
}


state_confirm(){
$(ask_state)
$(confirm)
}





##########################  START  ###############################

######################################create 5 peers
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% create 5 peers %%%%%%%%%%%%%%%%%%%%%%"
echo " "
echo " "
sh createPeers.sh 5
echo " "
echo " "
$(state_confirm)
echo " "
######################################backup test
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% backup 'files/lusiadas.txt' from 1 with replication 3 %%%%%%%%%%%%%%%%%%%%%%"
echo " "
echo " "
java -classpath bin service.TestApp //localhost/1 BACKUP "files/lusiadas.txt" 3 
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% please check folder 'fileSystem/peer*/backups/0x########' for chunks %%%%%%%%%%%%%%%%%%%%%%"
echo " "
echo " "
$(state_confirm)
echo " "
######################################restore test
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% restore 'files/lusiadas.txt' to 1 %%%%%%%%%%%%%%%%%%%%%%"
echo " "
echo " "
java -classpath bin service.TestApp //localhost/1 RESTORE "files/lusiadas.txt"
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% please check folder 'fileSystem/peer1/restored/' for lusiadas.txt %%%%%%%%%%%%%%%%%%%%%%"
echo " "
echo " "
$(state_confirm)
echo " "
######################################reclaim test
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% reclaim 8000000-63000 bytes in peer 5 %%%%%%%%%%%%%%%%%%%%%%"
echo " "
echo " "
java -classpath bin service.TestApp //localhost/5 RECLAIM 63000
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% please check available space in peer5 %%%%%%%%%%%%%%%%%%%%%%"
echo " "
echo " "
$(state_confirm)
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% reclaim 8000000-63000 bytes in peer 4 %%%%%%%%%%%%%%%%%%%%%%"
echo " "
echo " "
java -classpath bin service.TestApp //localhost/4 RECLAIM 63000
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% please check available space in peer4"
echo " "
echo " "
$(state_confirm)
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% reclaim 8000000-63000 bytes in peer 3 %%%%%%%%%%%%%%%%%%%%%%"
echo " "
echo " "
java -classpath bin service.TestApp //localhost/3 RECLAIM 63000
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% please check available space in peer3"
echo " "
echo " "
echo "and check if any chunks were created on peer4 or peer5 %%%%%%%%%%%%%%%%%%%%%%"
echo " "
echo " "
$(state_confirm)
echo " "
######################################delete test
echo " "
echo " "
echo ">>>>>>>>>>>> SE CONFIRMAR QUE QUER CONTINUAR VAI APAGAR O FICHEIRO stf.txt - É APENAS UM Olá Mundo - é facilmente refeito <<<<<<<<<<<<<<"
$(confirm)
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% delete "files/stf.txt" from 1 %%%%%%%%%%%%%%%%%%%%%%"
echo " "
echo " "
java -classpath bin service.TestApp //localhost/1 delete "files/stf.txt"
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% please check peer1 for a file not found error %%%%%%%%%%%%%%%%%%%%%%"
echo " "
echo " "
$(confirm)
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% backup 'files/stf.txt' from 1 with replication 3 %%%%%%%%%%%%%%%%%%%%%%"
echo " "
echo " "
java -classpath bin service.TestApp //localhost/1 BACKUP "files/stf.txt" 3 
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% delete "files/stf.txt" from 1"
echo " "
echo " "
java -classpath bin service.TestApp //localhost/1 delete "files/stf.txt %%%%%%%%%%%%%%%%%%%%%%"
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% please check folder 'files/' ; file 'files/stf.txt' should not be present %%%%%%%%%%%%%%%%%%%%%%"
echo " "
echo " "
$(confirm)
echo " "
echo " "
echo " %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"
echo " %%%%%%%%%%%              %%%%%%%%%%%"
echo " %%%%%%%%%%% all tests ok %%%%%%%%%%%"
echo " %%%%%%%%%%%              %%%%%%%%%%%"
echo " %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"

exit 0


