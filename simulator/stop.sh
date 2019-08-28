#!/bin/bash
kill -9 `ps -ef| grep com.ibm.lwm2m.client.LwM2MAdvClient | awk '{print $2}'`
