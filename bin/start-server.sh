#!/bin/bash
cd `dirname $0`
DIR=$('pwd')
java -classpath $DIR/../printcat.jar;$DIR/../printcat-dep.jar \
com.cartury.printcat.akka.PrintcatServer \
$*