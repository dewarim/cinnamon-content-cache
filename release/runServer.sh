#!/bin/bash

# Needs Java 9

# Start the content cache server and go to the background.
# See: https://stackoverflow.com/a/11856575/2018067

nohup java -jar cinnamon-content-cache-1.2.jar --config config.xml > /dev/null 2>&1 &

# alternative: for debugging, let nophup log to nohup.out:
# nohup java -jar cinnamon-content-cache-1.0.jar --config config.xml &

# to kill the server:
# ps
# kill $process id for java.