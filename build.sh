#!/bin/bash

OPTSTRING=":ab"

while getopts ${OPTSTRING} opt; do
  case ${opt} in
    a)
      echo "Option -a was triggered."
      ;;
    b)
      echo "Option -b was triggered."
      ;;
    ?)
      echo "Invalid option: -${OPTARG}."
      exit 1
      ;;
  esac
done