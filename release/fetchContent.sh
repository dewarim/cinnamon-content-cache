#!/usr/bin/env bash

export TICKET="fce11ba7-4677-4dfa-a0d2-bcaac0818677@demo"

set -e

echo ""
curl -d "<contentRequest><ticket>${TICKET}</ticket><id>1003</id></contentRequest>" http://localhost:9090/content/getContent
echo ""
    
