#!/bin/bash
for file in encode/*.obj;
do
    ./corto $file;
done
for file in encode/*.ply;
do
    ./corto $file;
done
