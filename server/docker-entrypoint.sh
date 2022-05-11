#!/bin/sh

java -jar wizard-server.jar \
    ${LOG_LEVEL:+--log-level=}$LOG_LEVEL \
    ${CERT:+--cert=}$CERT \
    ${KEY:+--key=}$KEY \
    ${PORT:+--port=}$PORT
