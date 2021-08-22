#!/bin/bash


# Change this to your netid
netid=nxr200017

#
# Root directory of your project
PROJDIR=/home/010/n/nx/nxr200017/6378/p2

#
# Directory where the config file is located on your local system
CONFIGLOCAL=config

i=0

cat $CONFIGLOCAL | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read n
    n=$(echo $n | awk '{ print $1 }')
    echo $n
    while [[ $i -lt $n ]]
    do
    	read line 
        host=$( echo $line | awk '{ print $2 }' )

        echo $host

        ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@$host.utdallas.edu "killall -u $netid" &

        i=$(( i + 1 ))
        
    done
   
)


echo "Cleanup complete"
