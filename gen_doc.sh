#!/bin/bash

mkdir -p build/outputs/doc
javadoc -d build/outputs/doc -sourcepath src/main/java -subpackages com -overview src/main/java/overview.html
