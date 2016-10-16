#!/bin/bash

function pause() {
    read -n1 -r -p "Press any key to continue..." key
}

java -Dfile.encoding=UTF8 -cp artifacts/*:lib/*:out info.kgeorgiy.java.advanced.walk.Tester RecursiveWalk ru.ifmo.ctddev.trofiv.walk.Walk
pause
java -Dfile.encoding=UTF8 -cp artifacts/*:lib/*:out info.kgeorgiy.java.advanced.arrayset.Tester NavigableSet ru.ifmo.ctddev.trofiv.arrayset.ArraySet
pause
java -Dfile.encoding=UTF8 -cp artifacts/*:lib/*:out info.kgeorgiy.java.advanced.implementor.Tester class ru.ifmo.ctddev.trofiv.implementor.Implementor
pause
java -Dfile.encoding=UTF8 -cp artifacts/*:lib/*:out info.kgeorgiy.java.advanced.implementor.Tester jar-class ru.ifmo.ctddev.trofiv.implementor.Implementor
pause
java -Dfile.encoding=UTF8 -cp artifacts/*:lib/*:out info.kgeorgiy.java.advanced.concurrent.Tester list ru.ifmo.ctddev.trofiv.concurrent.IterativeParallelism
pause
java -Dfile.encoding=UTF8 -cp artifacts/*:lib/*:out info.kgeorgiy.java.advanced.mapper.Tester list ru.ifmo.ctddev.trofiv.mapper.ParallelMapperImpl,ru.ifmo.ctddev.trofiv.concurrent.IterativeParallelism
pause
java -Dfile.encoding=UTF8 -cp artifacts/*:lib/*:out info.kgeorgiy.java.advanced.crawler.Tester hard ru.ifmo.ctddev.trofiv.crawler.WebCrawler
pause