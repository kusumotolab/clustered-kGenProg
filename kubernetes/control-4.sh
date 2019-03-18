#!/usr/bin/env bash

NAMESPACE="4-workers"
WATCH_INTERVAL_SEC=5

SCRIPT_DIR="$(cd $(dirname "${BASH_SOURCE:-$0}"); pwd)"

# Load shell-logger
source ${SCRIPT_DIR}/lib/shell-logger.sh

# shell-logger settings
# LOGGER_INFO_COLOR=36
LOGGER_SHOW_FILE=0
LOGGER_ERROR_TRACE=0


if [[ ! $(kubectl --namespace $NAMESPACE get pods 2> /dev/null | grep c-kgp-coordinator) = '' ]]; then
  error "Coordinator seems to be already running in this namespace.
You should at first delete coordinator to use this script.
Try: kubectl --namespace $NAMESPACE delete -f ${SCRIPT_DIR}/deploy.yml"
  exit 1
fi


while :
do

  notice '(Re-)Creating clustered-kGenProg services on Kubernetes ...'
  kubectl --namespace $NAMESPACE apply -f ${SCRIPT_DIR}/deploy-4.yml
  if [[ $? -eq 0 ]]; then
    notice 'Services successfully created.'
  else
    error 'Failed to creating services. Abort.'
    exit 1
  fi

  while :
  do

    sleep $WATCH_INTERVAL_SEC

    COORDINATOR_RESTARTS=$(kubectl --namespace $NAMESPACE get pods | grep c-kgp-coordinator | awk '{ print $4 }')
    if [[ $? -ne 0 ]]; then
      error "Failed to detect coordinator's status. Abort."
      exit 1
    fi

    if [[ $COORDINATOR_RESTARTS -ge '1' ]]; then
      warn 'Coordinator seems to have crashed!'
      notice 'Deleting existing services ...'
      kubectl --namespace $NAMESPACE delete -f ${SCRIPT_DIR}/deploy-4.yml
      if [[ $? -eq 0 ]]; then
        notice 'Services successfully deleted.'
        break
      else
        error 'Failed to deleting services. Abort.'
        exit 1
      fi
    fi

  done

done
