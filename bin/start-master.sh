#!/usr/bin/env bash
nohup \
java -cp ../out/artifacts/printcat_jar/printcat.jar \
com.cartury.printcat.PrintcatServer \
$* > server.out &