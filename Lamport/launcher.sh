#!/bin/bash

# Change this to your netid
netid=nxr200017

# Root directory of your project
PROJDIR=/home/010/n/nx/nxr200017/6378/p2

# Directory where the config file is located on your local system
CONFIGLOCAL=config

# Directory your java classes are in
BINDIR=$PROJDIR

# Your main project class
PROG=Lamport

i=0

cat $CONFIGLOCAL | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read n
    n=$(echo $n | awk '{ print $1 }')
    echo $n
    while [[ $i -lt $n ]]
    do
    	read line
        echo $line
        words=($line)

	    ssh $netid@${words[1]}.utdallas.edu "cd $PROJDIR;java -cp $BINDIR $PROG config ${words[0]}"&
        
    
        i=$(( i + 1 ))
    done
    exit 0
)
