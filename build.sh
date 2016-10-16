#!/bin/bash
rm -rf out
mkdir out
javac -cp lib/quickcheck-0.6.jar:lib/jsoup-1.8.1.jar -d out @sourcefiles.txt