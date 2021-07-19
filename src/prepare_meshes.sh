#!/bin/bash
for file in encode/*.obj;
do
    ./src/corto $file;
done
for file in encode/*.ply;
do
    ./src/corto $file;
done
