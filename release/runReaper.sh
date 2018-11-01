#!/bin/bash

# Start a process to walk the file tree and delete stale cache entries.
# Needs Java 9

java -jar cinnamon-content-cache-1.2.jar com.dewarim.cinnamon.application.Reaper  --config config.xml

