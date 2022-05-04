#!/bin/bash

set -e

echo "Now updating documentation..."
rm -rf docs
./gradlew dokkaHtmlMultiModule
echo "ktor.noelware.org" >> docs/CNAME
echo "done!"
