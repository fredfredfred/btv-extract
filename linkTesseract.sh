#!/usr/bin/env sh -e

version="5.11.0"
repoFolder="/Users/ansgar/.gradle/caches/modules-2/files-2.1/net.sourceforge.tess4j/tess4j/5.11.0/1118392720ab47297fc66d9708389ed670ee8eda"
jarFile="tess4j-$version.jar"
tessLibFile="libtesseract.5.dylib"
tessLibFolder="/opt/homebrew/lib"

cd $repoFolder
mkdir darwin
jar uf $jarFile darwin
cp "$tessLibFolder/$tessLibFile" "darwin/libtesseract.dylib"
jar uf $jarFile darwin/libtesseract.dylib
jar tf $jarFile
