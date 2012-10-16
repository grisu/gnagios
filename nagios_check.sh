#!/bin/bash

E_SUCCESS="0"
E_WARNING="1"
E_CRITICAL="2"
E_UNKNOWN="3"

# Sanity check
if [ $# -ne 1 ]; then
        echo "Usage: $0 path_to_output"
        exit ${E_UNKNOWN}
fi


LS_OUTPUT=`ls $1/error* 2> /dev/null`
ERRORS=0
for file in $LS_OUTPUT; do
        cat "$file" >> "$1/archive"
    rm "$file"
    ERRORS=$((ERRORS + 1))
done

LS_OUTPUT=`ls $1/success* 2> /dev/null`
SUCCESS=0
for file in $LS_OUTPUT; do
        cat "$file" >> "$1/archive"
    rm "$file"
    SUCCESS=$((SUCCESS + 1))
done


if [ "$ERRORS" -eq "0" ]; then
        if [ "$SUCCESS" -eq "0" ]; then
                echo "OK - No submissions finished"
        else
        echo "OK - $SUCCESS submission(s) successful"
        exit ${E_SUCCESS}
        fi
else
        echo "CRITICAL - $ERRORS submission(s) failed ($SUCCESS submissions successful)"
        exit ${E_CRITICAL}
