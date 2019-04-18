#!/bin/bash
cd `dirname $0`
CUR=$('pwd')
CLOSE=$CUR/../close/0
touch $CLOSE
rm $CLOSE