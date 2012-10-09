#!/bin/bash

E_SUCCESS="0"
E_WARNING="1"
E_CRITICAL="2"
E_UNKNOWN="3"

# Sanity check
if [ $# -ne 2 ]; then
        echo "Usage: $0 commandline1 commandline2"
        exit ${E_UNKNOWN}
fi

# checking whether probe is running
if ! ps ax | grep -v grep | grep grisu_probe > /dev/null
then
	echo "grisu_probe not running"
	exit ${E_UNKNOWN}
fi

LS_OUTPUT=`ls $1/error*`

for file in $LS_OUTPUT; do
	cat "$file" >> "$1/archive"
        rm "$file"
	echo
done


if grep -q "succeeded!" <<< $COMMAND; then
        echo "OK - $1 $2 working"
        exit ${E_SUCCESS}
        else
        echo "CRITICAL - $1 $2 not working"
        exit ${E_CRITICAL}
fi