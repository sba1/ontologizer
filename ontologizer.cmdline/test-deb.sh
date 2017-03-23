#!/bin/bash

NO_CACHE=""

if [ $# -gt 0 ]; then
	if [ "$1" = "--no-cache" ]; then
		NO_CACHE=--no-cache
	fi
fi

docker build $NO_CACHE . -t onto-cmd-test

