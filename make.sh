#!/bin/bash
rm *.class 1>/dev/null 2>/dev/null
javac *.java 1>&2 2>build.log
