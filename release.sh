#!/bin/sh

die ()
{
    echo "$@"
    exit 1
}

TAG=$1

if [ -z "$TAG" ] ; then
    echo "usage: $0 tag"
    exit 1
fi

if [ -z "$CVSROOT" ] ; then
    echo "You forgot to define CVSROOT.  perhaps you should CVSROOT=:ext:`id -un`@cvs.sourceforge.net:cvsroot/flea2flea"
    exit 1
fi

cd ${TMPDR:=/tmp} || die "failed to cd"

rm -r flea2flea
cvs export -r$TAG flea2flea || die "cvs export failed"

cd flea2flea
perl -i -ne "print; print \"Built from tag $TAG\\n\\n\" if /<body>/;" com/purplefrog/flea2flea/docs/about.html
./buildBinary.sh
