#!/bin/bash
[ "$#" -lt 1 ] && echo "ex: $0 number" && exit 1
min=1
max=$1
while [ $min -le $max ]
do
	echo "start agent : $min"
    	./run_one.sh &
	sleep 1
	min=`expr $min + 1`
done  

