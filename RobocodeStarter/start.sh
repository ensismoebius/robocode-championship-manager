#!/bin/bash
pwd=`pwd`
cd "${0%/*}"
java -cp lib/robocode.jar:. BattleStarter -p $1 -r $2 -o $3 $4 $5 $6 $7 $8
cd "${pwd}"
