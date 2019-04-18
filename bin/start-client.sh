#!/bin/bash
cd `dirname $0`
DIR=$('pwd')
java -cp $DIR/../printcat.jar \
com.cartury.printcat.PrintcatClient \
$*