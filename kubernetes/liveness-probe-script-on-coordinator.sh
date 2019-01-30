#!/bin/sh

set -e

# Load previous value of coordinator's uptime
LAST_UPTIME_TOTAL_SECONDS_FILE_PATH=/cache/last-uptime-total-seconds
if [ -f $LAST_UPTIME_TOTAL_SECONDS_FILE_PATH ]; then
  LAST_UPTIME_TOTAL_SECONDS=$(cat $LAST_UPTIME_TOTAL_SECONDS_FILE_PATH)
else
  LAST_UPTIME_TOTAL_SECONDS=0
fi

# Measure current coordinator's uptime
UPTIME=$(ps -o etime | sed -n 2P)
UPTIME_MINUTES_PLACE=$(echo $UPTIME | cut -d ':' -f 1)
UPTIME_SECONDS_PLACE=$(echo $UPTIME | cut -d ':' -f 2)
UPTIME_TOTAL_SECONDS=$(expr 60 \* $UPTIME_MINUTES_PLACE + $UPTIME_SECONDS_PLACE)

# Save the value
echo $UPTIME_TOTAL_SECONDS > $LAST_UPTIME_TOTAL_SECONDS_FILE_PATH

# If the coorcinator was turned out to be recreated, fail this health check
test $UPTIME_TOTAL_SECONDS -ge $LAST_UPTIME_TOTAL_SECONDS
