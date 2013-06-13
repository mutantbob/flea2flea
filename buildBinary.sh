#!/bin/sh

D=${TMPDIR:=/tmp}/f2f

rm -r $D > /dev/null
mkdir $D

cp flea2flea.mf thirdparty/*.jar $D/
cp README $D/

Z=com/purplefrog/flea2flea/docs
mkdir -p $D/$Z
cp $Z/*.html $Z/*.txt   $D/$Z/

find . -name '*.class' -exec rm \{\} \;

javac -classpath .:thirdparty/commons-logging.jar:thirdparty/javax.servlet.jar:thirdparty/org.mortbay.jetty.jar -d $D com/purplefrog/flea2flea/Main.java

(cd $D 
    jar cmf flea2flea.mf flea2flea.jar com
    zip ../flea2flea.zip *.jar README
)

